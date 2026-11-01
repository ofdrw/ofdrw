package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.ofdrw.converter.pdf.model.PatternCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * 底纹写入器（P3）：中间模型 {@link PatternCommand} → PDF Tiling Pattern
 * <p>
 * 语义（表 28、决策 17）：
 * <ul>
 *     <li>{@code /BBox} 取单元尺寸（{@link PatternCommand#getCell()}），单元内容只绘制在单元格内，
 *     超出部分被裁掉；</li>
 *     <li>{@code /XStep}、{@code /YStep} 取相邻单元的间距：与单元尺寸相同即无间隙平铺，
 *     大于单元尺寸时相邻单元之间保留透明间隙（OFD 允许间距大于单元）；</li>
 *     <li>{@code /PaintType 1}（有色图案）、{@code /TilingType 1}（固定间距）；</li>
 *     <li>PDF 原生支持镜像平铺，但决策 17 已确认首版统一按 {@code ReflectMethod=Normal} 近似，
 *     因此这里只输出 Normal 平铺；渲染阶段对 Column/Row/RowAndColumn 与 {@code RelativeTo=Page}
 *     已输出 warning 并标记 {@link PatternCommand#isApproximate()}。</li>
 * </ul>
 * 填充方式：{@code q → 填充不透明度 → 以图案为着色色的非描边色 → 填充路径 → Q}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfPatternWriter {

    private static final Logger logger = LoggerFactory.getLogger(PdfPatternWriter.class);

    /**
     * 路径工厂
     */
    private final PdfPathFactory pathFactory;

    /**
     * 页面内图案名序号
     */
    private int patternIndex = 1;

    /**
     * 构造底纹写入器
     *
     * @param pathFactory 路径工厂，不能为空
     */
    public PdfPatternWriter(PdfPathFactory pathFactory) {
        if (pathFactory == null) {
            throw new IllegalArgumentException("路径工厂不能为空");
        }
        this.pathFactory = pathFactory;
    }

    /**
     * 写入底纹填充（无文字钩子）
     *
     * @param document  PDF 文档
     * @param stream    页面内容流
     * @param resources 页面资源字典
     * @param command   底纹指令
     * @param area      填充区域（页面坐标），为空时跳过
     * @param alpha     不透明度，0~1
     * @throws IOException 写入失败
     */
    public void write(PDDocument document, PDPageContentStream stream, PDResources resources,
                      PatternCommand command, java.util.List<org.ofdrw.converter.pdf.model.PathSegment> area,
                      float alpha) throws IOException {
        write(document, stream, resources, command, area, alpha, null, null, null);
    }

    /**
     * 写入底纹填充（带文字写入能力）
     * <p>
     * 图案内的文字必须写进<b>图案自身</b>的内容流，页面内容流的钩子做不到这一点，
     * 因此这里由写入器自己驱动文字写入器。
     *
     * @param document   PDF 文档
     * @param stream     页面内容流
     * @param resources  页面资源字典
     * @param command    底纹指令
     * @param area       填充区域（页面坐标），为空时跳过
     * @param alpha      不透明度，0~1
     * @param hook       指令钩子，可为空
     * @param textWriter 文字写入器，可为空（为空时图案内的文字被跳过）
     * @param registry   字体登记表，可为空
     * @throws IOException 写入失败
     */
    public void write(PDDocument document, PDPageContentStream stream, PDResources resources,
                      PatternCommand command, java.util.List<org.ofdrw.converter.pdf.model.PathSegment> area,
                      float alpha, PdfCommandReplayer.CommandHook hook, PdfTextWriter textWriter,
                      TrueTypePdfFont.Registry registry) throws IOException {
        if (command == null || area == null || area.isEmpty()) {
            logger.warn("[{}] 底纹缺少填充区域，已跳过：{}", "OFD2PDF", command == null ? "null" : command.getName());
            return;
        }
        if (command.isApproximate()) {
            logger.warn("[{}] 底纹按 Normal 近似平铺（决策 17）：{}", "OFD2PDF", command.getName());
        }
        /*
         * 图案的内容流就是图案字典自身（PDF 的 Pattern 本身是一个带 /PatternType 的流对象），
         * 因此这里直接创建 COSStream 并把图案属性写入流字典。
         */
        org.apache.pdfbox.cos.COSStream patternStream = document.getDocument().createCOSStream();
        patternStream.setInt(COSName.getPDFName("PatternType"), 1);
        patternStream.setInt(COSName.getPDFName("PaintType"), PDTilingPattern.PAINT_COLORED);
        patternStream.setInt(COSName.getPDFName("TilingType"), PDTilingPattern.TILING_CONSTANT_SPACING);
        // 图案内容流直接引用页面资源字典：字体等资源由页面登记，图案内按同一资源名引用，
        // 另建空字典会让图案内的字体无法解析
        patternStream.setItem(COSName.getPDFName("Resources"), resources.getCOSObject());
        PDTilingPattern pattern = new PDTilingPattern(patternStream);
        pattern.setBBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(
                (float) command.getCell().getWidth(), (float) command.getCell().getHeight()));
        // 步进取底纹声明的 XStep/YStep（大于单元尺寸时单元之间留有间隙）
        pattern.setXStep((float) command.getXStepPt());
        pattern.setYStep((float) command.getYStepPt());

        /*
         * 单元内容：先把指令回放到临时页内容流，再把操作符整体拷进图案的内容流。
         * PDFBox 3 不再提供“doc + 任意内容流”的 PDPageContentStream 构造器（2.x 可用），
         * 因此采用“回放 → 拷贝”的方式复用同一套指令回放实现，避免两套代码分叉。
         */
        org.apache.pdfbox.pdmodel.PDPage scratchPage = new org.apache.pdfbox.pdmodel.PDPage();
        document.addPage(scratchPage);
        // 临时页必须复用正式页的资源字典：PDPageContentStream#setFont 写入的是“该内容流所属页资源字典
        // 里的资源名”，临时页若自带空字典，字体会被命名成 F1/F2…，与页面真实资源名（如 F9）不一致，
        // 图案单元就会引用到错误的字体（字形码属于原字体，替代字体缺这些 GID → 文字整块消失）
        scratchPage.setResources(resources);


        try {
            // 图案内容可引用字体等资源，必须在图案自身的资源字典登记；PDResources 只是 COS 字典的视图，
            // 因此这里必须持有同一个包装实例，否则钩子登记的资源会丢失
            // 图案内容流与页面共用同一份资源（字体等由页面登记，图案内按同一资源名引用）
            final PDResources cellResources = resources;
            final org.apache.pdfbox.pdmodel.PDPageContentStream[] streamHolder = new org.apache.pdfbox.pdmodel.PDPageContentStream[1];
            PdfCommandReplayer.CommandHook cellHook = hook;
            if (textWriter != null && registry != null) {
                final PdfTextWriter writer = textWriter;
                final TrueTypePdfFont.Registry reg = registry;
                cellHook = cellCommand -> {
                    if (hook != null && hook.handle(cellCommand)) {
                        return true;
                    }
                    if (!(cellCommand instanceof org.ofdrw.converter.pdf.model.TextCommand)) {
                        return false;
                    }
                    org.ofdrw.converter.pdf.model.TextCommand text =
                            (org.ofdrw.converter.pdf.model.TextCommand) cellCommand;
                    TrueTypePdfFont font = reg.lookupByResourceName(text.getFontRef());
                    if (font == null) {
                        return true;
                    }
                    writer.write(document, scratchPage, streamHolder[0], cellResources, font, text);
                    return true;
                };
            }
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cellStream =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(document, scratchPage)) {
                streamHolder[0] = cellStream;
                new PdfCommandReplayer(pathFactory).replay(document, cellStream,
                        cellResources, command.getCellCommands(), cellHook);
            }
            // 保留 /gsN gs：图案单元的半透明（底纹文字 Alpha）依赖它；图案与页面共用同一份资源字典，
            // 因此这些 ExtGState 名称在图案作用域内可解析
            byte[] operators = readContent(scratchPage);
            try (java.io.OutputStream out = patternStream.createOutputStream()) {
                out.write(operators);
            }
        } finally {
            document.removePage(scratchPage);
        }

        // 图案以页面资源的形式命名，并在着色时以 /Pattern 颜色空间引用
        String patternName = "P" + (patternIndex++);
        COSDictionary patterns = patternDict(resources);
        /*
         * 底纹的 CTM（旋转/缩放）：PDF 的平铺矩阵是 Pattern 字典的 /Matrix，
         * 它只影响图案单元的排布方向，不会旋转被填充的路径本身。
         * 注意 OFD 的含义是“先按 XStep/YStep 排列，再对整体做 CTM”，
         * 因此 /Matrix 取 CTM 线性部分，步进仍按单元尺寸给出。
         */
        double[] m = command.getTilingMatrix();
        if (m != null && m.length == 6) {
            org.apache.pdfbox.cos.COSArray matrixArray = new org.apache.pdfbox.cos.COSArray();
            for (int i = 0; i < 6; i++) {
                matrixArray.add(new org.apache.pdfbox.cos.COSFloat((float) m[i]));
            }
            patternStream.setItem(COSName.getPDFName("Matrix"), matrixArray);
        }
        patterns.setItem(COSName.getPDFName(patternName), pattern.getCOSObject());

        stream.saveGraphicsState();
        PDExtendedGraphicsState state = new PDExtendedGraphicsState();
        state.setNonStrokingAlphaConstant(alpha);
        stream.setGraphicsStateParameters(state);
        // 图案以 /Pattern 颜色空间的命名资源着色：直接写 "scn" 避免 PDFBox 对图案颜色空间的组件数校验
        stream.appendRawCommands("/Pattern cs\n");
        stream.appendRawCommands("/" + patternName + " scn\n");
        pathFactory.append(stream, area);
        stream.fill();
        stream.restoreGraphicsState();
    }

    /**
     * 去掉图案单元内容里针对“页面资源”的图形状态引用
     * <p>
     * 单元回放时路径图元会先写 {@code /gsN gs}（该名称由页面资源字典登记）。直接把这段操作符拷进
     * 图案内容流后，图案会按页面级图形状态绘制，透明度等设置作用到整个平铺区域，
     * 表现为图案把整页涂黑。图案只需要几何与文字，图形状态由单元内显式写入的
     * {@code ca}/{@code CA}/{@code rg} 决定，因此这里剥离 {@code /gsN gs} 行。
     *
     * @param operators 回放得到的操作符字节
     * @return 去掉图形状态引用的操作符
     */
    private static byte[] stripPageStateOperators(byte[] operators) {
        String text = new String(operators, java.nio.charset.StandardCharsets.ISO_8859_1);
        StringBuilder out = new StringBuilder(text.length());
        for (String line : text.split("\n")) {
            if (line.trim().matches("/gs\\d+\\s+gs")) {
                continue;
            }
            out.append(line).append('\n');
        }
        return out.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }

    /**
     * 读取页面内容流的原始操作符
     *
     * @param page 页面
     * @return 操作符字节
     * @throws IOException 读取失败
     */
    private static byte[] readContent(org.apache.pdfbox.pdmodel.PDPage page) throws IOException {
        org.apache.pdfbox.cos.COSBase contents = page.getCOSObject().getDictionaryObject(COSName.CONTENTS);
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        try {
            if (contents instanceof org.apache.pdfbox.cos.COSStream) {
                copy((org.apache.pdfbox.cos.COSStream) contents, out);
            } else if (contents instanceof COSArray) {
                // 内容为多个流的数组：逐个解码并按顺序拼接
                COSArray array = (COSArray) contents;
                for (int i = 0; i < array.size(); i++) {
                    org.apache.pdfbox.cos.COSBase item = array.getObject(i);
                    if (item instanceof org.apache.pdfbox.cos.COSStream) {
                        copy((org.apache.pdfbox.cos.COSStream) item, out);
                        out.write('\n');
                    }
                }
            }
        } finally {
            out.flush();
        }
        return out.toByteArray();
    }

    /**
     * 拷贝一个内容流的数据
     *
     * @param stream 内容流
     * @param out    输出
     * @throws IOException 读取或写入失败
     */
    private static void copy(org.apache.pdfbox.cos.COSStream stream, java.io.OutputStream out) throws IOException {
        try (java.io.InputStream in = stream.createInputStream()) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
        }
    }

    /**
     * 页面资源中的图案字典（不存在时创建）
     *
     * @param resources 页面资源
     * @return 图案字典
     */
    private static COSDictionary patternDict(PDResources resources) {
        COSDictionary patterns = (COSDictionary) resources.getCOSObject().getDictionaryObject(COSName.PATTERN);
        if (patterns == null) {
            patterns = new COSDictionary();
            resources.getCOSObject().setItem(COSName.PATTERN, patterns);
        }
        return patterns;
    }

}
