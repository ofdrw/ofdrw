package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.cos.COSArray;
import org.apache.pdfbox.cos.COSDictionary;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
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
     * 写入底纹填充
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
        patternStream.setItem(COSName.getPDFName("Resources"), new COSDictionary());
        PDTilingPattern pattern = new PDTilingPattern(patternStream);
        pattern.setBBox(new org.apache.pdfbox.pdmodel.common.PDRectangle(
                (float) command.getCell().getWidth(), (float) command.getCell().getHeight()));
        // 步进缺省与单元尺寸一致（无间隙平铺）
        pattern.setXStep((float) command.getCell().getWidth());
        pattern.setYStep((float) command.getCell().getHeight());

        /*
         * 单元内容：先把指令回放到临时页内容流，再把操作符整体拷进图案的内容流。
         * PDFBox 3 不再提供“doc + 任意内容流”的 PDPageContentStream 构造器（2.x 可用），
         * 因此采用“回放 → 拷贝”的方式复用同一套指令回放实现，避免两套代码分叉。
         */
        org.apache.pdfbox.pdmodel.PDPage scratchPage = new org.apache.pdfbox.pdmodel.PDPage();
        document.addPage(scratchPage);
        try {
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cellStream =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(document, scratchPage)) {
                new PdfCommandReplayer(pathFactory).replay(document, cellStream, resources, command.getCellCommands());
            }
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
