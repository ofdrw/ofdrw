package org.ofdrw.converter.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.PDFRenderer;
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
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.reader.model.StampAnnotEntity;
import org.ofdrw.core.signatures.appearance.StampAnnot;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
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
     * 整页栅格回退的目标分辨率，单位 dpi（参考实现的整页快照为 300 dpi）
     */
    private static final int SNAPSHOT_DPI = 300;

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
     * 页对象 ID → 该页的签章注释（印章图像 + 定位边界）
     */
    private java.util.Map<String, List<SealStamp>> sealStampsByPage;

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
            // 底纹单元内的文字必须写进图案内容流，由回放器转交给文字写入器
            replayer.usePatternText(textWriter, fontRegistry);
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

        // 设计 3.8：注释的静态外观（水印/图章/签名外观）由页面内容重绘，PDF 注释对象只承载交互；
        // 不写出没有 /AP 的 /Stamp 注释，避免阅读器合成默认外观（poppler 的整页 "Draft"）。
        List<Annot> annots = annotationsOf(pageId);
        if (!annots.isEmpty()) {
            PdfPageContent annotContent =
                    pipeline.pageRenderer().renderAnnotations(page, annots, pipeline.fontSelector());
            content.addAll(annotContent.getCommands());
            if (annotContent.isImageResourceMissing()) {
                content.markImageResourceMissing();
            }
        }

        try (PDPageContentStream stream = new PDPageContentStream(document, pdPage)) {
            PDResources resources = pdPage.getResources();
            if (resources == null) {
                resources = new PDResources();
                pdPage.setResources(resources);
            }
            replayCommands(pipeline, replayer, textWriter, stream, resources, content.getCommands(), pdPage);
            drawSealStamps(stream, pageId, box);
        }
        // 参考实现：带注释外观的页面若存在无法解析的图像引用，会输出 300 dpi 整页快照覆盖页面
        // （矢量内容仍保留在快照之下，文本可提取）。无注释的页面仅跳过缺失图像，不做整页栅格化。
        if (content.isImageResourceMissing() && !annots.isEmpty()) {
            drawPageSnapshot(pdPage, box);
        }
        exportedPages.add(pdPage);
        exportedPageIds.add(pageId);
    }

    /**
     * 回放内容指令：路径/图像交给回放器，文字交给写入器
     *
     * @param pipeline   渲染管线
     * @param replayer   指令回放器
     * @param textWriter 文字写入器
     * @param stream     目标内容流
     * @param resources  该内容流的资源字典
     * @param commands   内容指令
     * @param page       目标页；为 {@code null} 表示写入注释外观流
     * @throws IOException 写入失败
     */
    private void replayCommands(PageRendererImpl.RenderPipeline pipeline, PdfCommandReplayer replayer,
                                PdfTextWriter textWriter, PDPageContentStream stream, PDResources resources,
                                List<org.ofdrw.converter.pdf.model.ContentCommand> commands, PDPage page)
            throws IOException {
        final TrueTypePdfFont.Registry registry = pipeline.registry();
        replayer.replay(document, stream, resources, commands, command -> {
            if (!(command instanceof TextCommand)) {
                return false;
            }
            TextCommand text = (TextCommand) command;
            TrueTypePdfFont font = registry.lookupByResourceName(text.getFontRef());
            if (font == null) {
                logger.warn("[{}] 文字字体资源未登记，已跳过：{}", DIAGNOSTIC_TAG, text.getFontRef());
                return true;
            }
            textWriter.write(document, page, stream, resources, font, text);
            return true;
        });
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
     * 绘制指定页的电子签章图像
     * <p>
     * 签章外观来自 {@code Signs/*}：{@code StampAnnot} 给出页对象 ID、外观边界与可选裁剪区域，
     * 印章图片取自 {@code SES} 电子印章数据。边界与裁剪区域均为 mm，原点在页面左上角、Y 轴向下。
     *
     * @param stream 页面内容流
     * @param pageId 页对象 ID
     * @param box    页面框（pt），用于 Y 轴翻转
     * @throws IOException 写入失败
     */
    private void drawSealStamps(PDPageContentStream stream, String pageId, Box box) throws IOException {
        List<SealStamp> stamps = sealStampsOf(pageId);
        for (SealStamp stamp : stamps) {
            StampAnnot annot = stamp.annot;
            PDImageXObject xobject;
            try {
                xobject = createSealXObject(stamp.entity);
            } catch (Exception e) {
                logger.warn("[{}] 签章图像无法解析，已跳过：{}（{}）", DIAGNOSTIC_TAG,
                        annot.getID(), e.getMessage());
                continue;
            }
            if (xobject == null) {
                continue;
            }
            org.ofdrw.core.basicType.ST_Box boundary = annot.getBoundary();
            org.ofdrw.core.basicType.ST_Box clip = annot.getClip();
            final double k = CoordSystem.CONTENT_MM_TO_PT;
            // 页面高度由 pt 换算回 mm，才能与 Boundary 的 mm 坐标一起做 Y 轴翻转
            final double pageHeightMm = box.getHeight() * CoordSystem.PT_TO_MM;
            float x = (float) (boundary.getTopLeftX() * k);
            float y = (float) ((pageHeightMm - boundary.getTopLeftY() - boundary.getHeight()) * k);
            float w = (float) (boundary.getWidth() * k);
            float h = (float) (boundary.getHeight() * k);
            stream.saveGraphicsState();
            if (clip != null && clip.getWidth() > 0 && clip.getHeight() > 0) {
                float cx = (float) ((boundary.getTopLeftX() + clip.getTopLeftX()) * k);
                float cy = (float) ((pageHeightMm - boundary.getTopLeftY() - clip.getTopLeftY()
                        - clip.getHeight()) * k);
                stream.addRect(cx, cy, (float) (clip.getWidth() * k), (float) (clip.getHeight() * k));
                stream.clip();
            }
            /*
             * 参考实现的签章外观统一以 /BM /Darken 绘制（见基准 PDF 的签章 Form XObject ExtGState）：
             * 印章图片多为白底 RGB 无透明通道，Darken 让白色不覆盖页面背景，
             * 避免在页面上留下白色方块。
             */
            PDExtendedGraphicsState blend = new PDExtendedGraphicsState();
            blend.setBlendMode(BlendMode.DARKEN);
            blend.setNonStrokingAlphaConstant(1f);
            blend.setStrokingAlphaConstant(1f);
            stream.setGraphicsStateParameters(blend);
            stream.drawImage(xobject, x, y, w, h);
            stream.restoreGraphicsState();
        }
    }

    /**
     * 生成印章图像对象
     * <p>
     * SES 印章图片可能是位图（PNG/JPEG/GIF），也可能是嵌套 OFD 页面：
     * <ul>
     *     <li>位图直接按原始字节嵌入，保留 PNG 的索引色/透明通道等信息；</li>
     *     <li>嵌套 OFD 首页按 300 dpi 渲染为透明背景位图（与参考实现一致），透明区域转为 /SMask。</li>
     * </ul>
     *
     * @param entity 签章实体
     * @return PDF 图像对象；无可用图像返回 {@code null}
     * @throws IOException 读取或嵌入失败
     */
    private PDImageXObject createSealXObject(StampAnnotEntity entity) throws IOException {
        byte[] bytes = entity.getImageByte();
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        String type = entity.getImgType();
        if (type != null && "ofd".equalsIgnoreCase(type.trim())) {
            BufferedImage image = renderSealOfd(bytes);
            return image == null ? null : LosslessFactory.createFromImage(document, image);
        }
        try {
            // PNG 等 PDFBox 支持的格式直接嵌入，保留索引色与透明通道（参考实现同样保留）
            return PDImageXObject.createFromByteArray(document, bytes, "Seal" + entity.getStampAnnots().get(0).getID());
        } catch (IOException | RuntimeException e) {
            // GIF 等 PDFBox 不支持的格式解码为位图后无损重建
            try (ByteArrayInputStream input = new ByteArrayInputStream(bytes)) {
                BufferedImage image = ImageIO.read(input);
                return image == null ? null : LosslessFactory.createFromImage(document, image);
            }
        }
    }

    /**
     * 渲染嵌套 OFD 印章首页
     * <p>
     * 采用 300 dpi 与透明背景：透明背景使抗锯齿边缘转为 /SMask，与参考 PDF 的印章图像结构一致。
     *
     * @param ofdBytes 嵌套 OFD 文件字节
     * @return 印章位图（ARGB）；无可用页面返回 {@code null}
     * @throws IOException 读取失败
     */
    private BufferedImage renderSealOfd(byte[] ofdBytes) throws IOException {
        try (OFDReader sealReader = new OFDReader(new ByteArrayInputStream(ofdBytes))) {
            CanvasImageMaker maker = new CanvasImageMaker(
                    sealReader, org.ofdrw.converter.utils.CommonUtil.dpiToPpm(300));
            try {
                return maker.makePage(0);
            } catch (RuntimeException e) {
                logger.warn("[{}] 嵌套 OFD 印章渲染失败：{}", DIAGNOSTIC_TAG, e.getMessage());
                return null;
            }
        }
    }

    /**
     * 以整页栅格覆盖当前页面
     * <p>
     * 用于页面存在无法解析的图像引用时的回退：参考实现会输出 300 dpi 的整页快照并覆盖页面，
     * 矢量内容与文字层保留在快照之下，文本仍可提取。
     *
     * @param page 目标页面
     * @param box  页面框（pt）
     * @throws IOException 渲染或写入失败
     */
    private void drawPageSnapshot(PDPage page, Box box) throws IOException {
        BufferedImage snapshot;
        try {
            PDFRenderer renderer = new PDFRenderer(document);
            snapshot = renderCeil(renderer, document.getNumberOfPages() - 1, box, SNAPSHOT_DPI);
        } catch (IOException | RuntimeException e) {
            logger.warn("[{}] 整页栅格回退失败，保留矢量输出：{}", DIAGNOSTIC_TAG, e.getMessage());
            return;
        }
        PDImageXObject image = LosslessFactory.createFromImage(document, snapshot);
        try (PDPageContentStream stream = new PDPageContentStream(
                document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
            stream.drawImage(image, (float) box.getX(), (float) box.getY(),
                    (float) box.getWidth(), (float) box.getHeight());
        }
    }

    /**
     * 按整数像素栅格化整页，像素尺寸按向上取整
     * <p>
     * 页面尺寸（pt）换算成像素时通常不是整数，渲染器取整方式不同会让整页快照相差 1 像素
     * （例如 419.9996 mm × 300 dpi = 4960.62 px，向下取整得 4960、向上取整得 4961），
     * 进而使快照的采样网格与参考实现错开。这里统一按 ceil 计算目标像素尺寸，
     * 渲染尺寸不足时缩放补齐（差值不超过 1 像素）。
     *
     * @param renderer  页面渲染器
     * @param pageIndex 页面下标，从 0 开始
     * @param box       页面框，单位 pt
     * @param dpi       目标分辨率，单位 dpi
     * @return 目标像素尺寸的整页图像
     * @throws IOException 渲染失败
     */
    private static BufferedImage renderCeil(PDFRenderer renderer, int pageIndex, Box box, int dpi)
            throws IOException {
        double perPt = dpi / 72d;
        int width = (int) Math.ceil(box.getWidth() * perPt);
        int height = (int) Math.ceil(box.getHeight() * perPt);
        BufferedImage rendered = renderer.renderImage(pageIndex, (float) perPt);
        if (rendered.getWidth() == width && rendered.getHeight() == height) {
            return rendered;
        }
        // 尺寸通常只差 1 像素：按目标尺寸重采样，使整页采样网格与参考实现的 ceil 规则一致
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D graphics = scaled.createGraphics();
        try {
            graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                    java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.drawImage(rendered, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return scaled;
    }

    /**
     * 读取指定页的签章注释
     *
     * @param pageId 页对象 ID
     * @return 签章注释列表；无签章时返回空列表
     */
    private List<SealStamp> sealStampsOf(String pageId) {
        if (sealStampsByPage == null) {
            sealStampsByPage = new java.util.HashMap<>();
            try {
                for (StampAnnotEntity entity : ofdReader.getStampAnnots()) {
                    if (entity == null || entity.getImageByte() == null) {
                        continue;
                    }
                    for (StampAnnot annot : entity.getStampAnnots()) {
                        if (annot == null || annot.getBoundary() == null) {
                            continue;
                        }
                        String ref = annot.getPageRef().toString();
                        List<SealStamp> list = sealStampsByPage.get(ref);
                        if (list == null) {
                            list = new ArrayList<>();
                            sealStampsByPage.put(ref, list);
                        }
                        list.add(new SealStamp(annot, entity));
                    }
                }
            } catch (RuntimeException e) {
                logger.warn("[{}] 签章信息读取失败，本页不绘制印章：{}", DIAGNOSTIC_TAG, e.getMessage());
            }
        }
        List<SealStamp> stamps = sealStampsByPage.get(pageId);
        return stamps == null ? java.util.Collections.<SealStamp>emptyList() : stamps;
    }

    /**
     * 单个签章注释及其印章图片
     */
    private static final class SealStamp {

        /**
         * 签章注释（定位边界、裁剪区域）
         */
        private final StampAnnot annot;

        /**
         * 签章实体（印章图像）
         */
        private final StampAnnotEntity entity;

        /**
         * @param annot  签章注释
         * @param entity 签章实体
         */
        SealStamp(StampAnnot annot, StampAnnotEntity entity) {
            this.annot = annot;
            this.entity = entity;
        }
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
