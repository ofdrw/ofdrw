package org.ofdrw.converter.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.pdf.font.TrueTypePdfFont;
import org.ofdrw.converter.pdf.model.Box;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.render.PageRendererImpl;
import org.ofdrw.converter.pdf.write.PdfCommandReplayer;
import org.ofdrw.converter.pdf.write.PdfFontFactory;
import org.ofdrw.converter.pdf.write.PdfPathFactory;
import org.ofdrw.converter.pdf.write.PdfStructureWriter;
import org.ofdrw.converter.pdf.write.PdfTextWriter;
import org.ofdrw.converter.pdf.write.StructureSource;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.reader.OFDReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * OFD 导出为 PDF 文档
 * <p>
 * <b>不建议导出为 PDF。</b>OFD 是国产版式文档，除交换存档等特殊需求外无需转换为 PDF；
 * 如果只是需要查看文档内容，请直接使用 ofdrw-viewer 预览，无需转换。
 * <p>
 * 使用示例：
 * <pre>
 *     try (OFDExporter exporter = new PDFExporter(ofdPath, pdfPath)) {
 *         exporter.export();
 *     }
 * </pre>
 * <p>
 * 导出完成后必须关闭本对象，附件等文档级信息在关闭时写入。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public class PDFExporter implements OFDExporter {

    private static final Logger logger = LoggerFactory.getLogger(PDFExporter.class);

    /**
     * 告警标签，与渲染管线日志保持一致
     */
    private static final String DIAGNOSTIC_TAG = "OFD2PDF";

    /**
     * OFD 加密信息文件名（GB/T 33190-2016 第 18 章：位于包根目录，与 OFD.xml 同级）
     */
    private static final String ENCRYPTIONS_FILE = "Encryptions.xml";

    /**
     * OFD 解析器
     */
    private final OFDReader ofdReader;

    /**
     * 字体对象工厂：保存前需用它把全部字形映射刷入 ToUnicode CMap
     */
    private PdfFontFactory fontFactory;

    /**
     * 全文档字体登记表：跨页累积，保存前用于刷新所有字体的 ToUnicode
     */
    private TrueTypePdfFont.Registry fontRegistry;

    /**
     * 本次导出中发生字体替换的字体数量（内嵌缺失或无法解析时按替换策略处理）
     * <p>
     * 内部统计量，用于回归框架识别“纯字体替换样本”（决策 7：替换只告警、不阻断）。
     * 不是公共 API 的结果对象（决策 19 约束的是对外暴露的 Result/Options 类型）。
     */
    private int fontSubstitutionCount;

    /**
     * 是否由本对象负责关闭 OFD 解析器
     */
    private final boolean ownsReader;

    /**
     * PDF 输出文件路径，为空表示输出到流
     */
    private final Path outputPath;

    /**
     * PDF 输出流，为空表示输出到文件
     */
    private final OutputStream outputStream;

    /**
     * 是否已经关闭
     */
    private boolean closed = false;

    /**
     * 是否已经导出过页面（重复导出会覆盖同一 PDF 文档，按首次导出结果为准）
     */
    private boolean exported = false;

    /**
     * 目标 PDF 文档，首次导出时创建
     */
    private PDDocument document;

    /**
     * 与 {@link #document} 页顺序一一对应的 OFD 页对象 ID
     */
    private final List<String> exportedPageIds = new ArrayList<>();

    /**
     * 与 {@link #document} 页顺序一一对应的 PDF 页面
     */
    private final List<PDPage> exportedPages = new ArrayList<>();

    /**
     * 页对象 ID → 该页注释（首次使用时加载）
     */
    private java.util.Map<String, List<Annot>> annotationsByPage;

    /**
     * 通过文件路径创建 PDF 导出器
     *
     * @param ofdFilePath 待转换的 OFD 文件路径
     * @param pdfFilePath 生成的 PDF 文件路径，父目录不存在时自动创建
     * @throws IOException 文件创建失败
     */
    public PDFExporter(Path ofdFilePath, Path pdfFilePath) throws IOException {
        this(new OFDReader(ofdFilePath), true, prepareFile(pdfFilePath), null);
    }

    /**
     * 通过流创建 PDF 导出器
     * <p>
     * 注意：输入流与输出流由调用者负责关闭。
     *
     * @param ofdInStream  待转换的 OFD 文件流
     * @param pdfOutStream 生成的 PDF 文件流
     * @throws IOException 流操作失败
     */
    public PDFExporter(InputStream ofdInStream, OutputStream pdfOutStream) throws IOException {
        this(new OFDReader(ofdInStream), true, null, requireStream(pdfOutStream));
    }

    /**
     * 通过 OFD 解析器创建 PDF 导出器
     *
     * @param ofdReader   OFD 解析器，由调用者负责关闭
     * @param pdfFilePath 生成的 PDF 文件路径
     * @throws IOException 文件创建失败
     */
    public PDFExporter(OFDReader ofdReader, Path pdfFilePath) throws IOException {
        this(ofdReader, false, prepareFile(pdfFilePath), null);
    }

    /**
     * 通过 OFD 解析器创建 PDF 导出器
     * <p>
     * 注意：输出流由调用者负责关闭。
     *
     * @param ofdReader    OFD 解析器，由调用者负责关闭
     * @param pdfOutStream 生成的 PDF 文件流
     */
    public PDFExporter(OFDReader ofdReader, OutputStream pdfOutStream) {
        this(ofdReader, false, null, requireStream(pdfOutStream));
    }

    /**
     * @param ofdReader    OFD 解析器
     * @param ownsReader   是否由本对象负责关闭 OFD 解析器
     * @param outputPath   输出文件路径，可为空
     * @param outputStream 输出流，可为空
     */
    private PDFExporter(OFDReader ofdReader, boolean ownsReader, Path outputPath, OutputStream outputStream) {
        if (ofdReader == null) {
            throw new IllegalArgumentException("待转换 OFD 为空");
        }
        this.ofdReader = ofdReader;
        this.ownsReader = ownsReader;
        this.outputPath = outputPath;
        this.outputStream = outputStream;
    }

    /**
     * 导出指定 OFD 页
     * <p>
     * 页码序列为空时导出全部页面；页码从 0 起。
     *
     * @param indexes 页码序列，如果为空表示全部页码（注意：页码从0起）
     * @throws GeneralConvertException 导出异常
     */
    @Override
    public void export(int... indexes) throws GeneralConvertException {
        if (closed) {
            throw new GeneralConvertException("PDF 导出器已关闭");
        }
        List<Integer> target = new LinkedList<>();
        int total = ofdReader.getNumberOfPages();
        if (indexes == null || indexes.length == 0) {
            for (int i = 0; i < total; i++) {
                target.add(i);
            }
        } else {
            for (int index : indexes) {
                if (index >= 0 && index < total) {
                    target.add(index);
                }
            }
        }
        exportPages(target);
    }

    /**
     * 导出指定页面序列
     * <p>
     * 由新的 PDF 渲染管线实现，页码校验在本类完成（页码从 0 起，越界页码已在 {@link #export(int...)} 中剔除）。
     * 页面按给定顺序依次渲染并追加到同一 PDF 文档；重复调用不会重复追加页面。
     * <p>
     * 页面框按决策 26 取 {@code MediaBox = CropBox = PhysicalBox}；
     * 文字指令仅显式指定文字颜色与透明度，与路径、图像的先后关系保持渲染顺序（Z 序）。
     *
     * @param indexes 页码序列，页码从 0 起，已去除越界页码
     * @throws GeneralConvertException 导出异常
     */
    protected void exportPages(List<Integer> indexes) throws GeneralConvertException {
        if (exported) {
            logger.warn("[{}] 已执行过导出，重复导出请求被忽略", DIAGNOSTIC_TAG);
            return;
        }
        exported = true;
        // 决策 24：检测到密码保护直接抛异常，绝不输出空白页
        requireNotEncrypted();
        try {
            document = new PDDocument();
            fontFactory = new PdfFontFactory();
            PdfTextWriter textWriter = new PdfTextWriter(fontFactory);
            PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
            // 字体登记表由渲染管线创建：渲染时登记并分配资源名，写入时按资源名取回，全流程唯一实例
            PageRendererImpl.RenderPipeline pipeline = PageRendererImpl.create(ofdReader);
            fontRegistry = pipeline.registry();
            fontSubstitutionCount = 0;
            for (Integer index : indexes) {
                if (index == null) {
                    continue;
                }
                renderPage(pipeline, replayer, textWriter, index.intValue());
            }
            fontSubstitutionCount = countSubstitutedFonts(fontRegistry);
        } catch (IOException e) {
            closeQuietly();
            throw new GeneralConvertException("PDF 导出失败：" + e.getMessage(), e);
        } catch (RuntimeException e) {
            closeQuietly();
            throw new GeneralConvertException("PDF 导出失败：" + e.getMessage(), e);
        }
    }

    /**
     * 检测 OFD 包是否加密（决策 24）
     * <p>
     * GB/T 33190-2016 第 18 章：加密文档在包根目录放置 {@code Encryptions.xml}（与 {@code OFD.xml} 同级）。
     * 检测到即抛出 {@link GeneralConvertException}，不输出空白页。
     *
     * @throws GeneralConvertException 文档已加密
     */
    private void requireNotEncrypted() throws GeneralConvertException {
        Path workDir = ofdReader.getWorkDir();
        if (workDir == null) {
            // 无工作目录（流式读取尚未解包）时无法判定，交由后续解析报错
            return;
        }
        Path encryptions = workDir.resolve(ENCRYPTIONS_FILE);
        boolean exists;
        try {
            exists = Files.exists(encryptions);
        } catch (RuntimeException e) {
            // 连“是否存在”都判定不了时同样不能静默当作未加密
            throw new GeneralConvertException("无法判定 OFD 文档是否加密："
                    + workDir + "（" + e.getMessage() + "）");
        }
        if (!exists) {
            return;
        }
        // 决策 24：检测到密码保护直接抛异常，且不因“读不到”而静默放行
        if (!Files.isReadable(encryptions)) {
            throw new GeneralConvertException("OFD 文档存在加密信息文件（" + ENCRYPTIONS_FILE
                    + "）但无法读取，拒绝转换为 PDF：" + encryptions);
        }
        throw new GeneralConvertException("OFD 文档受密码保护（存在 " + ENCRYPTIONS_FILE + "），无法转换");
    }

    /**
     * 渲染并写入一页
     *
     * @param pipeline   渲染管线（含页面渲染器、字体选择器与共享字体登记表）
     * @param replayer   指令回放器
     * @param textWriter 文字写入器
     * @param index      OFD 页码，从 0 起
     * @throws IOException 写入失败
     */
    private void renderPage(PageRendererImpl.RenderPipeline pipeline, PdfCommandReplayer replayer,
                            PdfTextWriter textWriter, int index) throws IOException {
        // 清空上一页登记的字形资源，本页资源名重新从 F1 开始；必须先于 renderPage，否则本页登记的字体被清掉
        pipeline.registry().newPage();
        String pageId = ofdReader.getPageObjectId(index + 1).toString();
        Page page = ofdReader.getPage(index + 1);
        PdfPageContent content = pipeline.pageRenderer().renderPage(page, pipeline.fontSelector());
        // 设计 3.8：注释的静态外观（水印/图章/签名外观）必须由页面内容重绘，PDF 注释对象只承载交互
        List<Annot> annots = annotationsOf(pageId);
        if (!annots.isEmpty()) {
            PdfPageContent annotContent =
                    pipeline.pageRenderer().renderAnnotations(page, annots, pipeline.fontSelector());
            content.addAll(annotContent.getCommands());
        }

        // 决策 26：MediaBox = CropBox = PhysicalBox（值相同、独立对象）
        // 页面未声明 Area 时回退文档级 PageArea（表 27 可选属性），因此传入 reader
        Box box = PageRendererImpl.pageBox(page, ofdReader).toPdfBox();
        final float x0 = (float) box.getX();
        final float y0 = (float) box.getY();
        final float width = (float) box.getWidth();
        final float height = (float) box.getHeight();
        PDPage pdPage = new PDPage(new PDRectangle(x0, y0, width, height));
        pdPage.setCropBox(new PDRectangle(x0, y0, width, height));
        document.addPage(pdPage);

        final TrueTypePdfFont.Registry registry = pipeline.registry();
        try (PDPageContentStream stream = new PDPageContentStream(document, pdPage)) {
            PDResources resources = pdPage.getResources();
            if (resources == null) {
                resources = new PDResources();
                pdPage.setResources(resources);
            }
            final PDResources pageResources = resources;
            final PDPage targetPage = pdPage;
            // 单次遍历 + 文字钩子：路径/图像由回放处理，文字交给写入器，q/Q 作用域与 Z 序都不被打断
            replayer.replay(document, stream, pageResources, content.getCommands(), command -> {
                if (!(command instanceof TextCommand)) {
                    return false;
                }
                TextCommand text = (TextCommand) command;
                TrueTypePdfFont font = registry.lookupByResourceName(text.getFontRef());
                if (font == null) {
                    logger.warn("[{}] 文字字体资源未登记，已跳过：{}", DIAGNOSTIC_TAG, text.getFontRef());
                    return true;
                }
                textWriter.write(document, targetPage, stream, pageResources, font, text);
                return true;
            });
        }
        exportedPages.add(pdPage);
        exportedPageIds.add(pageId);
    }

    /**
     * 读取指定页的注释（含外观）
     *
     * @param pageId OFD 页对象 ID
     * @return 注释列表；无注释时返回空列表
     */
    private List<Annot> annotationsOf(String pageId) {
        if (annotationsByPage == null) {
            annotationsByPage = new java.util.HashMap<>();
            try {
                for (org.ofdrw.reader.model.AnnotionEntity entity : ofdReader.getAnnotationEntities()) {
                    if (entity == null || entity.getPageId() == null || entity.getAnnots() == null) {
                        continue;
                    }
                    List<Annot> list = annotationsByPage.get(entity.getPageId());
                    if (list == null) {
                        list = new ArrayList<>();
                        annotationsByPage.put(entity.getPageId(), list);
                    }
                    list.addAll(entity.getAnnots());
                }
            } catch (RuntimeException e) {
                logger.warn("[{}] 注释读取失败，本页不绘制注释外观：{}", DIAGNOSTIC_TAG, e.getMessage());
            }
        }
        List<Annot> annots = annotationsByPage.get(pageId);
        return annots == null ? java.util.Collections.<Annot>emptyList() : annots;
    }

    /**
     * 写出文档级信息并保存 PDF
     * <p>
     * 结构信息按实际导出的页面子集写入：大纲、注释的目标页只映射到已导出的页。
     *
     * @throws IOException 保存失败
     */
    private void writeDocument() throws IOException {
        if (document == null) {
            return;
        }
        StructureSource source = StructureSource.load(ofdReader, exportedPageIds);
        new PdfStructureWriter(document, source, exportedPages, exportedPageIds).writeAll();
        if (fontFactory != null && fontRegistry != null) {
            /*
             * 字体对象在首次使用时创建，后续页面新增的字形映射需在保存前统一刷入 ToUnicode；
             * 而字体登记表每页会重置资源名，因此这里取“全文档累积”的字体集合，
             * 否则只有最后一页的字体能更新到完整映射，前面各页的字形在提取时会退化为错位码位。
             */
            fontFactory.refreshToUnicode(document, fontRegistry.getAllFonts());
        }
        if (outputStream != null) {
            document.save(outputStream);
            outputStream.flush();
        } else {
            document.save(outputPath.toFile());
        }
    }

    /**
     * 释放 PDF 文档与 OFD 解析器，忽略释放异常
     */
    private void closeQuietly() {
        if (document != null) {
            try {
                document.close();
            } catch (IOException ignored) {
                // 释放失败无需上抛
            }
            document = null;
        }
        if (ownsReader) {
            try {
                ofdReader.close();
            } catch (IOException ignored) {
                // 释放失败无需上抛
            }
        }
    }

    /**
     * 本次导出中发生字体替换的字体数量（供回归框架识别“纯字体替换样本”，决策 7）
     * <p>
     * 返回值大于 0 表示该文档至少有一种字体并非原字体（内嵌缺失或无法解析时按替换策略处理）。
     *
     * @return 发生替换的字体数量；尚未导出时返回 0
     */
    public int getFontSubstitutionCount() {
        return fontSubstitutionCount;
    }

    /**
     * 统计渲染过程中发生替换的字体数量
     *
     * @param registry 字体登记表，可为空
     * @return 替换字体数量
     */
    private static int countSubstitutedFonts(TrueTypePdfFont.Registry registry) {
        if (registry == null) {
            return 0;
        }
        int count = 0;
        for (TrueTypePdfFont font : registry.getAllFonts()) {
            if (font != null && font.isSubstituted()) {
                count++;
            }
        }
        return count;
    }

    /**
     * 关闭导出器并写出 PDF
     * <p>
     * 文档级信息（附件、书签、元数据）在此步骤写入。
     *
     * @throws IOException 文件关闭异常
     */
    @Override
    public void close() throws IOException {
        if (closed) {
            return;
        }
        closed = true;
        try {
            writeDocument();
        } finally {
            closeQuietly();
        }
    }

    /**
     * 校验输出流
     *
     * @param outputStream 输出流
     * @return 输出流
     */
    private static OutputStream requireStream(OutputStream outputStream) {
        if (outputStream == null) {
            throw new IllegalArgumentException("导出PDF流为空");
        }
        return outputStream;
    }

    /**
     * 准备输出文件，父目录不存在时自动创建
     *
     * @param pdfFilePath 输出文件路径
     * @return 绝对路径
     * @throws IOException 文件创建失败
     */
    private static Path prepareFile(Path pdfFilePath) throws IOException {
        if (pdfFilePath == null) {
            throw new IllegalArgumentException("导出PDF路径为空");
        }
        Path target = pdfFilePath.toAbsolutePath();
        if (!Files.exists(target)) {
            Path parent = target.getParent();
            if (parent == null) {
                throw new IllegalArgumentException("导出PDF路径无效: " + pdfFilePath);
            }
            if (Files.exists(parent) && !Files.isDirectory(parent)) {
                throw new IllegalArgumentException("已经存在同名文件: " + parent);
            }
            Files.createDirectories(parent);
            Files.createFile(target);
        }
        return target;
    }
}
