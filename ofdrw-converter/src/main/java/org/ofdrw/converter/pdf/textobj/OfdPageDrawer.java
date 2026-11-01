package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDAbstractPattern;
import org.apache.pdfbox.pdmodel.graphics.pattern.PDTilingPattern;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.PDLineDashPattern;
import org.apache.pdfbox.pdmodel.graphics.color.PDColor;
import org.apache.pdfbox.contentstream.PDContentStream;
import org.apache.pdfbox.contentstream.PDFStreamEngine;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDSoftMask;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.ofdrw.core.annotation.Annotations;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.FillColor;
import org.ofdrw.converter.renderer.CanvasPaint;
import org.ofdrw.converter.renderer.Graphics2DRenderer;
import org.ofdrw.converter.renderer.ResolvedPaint;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.clips.CT_Clip;
import org.ofdrw.core.pageDescription.clips.Clips;
import org.ofdrw.graphics2d.OFDPageGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.TexturePaint;
import java.awt.image.BufferedImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * OFD 页面绘制器：在 PDFBox {@link PageDrawer} 的字形级入口拦截文字，输出 OFD 文字对象
 * <p>
 * 职责仅限“拦截与分流”：
 * <ul>
 *     <li>能对象化为 {@link TextObject} 的字形交给 {@link TextRunBuilder} 收集；</li>
 *     <li>Type3 字体、RenderingMode 4~7（文字裁剪）、离屏画布（透明组/图案/软掩码）、非内嵌字体等场景
 *     一律调用 {@code super} 回退为字形路径绘制；</li>
 *     <li>图形、图片仍由父类绘制到 {@link OFDPageGraphics2D}，并保证文字与图形的 z-order 与内容流一致。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OfdPageDrawer extends PageDrawer {

    private static final Logger logger = LoggerFactory.getLogger(OfdPageDrawer.class);

    /**
     * 页面主画布
     */
    private final OFDPageGraphics2D pageGraphics;

    /**
     * 页面主画布（由 {@link #prepareCanvas(Graphics2D)} 记录，用于与当前画布比较）
     */
    private Graphics2D pageCanvas;

    /**
     * 文字对象化是否已在本页画布上启用（页面顶层内容流内、且画布就是 drawPage 传入的主画布）
     */
    private boolean started = false;

    /**
     * 字体登记函数：返回 OFD 字体资源 ID，无法嵌入时返回 null
     */
    private final Function<PDFont, ST_ID> fontRegistrar;

    /**
     * 注释单元字体登记函数（允许系统字体兜底），可为 null
     * <p>
     * 与 {@link #fontRegistrar} 分离：正文渲染对“无内嵌字体”保持路径回退策略，
     * 只有平铺底纹/水印单元这类页面外资源域才允许用系统字体替代，避免整篇文档被替换成完整 CJK 字体。
     */
    private java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> annotationFontRegistrar;

    /**
     * 字形使用登记函数：记录该字体实际使用的 PDF 原始 GID
     */
    private final GlyphUsageRegistrar glyphRegistrar;

    /**
     * 降级告警聚合器
     */
    private final PdfTextWarningCollector warnings;

    /**
     * CGTransform GID 解码器：原样嵌入时恒等，子集化后按 old→new 映射换算
     */
    private final GidDecoder gidDecoder;

    /**
     * CGTransform GID 延迟登记函数：{@code (PDF 字体, Glyphs 节点, PDF 原始 GID 列表)}，
     * 子集化时由调用方注入，在 OFD 打包前把 Glyphs 改写为最终 GID；为 null 表示 GID 不变
     */
    private final GidTracker gidTracker;

    /**
     * 当前页页码（从 1 起）
     */
    private final int pageNumber;

    /**
     * 当前文字 run
     */
    private TextRunBuilder run = null;

    /**
     * 当前 run 的字体
     */
    private PDFont runFont = null;

    /**
     * 当前页的 PDF 用户空间页面框（单位 pt，原点在左下），每页开始时刷新
     */
    private Rectangle2D pageBox = null;

    /**
     * 当前页的缩放比例（PDF 用户空间 pt → OFD 页面 mm），每页开始时刷新
     */
    private float pageScale = 0f;

    /**
     * 当前 run 生效的裁剪区归属键（0 表示无裁剪）
     */
    private int runClipKey = 0;

    /**
     * 当前裁剪区对应的 OFD 元素（文字对象直接引用同一元素，保证裁剪只输出一次）
     */
    private Clips runClipElement = null;

    /**
     * 当前裁剪区覆盖整页（不写 Clips，但仍然需要以此判断 run 边界）
     */
    private boolean runClipFullPage = false;

    /**
     * 当前画布的字形解析函数（未启用文字对象化时为 null，表示一律回退）
     */
    private GlyphResolver currentResolver = null;

    /**
     * 字形使用登记函数（记录 PDF 原始 GID 与 Unicode，供字体子集化重建 cmap）
     */
    @FunctionalInterface
    public interface GlyphUsageRegistrar {
        /**
         * 记录一次字形使用
         *
         * @param font    PDF 字体
         * @param code    PDF 字符编码
         * @param gid     PDF 原始字形索引
         * @param unicode 该字形对应的 Unicode 码位，未知时为 -1
         */
        void register(PDFont font, int code, int gid, int unicode);
    }

    /**
     * CGTransform GID 解码函数：把 PDF 原始 GID 换算为最终写入 OFD 的 GID
     */
    @FunctionalInterface
    interface GidDecoder {
        /**
         * 换算 GID
         *
         * @param font   PDF 字体
         * @param pdfGid PDF 原始字形索引
         * @return 最终 GID（原样嵌入时等于 pdfGid）
         */
        int decode(PDFont font, int pdfGid);
    }

    /**
     * CGTransform GID 延迟登记函数：登记“待改写为最终 GID”的节点
     */
    @FunctionalInterface
    interface GidTracker {
        /**
         * 登记一条 CGTransform 的 GID 段
         *
         * @param font          PDF 字体
         * @param glyphsElement CGTransform 的 Glyphs 节点
         * @param pdfGids       该段对应的 PDF 原始 GID 列表
         */
        void track(PDFont font, org.dom4j.Element glyphsElement, org.ofdrw.core.basicType.ST_Array pdfGids);
    }

    /**
     * 字形解析函数：给出该字体在 OFD 中的字体资源 ID 与字形最终 GID
     */
    @FunctionalInterface
    interface GlyphResolver {
        /**
         * 解析字形
         *
         * @param font PDF 字体
         * @param code PDF 字符编码
         * @param pdfGid PDF 原始字形索引
         * @return 解析结果；无法对象化时返回 null
         */
        Resolved resolve(PDFont font, int code, int pdfGid);
    }

    /**
     * 字形解析结果
     */
    static final class Resolved {
        /**
         * OFD 字体资源 ID
         */
        final ST_ID fontId;
        /**
         * CGTransform 使用的最终 GID
         */
        final int gid;
        /**
         * 是否需要强制写 CGTransform（CID/Identity 字体）
         */
        final boolean cidFont;

        /**
         * @param fontId   OFD 字体资源 ID
         * @param gid      最终 GID
         * @param cidFont  是否 CID 字体
         */
        Resolved(ST_ID fontId, int gid, boolean cidFont) {
            this.fontId = fontId;
            this.gid = gid;
            this.cidFont = cidFont;
        }
    }

    /**
     * 创建页面绘制器（不带 GID 解码器，CGTransform 使用 PDF 原始 GID）
     *
     * @param parameters     PDFBox 页面绘制参数
     * @param pageGraphics   页面主画布
     * @param fontRegistrar  字体登记函数
     * @param glyphRegistrar 字形使用登记函数
     * @param warnings       降级告警聚合器
     * @param pageNumber     页码（从 1 起）
     * @throws IOException PDF 解析异常
     */
    OfdPageDrawer(PageDrawerParameters parameters, OFDPageGraphics2D pageGraphics,
                  Function<PDFont, ST_ID> fontRegistrar,
                  GlyphUsageRegistrar glyphRegistrar, PdfTextWarningCollector warnings, int pageNumber)
            throws IOException {
        this(parameters, pageGraphics, fontRegistrar, glyphRegistrar, warnings, pageNumber, null);
    }

    /**
     * 创建页面绘制器（带 GID 解码器）
     *
     * @param parameters     PDFBox 页面绘制参数
     * @param pageGraphics   页面主画布
     * @param fontRegistrar  字体登记函数
     * @param glyphRegistrar 字形使用登记函数
     * @param warnings       降级告警聚合器
     * @param pageNumber     页码（从 1 起）
     * @param gidDecoder     CGTransform GID 解码器，可为 null（表示 GID 不变）
     * @throws IOException PDF 解析异常
     */
    OfdPageDrawer(PageDrawerParameters parameters, OFDPageGraphics2D pageGraphics,
                  Function<PDFont, ST_ID> fontRegistrar,
                  GlyphUsageRegistrar glyphRegistrar, PdfTextWarningCollector warnings, int pageNumber,
                  GidDecoder gidDecoder) throws IOException {
        this(parameters, pageGraphics, fontRegistrar, glyphRegistrar, warnings, pageNumber, gidDecoder, null);
    }

    /**
     * 创建页面绘制器（带 GID 解码器与 GID 延迟登记）
     *
     * @param parameters     PDFBox 页面绘制参数
     * @param pageGraphics   页面主画布
     * @param fontRegistrar  字体登记函数
     * @param glyphRegistrar 字形使用登记函数
     * @param warnings       降级告警聚合器
     * @param pageNumber     页码（从 1 起）
     * @param gidDecoder     CGTransform GID 解码器，可为 null（表示 GID 不变）
     * @param gidTracker     CGTransform GID 延迟登记函数，可为 null（表示 GID 不变）
     * @throws IOException PDF 解析异常
     */
    OfdPageDrawer(PageDrawerParameters parameters, OFDPageGraphics2D pageGraphics,
                  Function<PDFont, ST_ID> fontRegistrar,
                  GlyphUsageRegistrar glyphRegistrar, PdfTextWarningCollector warnings, int pageNumber,
                  GidDecoder gidDecoder, GidTracker gidTracker) throws IOException {
        super(parameters);
        this.parameters = parameters;
        this.pageGraphics = pageGraphics;
        this.fontRegistrar = fontRegistrar;
        this.glyphRegistrar = glyphRegistrar;
        this.gidTracker = gidTracker;
        this.gidDecoder = gidDecoder == null ? OfdPageDrawer::identityGid : gidDecoder;
        this.warnings = warnings;
        this.pageNumber = pageNumber;
        // 软掩码/非 Normal 混合模式在 OFD 中无等价表达，进入时收起当前 run，期间文字一律回退路径
        registerSafeProcessor("gs");
        registerSafeProcessor("BDC");
    }

    /**
     * 开始绘制页面内容流（每页开始调用）
     * <p>
     * 此时父类的画布变换已设置完成，可据此判定页面主画布并准备好字形解析函数。
     *
     * @param g        绘制目标
     * @param pageSize 页面尺寸
     * @throws IOException PDF 解析异常
     */
    @Override
    public void drawPage(Graphics2D g, org.apache.pdfbox.pdmodel.common.PDRectangle pageSize) throws IOException {
        pageBox = pageSize == null ? null
                : new Rectangle2D.Double(0d, 0d, pageSize.getWidth(), pageSize.getHeight());
        // 画布初始变换的线性部分即“pt → 页面 mm”的缩放（翻页矩阵只含平移与 y 翻转，不改变水平尺度）
        pageScale = (float) Math.abs(g.getTransform().getScaleX());
        prepareCanvas(g);
        super.drawPage(g, pageSize);
        flushRun();
    }

    /**
     * 文字对象化是否处于暂停状态（软掩码/混合模式作用范围内）
     *
     * @return true - 暂停，文字走路径回退
     */
    boolean isTextSuspended() {
        return textSuspended;
    }

    /**
     * 设置文字对象化暂停状态
     *
     * @param suspended true - 暂停
     */
    void setTextSuspended(boolean suspended) {
        this.textSuspended = suspended;
    }

    /**
     * 文字对象化暂停状态
     */
    private boolean textSuspended = false;

    /**
     * 页面画布上的翻页矩阵（PDF 用户空间 → 设备空间：平移 + y 翻转），每页开始时从画布变换中还原
     */
    private AffineTransform pageFlip = new AffineTransform();

    /**
     * 页面画布的“图像变换”（PDF 用户空间单位 pt → OFD 页面 mm，缩放 + y 翻转），每页开始时记录
     * <p>
     * PDFBox 在渲染每页前还会通过 {@code Graphics2D#translate/scale} 施加“图像变换 + 翻页矩阵”，
     * 这部分<b>不</b>包含在本字段中；需要完整变换时调用 {@link #deviceTransform()}。
     */
    private AffineTransform pageTransform = null;

    /**
     * 已登记的“安全处理器”名称（软掩码/混合模式相关算子）
     */
    private final Set<String> safeOperators = new HashSet<>();

    /**
     * 平铺底纹渲染时的临时画布（非 null 时绘制操作落到该离屏画布，而不是页面画布）
     */
    private Graphics2D tilingGraphics = null;

    /**
     * PDFBox 页面绘制参数（用于取当前页资源，解析 Pattern/字体等）
     */
    private final PageDrawerParameters parameters;

    /**
     * 平铺底纹位图缓存：key = 底纹哈希 + 像素尺寸，value = 单元位图
     */
    private final Map<String, BufferedImage> tilingTiles = new HashMap<>();

    /**
     * 当前页是否已写出水印注释（同页多次填充同一底纹只写一条）
     */
    private boolean watermarkWritten = false;

    /**
     * 注释页面目录序号（Doc_N/Annots/Page_M）
     */
    private int annotPageIndex = 0;

    /**
     * 单页平铺底纹的最大格数（防止超大页面生成海量对象）
     */
    private static final long TILING_MAX_CELLS = 20000L;

    /**
     * 平铺底纹单元位图的像素缩放倍数（相对 pattern 单元坐标 1pt = 1px）
     */
    private static final double TILING_TILE_SCALE = 12d;

    /** 临时诊断开关 */


    /** 临时诊断计数 */



    /**
     * 无裁剪（覆盖整页）的裁剪结果：不写 Clips、归属键为 0
     */
    private static final OfdClips FULL_PAGE_CLIPS = new OfdClips(null, 0, true);

    /**
     * 当前 run 使用的字体 → 字体资源 ID
     */
    private final Map<PDFont, ST_ID> fontIds = new HashMap<>();

    /**
     * 把 pattern 的内容流写入一个临时页面并渲染成位图
     * <p>
     * 不依赖 PDFBox 内部的内容流处理接口：临时文档只有一页，尺寸取 pattern 的 BBox，
     * 页面内容流就是 pattern 自身的字节（外层包一层 q/Q），再用 {@link PDFRenderer} 渲染，
     * 因此不会重放原页面内容。
     *
     * @param pattern 平铺底纹
     * @param scale   单元坐标（pt）→ 位图像素的缩放倍数
     * @return 单元位图；渲染失败时返回 null
     */
    private BufferedImage renderPatternTile(PDTilingPattern pattern, double scale) {
        if (pattern == null || parameters == null || parameters.getPage() == null || scale <= 0d) {
            return null;
        }
        try {
            byte[] content = toByteArray(pattern.getContents());
            try (org.apache.pdfbox.pdmodel.PDDocument temp =
                         new org.apache.pdfbox.pdmodel.PDDocument()) {
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(pattern.getBBox());
                page.setMediaBox(pattern.getBBox());
                // 复用原文档的资源（字体等），pattern 内容流里的名称在临时页里同样有效
                page.setResources(pattern.getResources());
                page.setContents(new org.apache.pdfbox.pdmodel.common.PDStream(
                        temp, new java.io.ByteArrayInputStream(content)));
                temp.addPage(page);
                /*
                 * 临时页把 pattern 单位当作 pt 渲染，而单元变换把 pattern 单位当作“页面毫米”使用，
                 * 二者相差 pt → mm 的比例（1/0.3528 ≈ 2.83）。因此位图的分辨率必须按
                 * scale × 72 / pageScale 计算：1 pattern 单位 = scale/pageScale 像素，
                 * 配合绘制端的 1/scale 反缩放后，1 pattern 单位恰好落在 1 毫米上。
                 */
                org.apache.pdfbox.rendering.PDFRenderer renderer =
                        new org.apache.pdfbox.rendering.PDFRenderer(temp);
                float dpi = (float) (scale * 72d);
                return renderer.renderImageWithDPI(0, dpi);
            }
        } catch (IOException | RuntimeException e) {
            logger.warn("平铺底纹单元渲染失败（{}），已跳过绘制", e.toString());
            return null;
        }
    }

    /**
     * 读取输入流的全部字节
     *
     * @param in 输入流；null 时返回空数组
     * @return 字节数组
     * @throws IOException 读取异常
     */
    private static byte[] toByteArray(java.io.InputStream in) throws IOException {
        if (in == null) {
            return new byte[0];
        }
        try (java.io.InputStream stream = in) {
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = stream.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    /**
     * 取“PDF 用户空间 → OFD 页面 mm”的完整变换
     * <p>
     * 由页首记录的图像变换再叠加画布当前变换（含翻页矩阵）得到；PDFBox 对整页施加的 y 翻转为负缩放，
     * 因此结果中 y 轴向下，与 OFD 页面坐标一致。
     *
     * @return 完整变换（含缩放与平移，单位 mm）
     */
    private AffineTransform deviceTransform() {
        return currentDevice();
    }

    /**
     * 当前图形状态下的“PDF 用户空间 → OFD 页面 mm”完整变换
     * <p>
     * Form XObject 内部绘图时，PDFBox 会把 Form 的矩阵合进当前 CTM（例如把 form 空间 y 1400~2035pt
     * 平移到页面 27~662pt）。页面级变换 {@link #pageTransform}/{@link #pageFlip} 只在页首计算一次，
     * 因此这里必须再把当前 CTM 叠加进来，否则 form 内的内容会被算到页面外（表现为整块内容压到底部或消失）。
     *
     * @return 合成变换：先应用当前 CTM（对象空间 → PDF 用户空间），再应用页面变换（pt → 页面 mm）
     */
    private AffineTransform currentDevice() {
        AffineTransform transform = pageFlip == null ? new AffineTransform() : new AffineTransform(pageFlip);
        PDGraphicsState state = getGraphicsState();
        if (state != null) {
            Matrix ctm = state.getCurrentTransformationMatrix();
            if (ctm != null && !isIdentity(ctm)) {
                AffineTransform linear = new AffineTransform(ctm.getScaleX(), ctm.getShearY(), ctm.getShearX(),
                        ctm.getScaleY(), 0d, 0d);
                // 页面变换只负责 pt → mm 的缩放与 y 翻转；平移与缩放比例全部由 CTM 提供
                transform.concatenate(linear);
            }
        }
        return transform;
    }

    /**
     * 判断 PDFBox 矩阵是否为单位矩阵
     *
     * @param m 矩阵
     * @return true - 单位矩阵
     */
    private static boolean isIdentity(Matrix m) {
        return Math.abs(m.getScaleX() - 1d) < 1e-6 && Math.abs(m.getShearY()) < 1e-6
                && Math.abs(m.getShearX()) < 1e-6 && Math.abs(m.getScaleY() - 1d) < 1e-6
                && Math.abs(m.getTranslateX()) < 1e-6 && Math.abs(m.getTranslateY()) < 1e-6;
    }

    /**
     * 当前图形状态下的“文字空间 → 页面 mm”变换，用于字形轮廓与字体度量换算
     * <p>
     * PDFBox 的 {@code PageDrawer} 在调用 {@code showFontGlyph} 前已经把画布变换求逆放进当前 CTM，
     * 因此传入的 TRM 已经是“PDF 用户空间（pt）”口径，不再需要叠加 CTM。若再乘 CTM 的线性部分，
     * 会与 pageTransform 的 mm/pt 缩放正好相互抵消（实测 CTM 缩放 = 1/pageScale ≈ 2.8346），
     * 使字号被放大 1/pageScale 倍并整体偏移出页面。
     *
     * @return 图像变换（pt → 页面 mm，y 向上）；未初始化时返回单位矩阵
     */
    private AffineTransform currentGlyphTransform() {
        return pageTransform == null ? new AffineTransform() : new AffineTransform(pageTransform);
    }

    /**
     * 准备当前画布的文字解析能力
     * <p>
     * 仅当当前 Graphics2D 就是页面主画布时才开启文字对象化，离屏画布（透明组、图案、软掩码）一律回退。
     */
    private void prepareCanvas(Graphics2D g) {
        pageCanvas = g;
        started = false;
        run = null;
        runFont = null;
        currentResolver = null;
        fontIds.clear();
        /*
         * 画布初始变换 = 翻页矩阵（PDF 用户空间 → 设备空间，含 y 翻转与平移）× 图像变换（pt → 页面 mm）。
         * pageTransform 只保留图像变换（y 向上口径，供字形轮廓/包围盒使用），
         * pageFlip 保留完整变换（y 向下口径，供基线原点、裁剪区与底纹平铺使用）。
         */
        /*
         * 图像变换 = 纯缩放 scale（PDF 用户空间 pt → OFD 页面 mm）再叠加 y 翻转：
         * 页面的 y 翻转与平移由 pageFlip 负责，不能从画布变换反解（画布尺寸单位是 mm，会混入 mm/pt 比例）。
         */
        pageFlip = new AffineTransform(g.getTransform());
        if (pageBox != null && pageBox.getHeight() > 0d && pageScale > 0f) {
            pageTransform = new AffineTransform(pageScale, 0d, 0d, -pageScale, 0d, pageBox.getHeight() * pageScale);
        } else {
            pageTransform = new AffineTransform(g.getTransform());
        }
        started = true;
        currentResolver = (font, code, pdfGid) -> {
            ST_ID fontId = fontRegistrar.apply(font);
            if (fontId == null) {
                return null;
            }
            /*
             * 字体子集化后 GID 会重排，CGTransform 必须使用最终 GID。
             * 子集化的 old→new 映射只有在字体写出（flush）后才确定，而字体文件在 OFD 打包时（close）
             * 才写入；文字对象在渲染阶段就已追加到页面内容中，因此这里返回“延迟解析”的 GID：
             * 由 PdfTextFonts 先把原始 GID 写回 TextObject，打包前再通过 GidDecoder 统一改写。
             */
            int gid = isCidFont(font) ? gidDecoder.decode(font, pdfGid) : pdfGid;
            return new Resolved(fontId, gid, isCidFont(font));
        };
    }

    /**
     * 取该字体的 CGTransform GID 登记函数（子集化时登记节点，打包前统一改写为最终 GID）
     *
     * @param font PDF 字体
     * @return 登记函数；未启用子集化（GID 不变）时返回 null
     */
    private GidTracker gidTrackerFor(PDFont font) {
        return gidTracker;
    }

    /**
     * 恒等 GID 解码（原样嵌入，不做子集化）
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始字形索引
     * @return 原 GID
     */
    private static int identityGid(PDFont font, int pdfGid) {
        return pdfGid;
    }

    /**
     * 用“安全处理器”替换 PDFBox 的算子实现：执行前后收起当前 run，
     * 并在算子执行期间禁止文字对象化（软掩码、非 Normal 混合模式时文字带回退路径）
     *
     * @param operatorName 算子名称（如 "gs"、"BDC"）
     */
    private void registerSafeProcessor(String operatorName) {
        if (!safeOperators.add(operatorName)) {
            return;
        }
        if ("gs".equals(operatorName)) {
            addOperator(new SafeGraphicsStateProcessor(this));
        } else if ("BDC".equals(operatorName)) {
            addOperator(new SafeMarkedContentProcessor(this));
        }
    }

    /**
     * 字形级入口：能对象化的字形收集到当前文字 run，否则回退父类路径绘制
     *
     * @param textRenderingMatrix 文字渲染矩阵（文字空间 → PDF 用户空间）
     * @param font                PDF 字体
     * @param code                字符编码
     * @param displacement        字形位移
     * @throws IOException PDF 解析异常
     */
    @Override
    protected void showFontGlyph(Matrix textRenderingMatrix, PDFont font, int code, Vector displacement) throws IOException {
        if (tilingGraphics != null) {
            // 底纹单元里的文字直接画到离屏画布（路径），不进入文字对象化流程
            super.showFontGlyph(textRenderingMatrix, font, code, displacement);
            return;
        }
        PDGraphicsState gs = getGraphicsState();
        if (gs == null || font == null) {
            flushRun();
            super.showFontGlyph(textRenderingMatrix, font, code, displacement);
            return;
        }
        RenderingMode mode = gs.getTextState().getRenderingMode();
        if (textSuspended) {
            fallback("软掩码或混合模式作用范围内", font, textRenderingMatrix, code, displacement);
            return;
        }


        if (!started || getGraphics() != pageCanvas) {
            fallback("非页面主画布", font, textRenderingMatrix, code, displacement);
            return;
        }
        String reason = unsupportedReason(font, mode, textRenderingMatrix);
        if (reason != null) {
            fallback(reason, font, textRenderingMatrix, code, displacement);
            return;
        }
        int pdfGid = PdfFontHelper.codeToGid(font, code);
        if (pdfGid < 0) {
            fallback("无法取得字形索引", font, textRenderingMatrix, code, displacement);
            return;
        }
        if (gs.getBlendMode() != BlendMode.NORMAL) {
            fallback("非 Normal 混合模式", font, textRenderingMatrix, code, displacement);
            return;
        }
        PDSoftMask softMask = gs.getSoftMask();
        if (softMask != null) {
            fallback("软掩码", font, textRenderingMatrix, code, displacement);
            return;
        }
        String strokeReason = strokeFallbackReason(gs, mode);
        if (strokeReason != null) {
            fallback(strokeReason, font, textRenderingMatrix, code, displacement);
            return;
        }
        Resolved resolved = currentResolver == null ? null : currentResolver.resolve(font, code, pdfGid);
        if (resolved == null) {
            fallback("字体不可嵌入", font, textRenderingMatrix, code, displacement);
            return;
        }
        TextRunBuilder.Glyph glyph = buildGlyph(textRenderingMatrix, font, code, pdfGid, resolved);
        if (glyph == null) {
            fallback("文字矩阵退化", font, textRenderingMatrix, code, displacement);
            return;
        }
        if (glyphRegistrar != null) {
            try {
                glyphRegistrar.register(font, code, pdfGid, glyph.unicode.isEmpty() ? -1 : glyph.unicode.codePointAt(0));
            } catch (RuntimeException e) {
                logger.debug("字形使用登记失败：{}", PdfFontHelper.fontName(font), e);
            }
        }
        OfdClips clips = buildClips(gs);
        if (clips == null) {
            clips = FULL_PAGE_CLIPS;
        }
        int clipKey = clips.key;
        if (run != null && runFont == font && runClipKey != clipKey) {
            // 裁剪区变化：先收掉上一段，避免一个文字对象携带两段不同的裁剪
            flushRun();
        }
        if (run == null || runFont != font) {
            flushRun();
            TextRunBuilder.TextRunStyle style = buildStyle(gs, mode, textRenderingMatrix);
            run = new TextRunBuilder(font, style, pageGraphics);
            runFont = font;
            runClipKey = clipKey;
            runClipFullPage = clips.fullPage;
            runClipElement = clips.clips;
        }
        if (!runClipFullPage) {
            // 有效裁剪：交给 run，flush 时写入文字对象
            run.setClips(runClipElement);
        }
        run.add(glyph);
        recordFont(font, resolved.fontId);
        if (!run.accepts()) {
            flushRun();
        }
    }

    /**
     * 设置注释单元（平铺底纹/水印）的字体登记函数
     * <p>
     * 该函数允许在字体缺少内嵌程序时用系统字体替代；未设置时注释单元沿用 {@link #fontRegistrar}。
     *
     * @param registrar 字体登记函数
     */
    void setAnnotationFontRegistrar(java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> registrar) {
        this.annotationFontRegistrar = registrar;
    }

    /**
     * 判断当前字形是否必须回退路径绘制
     *
     * @param font 字体
     * @param mode 文字渲染模式
     * @param trm  文字渲染矩阵（文字空间 → PDF 用户空间）
     * @return 回退原因；可对象化时返回 null
     */
    private String unsupportedReason(PDFont font, RenderingMode mode, Matrix trm) {
        if (font instanceof PDType3Font) {
            return "Type3 字体";
        }
        if (!PdfFontHelper.isTrueTypeLike(font)) {
            return "非 TrueType 轮廓字体";
        }
        String transformReason = transformFallbackReason(font, trm);
        if (transformReason != null) {
            return transformReason;
        }
        switch (mode) {
            case FILL:
            case STROKE:
            case FILL_STROKE:
            case NEITHER:
                return null;
            default:
                return "文字渲染模式 " + mode;
        }
    }

    /**
     * 判断文字矩阵是否必须回退路径绘制（竖排、旋转、镜像、剪切或非等比缩放）
     * <p>
     * 页面变换在 OFD 侧已由坐标换算吸收、y 轴翻转由 TextObject 自身表达，因此先剔除页面变换的线性部分，
     * 再看文字矩阵在页面上的剩余变形是否仍满足“沿 x 等比缩放、不旋转、不镜像”。
     *
     * @param font 字体
     * @param trm  文字渲染矩阵（文字空间 → PDF 用户空间）
     * @return 回退原因；可用 Boundary + Delta 表达时返回 null
     */
    private String transformFallbackReason(PDFont font, Matrix trm) {
        if (font instanceof PDType0Font && ((PDType0Font) font).isVertical()) {
            // 竖排文字（WMode=1）：后续按 ReadDirection=90 精确映射，当前版本回退路径
            return "竖排文字";
        }
        if (trm == null) {
            return null;
        }
        AffineTransform trmAwt = new AffineTransform(trm.getScaleX(), trm.getShearY(), trm.getShearX(),
                trm.getScaleY(), 0d, 0d);
        // 与 buildGlyph 同一口径：Pg 只取图像变换（pt → 页面 mm），y 向上
        AffineTransform n = new AffineTransform(pageTransform);
        n.concatenate(trmAwt);
        // 页面变换为对角缩放（含整页 y 翻转），剔除它等价于把线性部分按列除以对应尺度
        double px = n.getScaleX() / (pageTransform == null ? 1d : pageTransform.getScaleX());
        double py = n.getScaleY() / (pageTransform == null ? 1d : pageTransform.getScaleY());
        double a = n.getScaleX() / px;
        double b = n.getShearY() / px;
        double c = n.getShearX() / py;
        double d = n.getScaleY() / py;
        double em = Math.hypot(c, d);
        if (em <= 1e-9 || Double.isNaN(em)) {
            return null;
        }
        double tolerance = 1e-3;
        // 正常横排文字为 (1,0,0,-1)（含 PDF→页面的 y 翻转），此时无需 CTM
        double ux = a / em;
        double uy = b / em;
        double vx = c / em;
        double vy = d / em;
        boolean plain = Math.abs(ux - 1d) <= tolerance && Math.abs(uy) <= tolerance
                && Math.abs(vx) <= tolerance && Math.abs(vy + 1d) <= tolerance;
        if (plain) {
            return null;
        }
        boolean rotated = Math.abs(b) > tolerance || Math.abs(c) > tolerance;
        /*
         * y 轴方向不参与判定：这里只关心“旋转 / 剪切 / 非等比缩放”。
         * PDF 用户空间 y 向上、OFD 页面 y 向下，二者之间的固定翻转由线性部分 (1,0,0,-1) 表达，
         * 属于所有普通横排文字的共性，不应视为需要 CTM 的镜像。
         */
        boolean nonUniform = Math.abs(a / em - 1d) > tolerance;
        if (!rotated && !nonUniform) {
            return null;
        }
        // 旋转/非等比缩放需要 TextObject.CTM；参考查看器（ofdrw-viewer 0.1.5）不渲染带 CTM 的文字对象
        // （字形被按字体单位压扁后不可见），为保证视觉正确保持路径绘制，详见设计文档 5.4.1
        return "旋转/镜像文字需要 CTM";
    }

    /**
     * 根据图形状态构造 run 的绘制属性
     *
     * @param gs                  图形状态
     * @param mode                文字渲染模式
     * @param textRenderingMatrix 文字渲染矩阵
     * @return 绘制属性快照
     */
    private TextRunBuilder.TextRunStyle buildStyle(PDGraphicsState gs, RenderingMode mode, Matrix textRenderingMatrix) {
        boolean fill = mode == RenderingMode.FILL || mode == RenderingMode.FILL_STROKE;
        boolean stroke = mode == RenderingMode.STROKE || mode == RenderingMode.FILL_STROKE;
        double alpha = 1d;
        if (mode == RenderingMode.NEITHER) {
            // 不可见文字：保留可搜索性，但透明度为 0
            alpha = 0d;
        } else {
            if (fill) {
                alpha *= gs.getNonStrokeAlphaConstant();
            }
            if (stroke) {
                alpha *= gs.getAlphaConstant();
            }
        }
        int alpha255 = (int) Math.round(Math.max(0d, Math.min(1d, alpha)) * 255d);
        // 描边线宽：PDF 用户空间 → 页面 mm
        double lineWidth = 0d;
        try {
            double scale = textRenderingMatrix.getScalingFactorY();
            if (scale <= 0 || Double.isNaN(scale)) {
                scale = 1d;
            }
            float widthPt = gs.getLineWidth() * (float) scale;
            lineWidth = Math.abs(pageTransform.getScaleY()) * widthPt;
        } catch (RuntimeException e) {
            logger.debug("计算文字描边线宽失败", e);
        }
        return new TextRunBuilder.TextRunStyle(fill, stroke,
                gs.getNonStrokingColor(), gs.getStrokingColor(), lineWidth, alpha255);
    }

    /**
     * 判断文字描边是否属于“复杂描边”（虚线、非默认线帽/线接），复杂描边在 OFD 文字对象中无法等价表达时回退路径
     * <p>
     * 依据设计文档 5.7：RenderingMode 1/2 只有在“纯色 + 实线 + 默认线帽/线接”时映射到 TextObject 的 Stroke；
     * 虚线、圆帽/方帽、圆角/斜接线接等组合交给字形路径绘制，保证视觉一致。
     *
     * @param gs   图形状态
     * @param mode 文字渲染模式
     * @return 回退原因；可对象化时返回 null
     */
    private String strokeFallbackReason(PDGraphicsState gs, RenderingMode mode) {
        if (mode != RenderingMode.STROKE && mode != RenderingMode.FILL_STROKE) {
            return null;
        }
        PDLineDashPattern dash = gs.getLineDashPattern();
        if (dash != null) {
            float[] array = dash.getDashArray();
            if (array != null && array.length > 0 && !(array.length == 1 && array[0] <= 0f)) {
                return "文字描边使用虚线";
            }
        }
        int cap = gs.getLineCap();
        if (cap != BasicStroke.CAP_BUTT && cap != BasicStroke.CAP_SQUARE) {
            return "文字描边使用圆线帽";
        }
        int join = gs.getLineJoin();
        if (join != BasicStroke.JOIN_MITER && join != BasicStroke.JOIN_BEVEL) {
            return "文字描边使用圆角线接";
        }
        return null;
    }

    /**
     * 把 PDF 图形状态的裁剪区映射为 OFD {@link org.ofdrw.core.pageDescription.clips.Clips}
     * <p>
     * OFD 图元裁剪路径处于<b>页面坐标系</b>（与 Boundary/CTM 同一坐标系），因此这里把按页面画布变换求得的
     * 裁剪多边形写入 {@code Clips}；生成后同时更新 run 的归属键，裁剪变化即触发 flush，
     * 保证每个文字对象只携带“自身生效期间”的裁剪区。
     *
     * @param gs PDF 图形状态
     * @return 裁剪区；裁剪为空或覆盖整页时返回 null（不写 Clips）
     */
    private OfdClips buildClips(PDGraphicsState gs) {
        java.awt.geom.Area clipping = gs == null ? null : gs.getCurrentClippingPath();
        if (clipping == null || clipping.isEmpty()) {
            return new OfdClips(null, 0, true);
        }
        /*
         * 文字对象暂不写 Clips。
         * <p>
         * OFD 的 Clips 在文字对象中使用对象坐标系，而当前实现按 PDF 裁剪区的页面坐标输出，
         * 二者不一致时查看器会按错误范围裁剪，导致整段文字消失（实测 y.pdf 第 2 页 131 个文字对象
         * 墨迹比仅 0.01，移除 Clips 后恢复为 1.00）。在正确坐标系方案落地前，宁可少裁也不错裁：
         * 不写 Clips 只损失“超出区域被裁剪”的效果，查看器仍会按页面边界裁剪。
         */
        /* PDFBox 的裁剪区以 PDF 用户空间（单位 pt，y 向上）给出，页面框即 [0,0,pageWidth,pageHeight]；
         * 依次施加翻页矩阵与图像变换后即得到 OFD 页面 mm 坐标，与文字对象边界处于同一坐标系。 */
        AffineTransform device = deviceTransform();
        if (pageBox == null || pageBox.getWidth() <= 0d || pageBox.getHeight() <= 0d) {
            return new OfdClips(null, 0, true);
        }
        Area pageClip = new Area(clipping);
        pageClip.intersect(new Area(pageBox));
        if (pageClip.isEmpty()) {
            return new OfdClips(null, 0, true);
        }
        if (pageClip.getBounds2D().getWidth() >= pageBox.getWidth() - 1e-6d
                && pageClip.getBounds2D().getHeight() >= pageBox.getHeight() - 1e-6d) {
            return new OfdClips(null, 0, true);
        }
        // 暂不写 Clips（见方法开头说明）
        return new OfdClips(null, 0, true);
    }

    /**
     * 把一个裁剪区转换为 OFD Area 的外接矩形加上按该矩形起画的图形命令
     * <p>
     * OFD 查看器在绘制裁剪路径时按 {@code Boundary} 尺寸建立对象空间，路径命令是相对 Boundary 左上角的坐标；
     * 这里把路径平移到 Boundary 原点（以左上角对齐），保证任何查看器都能按规范得到同一裁剪形状。
     *
     * @param clips 裁剪集合
     * @param area  页面坐标系下的裁剪区域，单位 mm
     */
    private void addClip(Clips clips, java.awt.geom.Area area) {
        java.awt.geom.Rectangle2D box = area.getBounds2D();
        if (box.getWidth() <= 0d || box.getHeight() <= 0d) {
            return;
        }
        StringBuilder data = new StringBuilder();
        PathIterator it = area.getPathIterator(null);
        double[] c = new double[6];
        double firstX = 0d;
        double firstY = 0d;
        while (!it.isDone()) {
            int seg = it.currentSegment(c);
            switch (seg) {
                case PathIterator.SEG_MOVETO:
                    firstX = c[0] - box.getMinX();
                    firstY = c[1] - box.getMinY();
                    data.append("M ").append(fmt(firstX)).append(' ').append(fmt(firstY)).append(' ');
                    break;
                case PathIterator.SEG_LINETO:
                    if (Math.abs(c[0] - box.getMinX() - firstX) <= 1e-6 && Math.abs(c[1] - box.getMinY() - firstY) <= 1e-6) {
                        break;
                    }
                    data.append("L ").append(fmt(c[0] - box.getMinX())).append(' ').append(fmt(c[1] - box.getMinY())).append(' ');
                    break;
                case PathIterator.SEG_QUADTO:
                    data.append('Q').append(' ').append(fmt(c[0] - box.getMinX())).append(' ').append(fmt(c[1] - box.getMinY()))
                            .append(' ').append(fmt(c[2] - box.getMinX())).append(' ').append(fmt(c[3] - box.getMinY())).append(' ');
                    break;
                case PathIterator.SEG_CUBICTO:
                    data.append('B').append(' ').append(fmt(c[0] - box.getMinX())).append(' ').append(fmt(c[1] - box.getMinY()))
                            .append(' ').append(fmt(c[2] - box.getMinX())).append(' ').append(fmt(c[3] - box.getMinY()))
                            .append(' ').append(fmt(c[4] - box.getMinX())).append(' ').append(fmt(c[5] - box.getMinY())).append(' ');
                    break;
                default:
                    break;
            }
            it.next();
        }
        if (data.length() == 0) {
            return;
        }
        data.append("C ");
        CT_Path clipPath = new CT_Path()
                .setAbbreviatedData(new AbbreviatedData(AbbreviatedData.parse(data.toString().trim())))
                .setFill(true)
                .setStroke(false);
        clipPath.setBoundary(new ST_Box(
                Math.round(box.getMinX() * 1000d) / 1000d,
                Math.round(box.getMinY() * 1000d) / 1000d,
                Math.round(box.getWidth() * 1000d) / 1000d,
                Math.round(box.getHeight() * 1000d) / 1000d));
        clipPath.setObjID(pageGraphics.newObjectId());
        org.ofdrw.core.pageDescription.clips.Area ofdArea = new org.ofdrw.core.pageDescription.clips.Area()
                .setClipObj(clipPath);
        clips.addClip(new CT_Clip().addArea(ofdArea));
    }


    /**
     * 页面内的新对象 ID
     *
     * @return 对象 ID
     */
    private ST_ID newObjectId() {
        return pageGraphics.newObjectId();
    }

    /**
     * 数值格式化为 3 位小数
     *
     * @param v 数值
     * @return 文本
     */
    private static String fmt(double v) {
        String t = String.format(java.util.Locale.ROOT, "%.3f", v);
        return t.contains(".") ? t.replaceAll("0+$", "").replaceAll("\\.$", "") : t;
    }

    /**
     * 裁剪区及其归属键
     */
    private static final class OfdClips {
        /**
         * OFD 裁剪区
         */
        final Clips clips;
        /**
         * 归属键（裁剪区内容哈希），用于判断 run 是否需要因裁剪变化而 flush
         */
        final int key;

        /**
         * 裁剪区是否覆盖整页（覆盖整页时不写 Clips，但 run 仍以此为边界）
         */
        final boolean fullPage;

        OfdClips(Clips clips, int key) {
            this(clips, key, false);
        }

        OfdClips(Clips clips, int key, boolean fullPage) {
            this.clips = clips;
            this.key = key;
            this.fullPage = fullPage;
        }
    }

    /**
     * 构造单个字形的几何信息
     *
     * @param trm      文字渲染矩阵
     * @param font     字体
     * @param code     字符编码
     * @param pdfGid   PDF 原始 GID
     * @param resolved 字形解析结果
     * @return 字形信息；矩阵退化时返回 null
     */
    private TextRunBuilder.Glyph buildGlyph(Matrix trm, PDFont font, int code, int pdfGid,
                                            Resolved resolved) {
        // N = Pg × TRM��Pg 只取“图像变换”（pt → 页面 mm），PDFBox 的翻页矩阵（设备 y 向下）另行叠加，
        // 这样 SY 恒为负，与 PDF 用户空间 y 向上一致，字形轮廓与字体度量的换算同一口径
        AffineTransform trmAwt = new AffineTransform(trm.getScaleX(), trm.getShearY(), trm.getShearX(), trm.getScaleY(),
                trm.getTranslateX(), trm.getTranslateY());
        // N = 图像变换 × 当前 CTM × TRM：Form 内部绘字时 CTM 携带 Form 的平移，不能漏乘
        AffineTransform n = currentGlyphTransform();
        n.concatenate(trmAwt);
        /*
         * 基线原点：N 的平移分量已经是"页面毫米"的坐标（pageTransform 是 pt → mm 的纯缩放）。
         * pageTransform 的 y 列取 -pageScale，因此 N.getTranslateY() 得到的就是 y 向下口径的页面坐标，
         * 与 OFD 文字对象 Boundary/TextCode.X/Y 的约定一致，直接使用即可；
         * 不能再叠一次 pageFlip：pageFlip 是画布初始变换（pt → 设备像素），重复施加会把坐标放大约 2.83 倍。
         */
        double originX = n.getTranslateX();
        double originY = n.getTranslateY();
        double em = Math.hypot(n.getShearX(), n.getScaleY());
        if (em <= 1e-9 || Double.isNaN(em)) {
            return null;
        }
        /*
         * N 是“PDF 文字空间 → 页面 mm（y 向上口径）”的完整变换，普通横排文字的 y 列为负。
         * linear 去掉 em 长度后即对象坐标系到页面的线性映射：普通横排文字得到 (1,0,0,-1)，
         * 而 OFD 没有 CTM 时默认按 y 向下渲染，二者正好一致；旋转、镜像与非等比缩放由 CTM 表达。
         */
        double sx = n.getScaleX() / em;
        double sy = n.getShearY() / em;
        double shx = n.getShearX() / em;
        double shy = n.getScaleY() / em;
        AffineTransform linear = new AffineTransform(sx, sy, shx, shy, 0d, 0d);

        String unicode = resolveUnicode(font, code);
        // 字体轮廓与度量都在 y 向上的 em 空间中给出，取 -N 让 em 的 y 列朝上，换算出的页面偏移可直接叠加到基线上
        AffineTransform glyphMatrix = new AffineTransform(-n.getScaleX(), -n.getShearY(),
                -n.getShearX(), -n.getScaleY(), n.getTranslateX(), n.getTranslateY());
        Rectangle2D box = glyphBox(font, pdfGid, glyphMatrix, em);
        /*
         * 字体垂直度量（设计单位）按 em 换算到页面 mm。
         * PDFBox 读取的 TrueType 水平度量可能被“码表归一化”（例如把 ascender 折成 220/256 em），
         * 而 OFD 文字对象按字体自身设计单位换算；这里以 head.unitsPerEm 为准，必要时按 1em 兜底。
         */
        float upem = PdfFontHelper.unitsPerEm(font);
        float[] metrics = PdfFontHelper.verticalMetrics(font);
        double k = em / (upem > 0 ? upem : 1000d);
        double ascent = Math.abs(metrics[0]) * k;
        double descent = Math.abs(metrics[1]) * k;
        return new TextRunBuilder.Glyph(unicode, resolved.gid, originX, originY, em, linear, box, ascent, descent);
    }

    /**
     * 取字形在页面坐标系下的轮廓包围盒
     *
     * @param font 字体
     * @param gid  字形索引
     * @param n    文字空间（em）→ 页面 mm 的完整矩阵
     * @param em   1 em 的页面长度，单位 mm
     * @return 包围盒；无轮廓时返回 null
     */
    private Rectangle2D glyphBox(PDFont font, int gid, AffineTransform n, double em) {
        try {
            float[] bb = PdfFontHelper.gidBBox(font, gid);
            if (bb == null) {
                return null;
            }
            double k = em / PdfFontHelper.unitsPerEm(font);
            double x1 = bb[0] * k, y1 = bb[1] * k, x2 = bb[2] * k, y2 = bb[3] * k;
            Point2D p1 = n.transform(new Point2D.Double(x1, y1), null);
            Point2D p2 = n.transform(new Point2D.Double(x2, y2), null);
            Point2D p3 = n.transform(new Point2D.Double(x1, y2), null);
            Point2D p4 = n.transform(new Point2D.Double(x2, y1), null);
            double minX = Math.min(Math.min(p1.getX(), p2.getX()), Math.min(p3.getX(), p4.getX()));
            double minY = Math.min(Math.min(p1.getY(), p2.getY()), Math.min(p3.getY(), p4.getY()));
            double maxX = Math.max(Math.max(p1.getX(), p2.getX()), Math.max(p3.getX(), p4.getX()));
            double maxY = Math.max(Math.max(p1.getY(), p2.getY()), Math.max(p3.getY(), p4.getY()));
            return new Rectangle2D.Double(minX, minY, maxX - minX, maxY - minY);
        } catch (RuntimeException e) {
            logger.debug("计算字形包围盒失败：{} gid={}", PdfFontHelper.fontName(font), gid, e);
            return null;
        }
    }

    /**
     * 取字形的 Unicode 文本：ToUnicode → 字形名反查 → 占位符
     *
     * @param font 字体
     * @param code 字符编码
     * @return 至少 1 个字符的 Unicode 文本
     */
    private String resolveUnicode(PDFont font, int code) {
        String uni = PdfFontHelper.toUnicode(font, code);
        if (uni != null && !uni.isEmpty()) {
            return uni;
        }
        uni = PdfFontHelper.unicodeFromGlyphName(font, code);
        if (uni != null && !uni.isEmpty()) {
            return uni;
        }
        warnings.record("缺少 ToUnicode", PdfFontHelper.fontName(font), pageNumber);
        return TextRunBuilder.PLACEHOLDER;
    }

    /**
     * 记录本次 run 使用的字体对应的 OFD 字体资源 ID
     *
     * @param font   字体
     * @param fontId 字体资源 ID
     */
    private void recordFont(PDFont font, ST_ID fontId) {
        fontIds.put(font, fontId);
    }

    /**
     * 输出当前文字 run（若存在），追加到页面内容容器尾部
     */
    void flushRun() {
        if (run == null) {
            return;
        }
        TextRunBuilder current = run;
        PDFont font = runFont;
        run = null;
        runFont = null;
        runClipKey = 0;
        runClipFullPage = false;
        runClipElement = null;
        if (current.size() == 0) {
            return;
        }
        ST_ID fontId = fontIds.get(font);
        try {
            TextObject text = current.build(pageGraphics.newObjectId(), isCidFont(font), gidTrackerFor(font));
            if (fontId != null) {
                text.setFont(fontId.ref());
            }
            pageGraphics.appendPageBlock(text);
        } catch (RuntimeException e) {
            logger.warn("构造文字对象失败，第 {} 页文字已按路径回退：{}，原因={}", pageNumber,
                    PdfFontHelper.fontName(font), e.toString());
        }
    }

    /**
     * 回退到字形路径绘制
     *
     * @param reason  回退原因
     * @param font    字体
     * @param trm     文字渲染矩阵
     * @param code    字符编码
     * @param disp    字形位移
     * @throws IOException 绘制异常
     */
    private void fallback(String reason, PDFont font, Matrix trm, int code, Vector disp) throws IOException {
        warnings.record(reason, PdfFontHelper.fontName(font), pageNumber);
        flushRun();
        super.showFontGlyph(trm, font, code, disp);
    }

    /**
     * 判断是否为 CID 字体（PDType0Font），CID 字体总是写 CGTransform
     *
     * @param font 字体
     * @return true - CID 字体
     */
    private static boolean isCidFont(PDFont font) {
        return font instanceof PDType0Font;
    }

    // ======================= 图形操作：先 flush，保证 z-order =======================

    @Override
    public void fillPath(int windingRule) throws IOException {
        if (tilingGraphics != null) {
            // 底纹单元直接画到离屏画布，不参与页面上的文字对象化与 flush
            super.fillPath(windingRule);
            return;
        }
        flushRun();
        if (skipInvisiblePaint(false)) {
            return;
        }
        if (fillTilingPattern(windingRule)) {
            return;
        }
        super.fillPath(windingRule);
    }

    /**
     * 判断当前填充/描边的整体透明度是否为 0，从而跳过绘制
     * <p>
     * PDF 常把“完全透明”的图案（ExtGState 中 {@code ca 0} 或 {@code CA 0}）用作排版遮盖层。
     * {@link OFDPageGraphics2D} 把图形写成 OFD 路径图元时不携带透明度，直接绘制会把遮盖层
     * 变成实心色块盖住正文（例如 testPathFillOpacity 的红色覆盖层）。透明度为 0 的图元
     * 在视觉上不可见，直接跳过即与 PDF 渲染结果一致。
     *
     * @param stroke true - 检查描边透明度（CA）；false - 检查填充透明度（ca）
     * @return true - 透明度为 0，应跳过绘制
     */
    private boolean skipInvisiblePaint(boolean stroke) {
        PDGraphicsState gs = getGraphicsState();
        if (gs == null) {
            return false;
        }
        /*
         * fillPath/strokePath 由 PDFBox 直接调用，能准确区分填充与描边；
         * 但字形回退路径（super.showFontGlyph）与渐变/遮罩填充最终落到 Java2D 的 fill(Shape)/draw(Shape)，
         * 这两者不经过本方法。为保证“完全透明的遮盖层”不被写成可见图元，只要填充与描边同时不可见就整体跳过；
         * 只一侧透明时仍按各自通道判断。
         */
        double fillAlpha = gs.getNonStrokeAlphaConstant();
        double strokeAlpha = gs.getAlphaConstant();
        if (fillAlpha <= 1e-6 && strokeAlpha <= 1e-6) {
            return true;
        }
        return (stroke ? strokeAlpha : fillAlpha) <= 1e-6;
    }

    /**
     * 用平铺底纹（PatternType 1）填充当前路径
     * <p>
     * PDFBox 在 tiling pattern 上走 {@code shadingFill} 之外的另一条路径：先把 pattern 渲染成
     * {@link TexturePaint} 再填充路径，而 {@link OFDPageGraphics2D} 的绘制参数只识别纯色与渐变，
     * 纹理会被忽略，导致“水印/底纹图案整页丢失”。这里直接把 pattern 单元预先渲染成位图，
     * 按 [pattern 矩阵 × 页内变换] 平铺填充，保证图案可见且位置与 PDF 一致。
     *
     * @param windingRule 填充规则（{@code Path2D.WIND_*}）
     * @return true - 已按底纹绘制；false - 当前不是底纹填充，调用方继续走父类
     * @throws IOException PDF 解析异常
     */
    private boolean fillTilingPattern(int windingRule) throws IOException {
        PDGraphicsState gs = getGraphicsState();
        if (gs == null) {
            return false;
        }
        PDTilingPattern tiling = currentTilingPattern(gs);
        if (tiling == null) {
            return false;
        }
        java.awt.geom.Area clip = gs.getCurrentClippingPath();
        java.awt.geom.Area path = new java.awt.geom.Area(getLinePath());
        if (path.isEmpty()) {
            return true;
        }
        path.intersect(clip);
        if (path.isEmpty()) {
            return true;
        }
        // 可见区域：路径包围盒与页面框求交，限制纹理位图尺寸
        Rectangle2D visible = path.getBounds2D();
        if (pageBox != null) {
            visible = visible.createIntersection(pageBox);
        }
        if (visible.getWidth() <= 0d || visible.getHeight() <= 0d) {
            return true;
        }
        return paintTiling(tiling, path.getBounds2D());
    }

    /**
     * 把平铺底纹写成 OFD 标准的水印注释（Annot Type="Watermark"）
     * <p>
     * OFD 规范为重复底纹（水印）提供了专门表达：{@code Annot/Appearance/PathObject/FillColor/CT_Pattern}，
     * 由 {@code Width/Height/XStep/YStep/CTM} 描述“单元内容 + 平铺周期 + 旋转缩放”，
     * 查看器按周期自动平铺。相比把 pattern 逐格渲染成位图，这种写法：
     * 1) 与期望 OFD 结构一致，产物是矢量、体积小；
     * 2) 单元内容保持为 TextObject，文字仍可搜索；
     * 3) 留白（XStep/Height 之间的空隙）由查看器按规范处理，不会因位图铺满而变密。
     *
     * @param tiling 平铺底纹
     * @param area   需要覆盖的区域（PDF 用户空间，单位 pt）
     * @return true - 已处理（无论是否成功），调用方不再走父类
     */
    private boolean paintTiling(PDTilingPattern tiling, Rectangle2D area) {
        try {
            org.ofdrw.core.pageDescription.color.pattern.CT_Pattern pattern =
                    buildOfdPattern(tiling);
            if (pattern == null) {
                return true;
            }
            writeWatermarkAnnot(pattern, area);
        } catch (RuntimeException | IOException e) {
            logger.warn("水印注释写出失败，已跳过：{}", e.toString());
        }
        return true;
    }

    /**
     * 由 PDF 平铺底纹构造 OFD 的 {@code CT_Pattern} 单元描述
     * <p>
     * 字段对应关系：{@code Width/Height} 取 pattern 的 BBox（单元内容范围），
     * {@code XStep/YStep} 取 PDF 的平铺步进（大于 BBox 的部分即单元之间的留白），
     * {@code CTM} 取 pattern 矩阵（pattern 空间 → PDF 用户空间）去掉 mm/pt 缩放后的线性部分。
     * 单元内容用位图渲染结果作为缩略图（{@code Thumbnail}），保证在不支持 CellContent 文字解析的
     * 查看器上也能显示。
     *
     * @param tiling PDF 平铺底纹
     * @return OFD 单元描述；无法构造时返回 null
     */
    private org.ofdrw.core.pageDescription.color.pattern.CT_Pattern buildOfdPattern(PDTilingPattern tiling) {
        if (tiling == null || pageGraphics == null) {
            return null;
        }
        org.apache.pdfbox.pdmodel.common.PDRectangle bbox = tiling.getBBox();
        if (bbox == null || bbox.getWidth() <= 0f || bbox.getHeight() <= 0f) {
            return null;
        }
        org.ofdrw.core.pageDescription.color.pattern.CT_Pattern pattern =
                new org.ofdrw.core.pageDescription.color.pattern.CT_Pattern();
        pattern.setWidth((double) bbox.getWidth());
        pattern.setHeight((double) bbox.getHeight());
        // 步进缺失或小于单元尺寸时退化为单元尺寸，避免查看器算出负留白
        double xStep = tiling.getXStep() > 0d ? tiling.getXStep() : bbox.getWidth();
        double yStep = tiling.getYStep() > 0d ? tiling.getYStep() : bbox.getHeight();
        pattern.setXStep(Math.max(xStep, bbox.getWidth()));
        pattern.setYStep(Math.max(yStep, bbox.getHeight()));
        /*
         * CTM 只保留“旋转/镜像”的方向信息，缩放归一化为 1：
         * PDF 的 pattern 矩阵形如 [2.0044 2.0044 -2.0044 2.0044 ...]，其线性缩放 2.8346 已经把
         * pattern 单位换算成点；而 OFD 的 CT_Pattern 中 Width/Height/XStep/YStep 与 CellContent 的
         * TextObject 都使用同一套“单元坐标”，如果再让 CTM 带上 2.8346 的缩放，字形会被放大 2.83 倍
         * （期望 OFD 的 CTM 即为归一化后的 0.7071 -0.7071 0.7071 0.7071）。
         */
        org.apache.pdfbox.util.Matrix pm = tiling.getMatrix();
        if (pm != null) {
            double norm = Math.hypot(pm.getScaleX(), pm.getShearY());
            if (norm <= 0d || Double.isNaN(norm)) {
                norm = 1d;
            }
            pattern.setCTM(new org.ofdrw.core.basicType.ST_Array(
                    nzValue(pm.getScaleX() / norm), nzValue(pm.getShearY() / norm),
                    nzValue(pm.getShearX() / norm), nzValue(pm.getScaleY() / norm),
                    nzValue(pm.getTranslateX()), nzValue(pm.getTranslateY())));
        }
        org.ofdrw.core.pageDescription.color.pattern.CellContent cellContent =
                new org.ofdrw.core.pageDescription.color.pattern.CellContent();
        /*
         * 单元内容优先写成 TextObject：OFD 规范中 CellContent 的图元与页面图元同构，
         * 文字保持可搜索，且查看器按 Pattern 的 Width/Height/XStep/YStep 平铺时不会引入位图边缘。
         * 解析失败时退回缩略图，保证至少能看到底纹。
         */
        TextObject cellText = buildCellTextObject(tiling);
        if (cellText != null) {
            cellContent.addPageBlock(cellText);
        } else {
            ST_ID thumbnail = buildCellThumbnail(tiling);
            if (thumbnail != null) {
                cellContent.setThumbnail(thumbnail.ref());
            }
        }
        pattern.setCellContent(cellContent);
        return pattern;
    }

    /**
     * 把平铺底纹单元里的文字解析成 CellContent 的 TextObject
     * <p>
     * PDF 的水印底纹单元通常就是一行文字（例如 {@code BT /F1 26.252 Tf ... TJ ET}）。
     * 这里复用页面的字体注册表与字符解码逻辑，按单元坐标系（pattern 空间，单位与 BBox 相同）
     * 生成一个 TextObject：Boundary 取 BBox，文本与逐字位移取自内容流。
     *
     * @param tiling PDF 平铺底纹
     * @return 单元文字对象；单元不含可解析文字时返回 null
     */
    private TextObject buildCellTextObject(PDTilingPattern tiling) {
        if (tiling == null || tiling.getResources() == null) {
            return null;
        }
        try {
            List<byte[]> operands = new ArrayList<>();
            String text = null;
            PDFont usedFont = null;
            double fontSize = 0d;
            double[] firstPosition = null;
            List<double[]> advances = new ArrayList<>();
            CellTextCollector collector = new CellTextCollector(tiling.getResources());
            collector.collect(toByteArray(tiling.getContents()));
            if (collector.text.length() == 0) {
                return null;
            }
            // 单元文字：Boundary 取 BBox，字串从 (0,0) 起按 UnitX 累加
            TextObject textObject = new TextObject(pageGraphics.newObjectId());
            double bw = tiling.getBBox().getWidth();
            double bh = tiling.getBBox().getHeight();
            textObject.setBoundary(new ST_Box(0d, 0d, bw, bh));
            textObject.setSize(collector.fontSize);
            org.ofdrw.core.text.TextCode code = new org.ofdrw.core.text.TextCode();
            code.setContent(collector.text.toString());
            code.setCoordinate(0d, collector.baselineY);
            if (collector.advances.size() > 1) {
                // ST_Array 的可变参数需逐个传入 Number，整体传 double[] 会被当单一元素序列化
                Double[] dx = new Double[collector.advances.size() - 1];
                for (int i = 0; i < dx.length; i++) {
                    dx[i] = collector.advances.get(i);
                }
                code.setDeltaX(new org.ofdrw.core.basicType.ST_Array(dx));
            }
            textObject.addTextCode(code);
            /*
             * 单元字体的登记：先用单元资源里的字体对象登记；
             * 若该字体在 PDF 中没有内嵌程序（水印字体常见），登记会失败，
             * 此时改用页面资源树中同名字体的对象再试一次——页面上的同名字体通常已经在正文渲染时
             * 成功登记过，可直接复用同一字体资源。
             */
            java.util.Set<Integer> cellUnicodes = new java.util.LinkedHashSet<>();
            for (int[] pair : collector.glyphs) {
                if (pair[2] >= 0) {
                    cellUnicodes.add(pair[2]);
                }
            }
            ST_ID fontId = annotationFontRegistrar != null
                    ? annotationFontRegistrar.apply(collector.font, cellUnicodes)
                    : fontRegistrar.apply(collector.font);
            if (fontId == null) {
                PDFont pageSibling = findPageFontByName(collector.font.getName());
                if (pageSibling != null && pageSibling != collector.font) {
                    fontId = annotationFontRegistrar != null
                            ? annotationFontRegistrar.apply(pageSibling, cellUnicodes)
                            : fontRegistrar.apply(pageSibling);
                }
            }
            if (fontId != null) {
                // 注释外观内的文字同样按文档级 PublicRes 的字体 ID 引用
                textObject.setFont(fontId.ref());
            }
            // 注释单元的字形使用也要登记：系统匹配到的完整 CJK 字体只有登记了实际字形才可子集化，
            // 否则会把十几 MB 的整字体写进 OFD（实测单个水印导致 492KB → 10.1MB）。
            if (glyphRegistrar != null) {
                for (int[] pair : collector.glyphs) {
                    glyphRegistrar.register(collector.font, pair[0], pair[1], pair[2]);
                }
            }
            textObject.setFill(true);
            textObject.setStroke(false);
            /*
             * 水印单元的图形状态通常带非不透明度（本样本 ExtGState 为 ca 0.2），
             * 对应 OFD 文字对象的 Alpha（0~255）。不写 Alpha 会被渲染成纯黑，
             * 与期望的浅灰水印差异极大。
             */
            int alpha = Math.round((float) (collector.alpha * 255d));
            if (alpha < 255 && alpha >= 0) {
                textObject.setAlpha(alpha);
            }
            return textObject;
        } catch (IOException | RuntimeException e) {
            logger.debug("底纹单元文字解析失败，退回缩略图：{}", e.toString());
        }
        return null;
    }

    /**
     * 在页面资源（含嵌套 Form 资源）中按名字查找字体
     *
     * @param fontName 字体名（可带子集前缀）
     * @return 页面中同名 PDF 字体；找不到时返回 null
     */
    private PDFont findPageFontByName(String fontName) {
        if (fontName == null || parameters == null || parameters.getPage() == null) {
            return null;
        }
        try {
            PDFont found = findFontInResources(parameters.getPage().getResources(), fontName);
            if (found != null) {
                return found;
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("在页面资源中查找字体失败：{}", e.toString());
        }
        return null;
    }

    /**
     * 递归在资源树中按名字查找字体
     *
     * @param resources 起始资源
     * @param fontName  字体名
     * @return 命中字体；找不到时返回 null
     */
    private PDFont findFontInResources(org.apache.pdfbox.pdmodel.PDResources resources, String fontName)
            throws IOException {
        if (resources == null || fontName == null) {
            return null;
        }
        for (org.apache.pdfbox.cos.COSName name : resources.getFontNames()) {
            PDFont font = resources.getFont(name);
            if (font != null && fontName.equals(font.getName())) {
                return font;
            }
        }
        for (org.apache.pdfbox.cos.COSName xobjectName : resources.getXObjectNames()) {
            org.apache.pdfbox.pdmodel.graphics.PDXObject xobject = resources.getXObject(xobjectName);
            if (xobject instanceof org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject) {
                PDFont found = findFontInResources(
                        ((org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject) xobject).getResources(), fontName);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    /**
     * 平铺底纹单元内容流中的文字收集器
     * <p>
     * 只处理水印单元常见的 {@code Tf / Td / TJ / Tj} 组合：记录解码后的文本、字号、
     * 首字基线偏移与逐字前进量（单元坐标系，单位与 pattern BBox 相同）。
     */
    private final class CellTextCollector {
        private final org.apache.pdfbox.pdmodel.PDResources resources;
        private final StringBuilder text = new StringBuilder();
        private final List<Double> advances = new ArrayList<>();
        private PDFont font;
        private double fontSize = 0d;
        private double baselineY = 0d;
        /** 单元内用到的字形三元组：{PDF 编码, GID, Unicode} */
        private final List<int[]> glyphs = new ArrayList<>();
        /** 单元内容流的填充不透明度（PDF ExtGState 的 ca，1 表示完全不透明） */
        private double alpha = 1d;

        CellTextCollector(org.apache.pdfbox.pdmodel.PDResources resources) {
            this.resources = resources;
        }

        /**
         * 解析单元内容流中的文字指令
         * <p>
         * 只识别水印单元常见的 {@code /Fx size Tf}、{@code 0 ty Td}、{@code [<hex> ...] TJ}
         * 与 {@code <hex> Tj} 组合；命中即可得到文本、字号、基线偏移与逐字前进量。
         *
         * @param content 单元内容流字节
         * @throws IOException 字体读取异常
         */
        void collect(byte[] content) throws IOException {
            String cs = new String(content, java.nio.charset.StandardCharsets.ISO_8859_1);
            java.util.regex.Matcher tf = java.util.regex.Pattern
                    .compile("/([A-Za-z0-9]+)\\s+([0-9.]+)\\s+Tf").matcher(cs);
            if (tf.find()) {
                font = resources.getFont(org.apache.pdfbox.cos.COSName.getPDFName(tf.group(1)));
                fontSize = Double.parseDouble(tf.group(2));
            }
            java.util.regex.Matcher td = java.util.regex.Pattern
                    .compile("([-0-9.]+)\\s+([-0-9.]+)\\s+Td").matcher(cs);
            if (td.find()) {
                baselineY = Math.abs(Double.parseDouble(td.group(2)));
            }
            java.util.regex.Matcher tj = java.util.regex.Pattern
                    .compile("\\[([^\\]]*)\\]\\s*TJ").matcher(cs);
            if (tj.find()) {
                java.util.regex.Matcher hex = java.util.regex.Pattern.compile("<([0-9A-Fa-f]+)>").matcher(tj.group(1));
                while (hex.find()) {
                    appendCode(hex.group(1));
                }
                java.util.regex.Matcher kerning = java.util.regex.Pattern.compile("(?<!<)(-?[0-9.]+)(?!>)")
                        .matcher(tj.group(1));
                while (kerning.find() && advances.size() > 1) {
                    int idx = advances.size() - 1;
                    advances.set(idx, advances.get(idx) - Double.parseDouble(kerning.group(1)) / 1000d * fontSize);
                }
            } else {
                java.util.regex.Matcher tjs = java.util.regex.Pattern.compile("<([0-9A-Fa-f]+)>\\s*Tj").matcher(cs);
                if (tjs.find()) {
                    appendCode(tjs.group(1));
                }
            }
            /*
             * 透明度必须在取文字之后单独解析：早先的实现把 alpha 解析放在 TJ 分支之后，
             * 而 TJ 分支带有 return，导致带 TJ 的单元（水印的常见形态）永远拿不到 ca，
             * 水印被按不透明绘制成纯黑。
             */
            // 取单元内容流里第一个生效的 ExtGState 填充不透明度（ca）
            java.util.regex.Matcher gsName = java.util.regex.Pattern.compile("/([A-Za-z0-9]+)\\s+gs").matcher(cs);
            while (gsName.find() && alpha >= 1d) {
                Double ca = lookupAlpha(resources, gsName.group(1));
                if (ca != null) {
                    alpha = ca;
                }
            }
        }

        /**
         * 在给定资源（及其上层页面资源）中查找 ExtGState 的填充不透明度
         *
         * @param res     起始资源，可为 null
         * @param gsKey   ExtGState 名称
         * @return ca 值（0~1）；找不到时返回 null
         */
        private Double lookupAlpha(org.apache.pdfbox.pdmodel.PDResources res, String gsKey) {
            org.apache.pdfbox.cos.COSName key = org.apache.pdfbox.cos.COSName.getPDFName(gsKey);
            /*
             * 直接按 COS 字典读取 ExtGState：pattern 的 Resources 常常只写
             * “/ExtGState << /ExtGS1 43 0 R >>” 这样的间接引用，PDFBox 的高层 API 在
             * 资源不完整（缺少 /Type /ExtGState 等）时会返回 null，而 COS 层能拿到真实字典。
             */
            for (org.apache.pdfbox.pdmodel.PDResources probe : new org.apache.pdfbox.pdmodel.PDResources[]{
                    res, parameters == null || parameters.getPage() == null
                            ? null : parameters.getPage().getResources()}) {
                if (probe == null) {
                    continue;
                }
                try {
                    org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState egs = probe.getExtGState(key);
                    Float ca = egs == null ? null : egs.getNonStrokingAlphaConstant();
                    if (ca != null) {
                        return ca.doubleValue();
                    }
                    org.apache.pdfbox.cos.COSDictionary raw = probe.getCOSObject();
                    if (raw != null) {
                        org.apache.pdfbox.cos.COSDictionary gsDict = raw.getCOSDictionary(
                                org.apache.pdfbox.cos.COSName.EXT_G_STATE);
                        if (gsDict != null) {
                            org.apache.pdfbox.cos.COSBase base = gsDict.getDictionaryObject(key);
                            if (base instanceof org.apache.pdfbox.cos.COSObject) {
                                base = ((org.apache.pdfbox.cos.COSObject) base).getObject();
                            }
                            if (base instanceof org.apache.pdfbox.cos.COSDictionary) {
                                org.apache.pdfbox.cos.COSNumber num = (org.apache.pdfbox.cos.COSNumber)
                                        ((org.apache.pdfbox.cos.COSDictionary) base)
                                                .getDictionaryObject(org.apache.pdfbox.cos.COSName.getPDFName("ca"));
                                if (num != null) {
                                    return (double) num.floatValue();
                                }
                            }
                        }
                    }
                } catch (RuntimeException e) {
                    logger.debug("读取底纹单元透明度失败：{}", e.toString());
                }
            }
            return null;
        }

        /**
         * 解码一个十六进制字符串并按字形累加前进量
         *
         * @param hex 十六进制字符串（每个字形 4 位）
         * @throws IOException 字体读取异常
         */
        private void appendCode(String hex) throws IOException {
            if (font == null || hex.length() % 4 != 0) {
                return;
            }
            for (int i = 0; i < hex.length(); i += 4) {
                int code = Integer.parseInt(hex.substring(i, i + 4), 16);
                String uni = PdfFontHelper.toUnicode(font, code);
                int gid = -1;
                try {
                    gid = PdfFontHelper.codeToGid(font, code);
                } catch (RuntimeException e) {
                    logger.debug("底纹单元字形索引解析失败：{}", e.toString());
                }
                glyphs.add(new int[]{code, gid, uni == null || uni.isEmpty() ? -1 : uni.codePointAt(0)});
                text.append(uni == null || uni.isEmpty() ? TextRunBuilder.PLACEHOLDER : uni);
                try {
                    advances.add((double) font.getStringWidth(uni == null ? "" : uni) / 1000d * fontSize);
                } catch (IOException | RuntimeException e) {
                    advances.add(fontSize);
                }
            }
        }
    }

    /**
     * 把平铺底纹的单元内容渲染成缩略图并登记为文档图片资源
     *
     * @param tiling PDF 平铺底纹
     * @return 图片资源 ID；渲染失败时返回 null
     */
    private ST_ID buildCellThumbnail(PDTilingPattern tiling) {
        try {
            BufferedImage tile = renderPatternTile(tiling, TILING_TILE_SCALE);
            return tile == null ? null : pageGraphics.addImageResource(tile);
        } catch (IOException | RuntimeException e) {
            logger.debug("底纹单元缩略图生成失败：{}", e.toString());
            return null;
        }
    }

    /**
     * 把单元描述写成页面级水印注释
     * <p>
     * 结构：{@code Doc_N/Annots/Page_M/Annotation.xml}（{@code PageAnnot}）+ 文档级
     * {@code Doc_N/Annots/Annotations.xml} 索引，并在 {@code Document.xml} 登记 {@code Annotations} 位置。
     * 注释外观为覆盖整页的矩形路径，其填充色使用 {@code CT_Pattern} 平铺。
     *
     * @param pattern OFD 单元描述
     * @param area    需要覆盖的区域（PDF 用户空间，单位 pt）
     * @throws IOException 写包异常
     */
    private void writeWatermarkAnnot(org.ofdrw.core.pageDescription.color.pattern.CT_Pattern pattern,
                                     Rectangle2D area) throws IOException {
        if (watermarkWritten) {
            // 同页多次填充同一底纹时只写一条注释，避免重复叠加加深
            return;
        }
        org.ofdrw.pkg.container.OFDDir ofdDir = pageGraphics.getOfdDir();
        org.ofdrw.core.basicStructure.doc.Document document = pageGraphics.getDocument();
        if (ofdDir == null || document == null) {
            return;
        }
        /*
         * 注释外观的 Boundary 取“以页面中心为中心、边长≈页面对角线”的正方形：
         * 水印外观内部带有旋转（CT_Pattern 的 CTM），用页面矩形会露出四角；
         * 用对角线方形可保证任意旋转角下都覆盖整页（与期望 OFD 的 -74.25 -30.76 358.52 358.52 一致）。
         */
        double w = pageBox != null ? pageBox.getWidth() * pageTransform.getScaleX() : 210d;
        double h = pageBox != null ? -pageBox.getHeight() * pageTransform.getScaleY() : 297d;
        double side = Math.hypot(w, h);
        ST_Box pageBoundary = new ST_Box((w - side) / 2d, (h - side) / 2d, side, side);

        PathObject path = new PathObject(pageGraphics.newObjectId());
        path.setBoundary(pageBoundary);
        path.setFill(true);
        path.setStroke(false);
        // 水印外观必须显式声明 Normal 混合模式，避免继承页面的非 Normal 混合状态
        path.addAttribute("BlendMode", "Normal");
        FillColor fill = new FillColor();
        fill.setColor(pattern);
        path.setFillColor(fill);
        // 路径数据使用对象坐标系：相对 Boundary 左上角，因此为 0..side 的正方形
        AbbreviatedData data = new AbbreviatedData();
        data.moveTo(0d, 0d);
        data.lineTo(side, 0d);
        data.lineTo(side, side);
        data.lineTo(0d, side);
        data.C();
        path.setAbbreviatedData(data);

        Appearance appearance = new Appearance(pageBoundary);
        appearance.addPageBlock(path);

        Annot annot = new Annot();
        annot.setID(pageGraphics.newObjectId());
        annot.setType(AnnotType.Watermark);
        annot.setSubtype("Watermark");
        annot.setAppearance(appearance);
        annot.setPrint(true);
        // 水印参数：步进用页面毫米表示；旋转与缩放已由 CT_Pattern 的 CTM 表达
        /*
         * Step 使用“页面毫米”而不是 pattern 单位：查看器按 Step 以页面坐标平铺注释外观，
         * 若直接写 pattern 单位（如 127），会被当成 127mm 步进，远大于实际单元尺寸（约 45mm）。
         * 换算：pattern 单位 × |Matrix 线性缩放| × pageScale(mm/pt)。
         */
        AffineTransform patternMatrix = new AffineTransform();
        if (pattern.getCTM() != null) {
            // ST_Array 以空格分隔的文本形式暴露，直接按数值解析出 6 个矩阵分量
            String[] raw = pattern.getCTM().toString().trim().split("\\s+");
            if (raw.length >= 6) {
                patternMatrix = new AffineTransform(
                        Double.parseDouble(raw[0]), Double.parseDouble(raw[1]),
                        Double.parseDouble(raw[2]), Double.parseDouble(raw[3]),
                        Double.parseDouble(raw[4]), Double.parseDouble(raw[5]));
            }
        }
        // CTM 已归一化为纯旋转，单元坐标与页面毫米同尺度，换算系数取 1
        double matrixScale = 1d;
        double toMm = Math.abs(pageTransform.getScaleY());
        /*
         * Step 表示注释外观（整块单元区域）在页面上的重复间距，单位页面毫米。
         * 取单元的可见尺寸 Width/Height（即 BBox）经 pattern 矩阵换算到毫米：
         * Pattern 自身的 XStep/YStep 已由查看器在单元内部处理，此处再用 XStep 会导致间距过大、
         * 每格只放得下 1~2 个字形（实测水印字间距达 127mm，而单元仅 107 单位）。
         */
        double widthMm = pattern.getWidth() * matrixScale * toMm;
        double heightMm = pattern.getHeight() * matrixScale * toMm;
        /*
         * 期望 OFD 的形态：PathObject.Boundary 覆盖一个远大于单页的方形区域（358.52 见方），
         * 单元内容按 CT_Pattern 的 CTM 缩放到页外，再由 Annot 的 Step 决定注释在页面上重复的间距。
         * 这里按同样思路：Boundary 取“页面对角线长度”的方形，保证旋转后仍能覆盖整页。
         */
        annot.addParameter("Step", formatNumber(widthMm) + " " + formatNumber(heightMm));
        annot.addParameter("Rotate", "0");
        annot.addParameter("RotateMode", "Angle");
        annot.addParameter("Scale", "1");
        annot.addParameter("HorizontalAlign", "AlignCenter");
        annot.addParameter("VerticalAlign", "AlignCenter");
        annot.addParameter("sw_printable", "true");

        PageAnnot pageAnnot = new PageAnnot();
        pageAnnot.addAnnot(annot);
        // 注释索引：Doc_N/Annots/Annotations.xml，页面条目 Doc_N/Annots/Page_M/Annotation.xml
        org.ofdrw.pkg.container.DocDir docDir = ofdDir.obtainDocDefault();
        org.ofdrw.pkg.container.AnnotsDir annotsDir = docDir.obtainAnnots();
        org.ofdrw.pkg.container.PageDir annotPageDir = annotsDir.obtainByIndex(annotPageIndex);
        annotPageDir.setPageAnnot(pageAnnot);
        Annotations annotations = new Annotations();
        AnnPage annPage = new AnnPage();
        annPage.setPageID(pageGraphics.pageID);
        annPage.setFileLoc(new ST_Loc("Page_" + annotPageIndex + "/"
                + org.ofdrw.pkg.container.PageDir.AnnotationFileName));
        annotations.addPage(annPage);
        annotsDir.setAnnotations(annotations);
        document.setAnnotations(new ST_Loc(org.ofdrw.pkg.container.DocDir.AnnotsDir + "/"
                + org.ofdrw.pkg.container.DocDir.AnnotationsFileName));
        /*
         * 注释索引直接落盘：Doc_N 容器在构建过程中会被 flush 清空缓存，
         * 仅依赖 putObj 会丢失 Annots/Annotations.xml（实测未进入包内，导致查看器忽略整份注释）。
         */
        java.nio.file.Path annotsDirPath = java.nio.file.Paths.get(
                docDir.getSysAbsPath(), org.ofdrw.pkg.container.DocDir.AnnotsDir);
        java.nio.file.Files.createDirectories(annotsDirPath);
        org.dom4j.io.OutputFormat annotsFormat = org.dom4j.io.OutputFormat.createPrettyPrint();
        annotsFormat.setEncoding("UTF-8");
        org.dom4j.io.XMLWriter annotsWriter = new org.dom4j.io.XMLWriter(
                java.nio.file.Files.newOutputStream(
                        annotsDirPath.resolve(org.ofdrw.pkg.container.DocDir.AnnotationsFileName)),
                annotsFormat);
        annotsWriter.write(annotations);
        annotsWriter.close();
        watermarkWritten = true;
    }

    /**
     * 数值归一化：避免 -0.0 进入 XML
     *
     * @param v 数值
     * @return 归一化后的数值
     */
    private static double nzValue(double v) {
        return Math.abs(v) < 1e-12 ? 0d : v;
    }

    /**
     * 数值格式化为紧凑字符串（去掉多余小数位）
     *
     * @param v 数值
     * @return 格式化结果
     */
    private static String formatNumber(Double v) {
        if (v == null) {
            return "0";
        }
        return new java.math.BigDecimal(v).setScale(4, java.math.RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    /**
     * 判断当前非描边着色源是否为平铺底纹
     *
     * @param gs PDF 图形状态
     * @return 平铺底纹；不是底纹时返回 null
     */
    private PDTilingPattern currentTilingPattern(PDGraphicsState gs) {
        PDColor color = gs.getNonStrokingColor();
        if (color == null || color.getPatternName() == null || parameters == null) {
            return null;
        }
        try {
            PDAbstractPattern pattern = parameters.getPage().getResources().getPattern(color.getPatternName());
            if (pattern == null) {
                // pattern 可能定义在 Form XObject 的资源里，回退到页面资源树中逐层查找
                pattern = findPattern(getResources(), color.getPatternName());
            }
            return pattern instanceof PDTilingPattern ? (PDTilingPattern) pattern : null;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /**
     * 在资源树中按名称查找 Pattern（页面内容常引用 Form XObject 资源里的底纹）
     *
     * @param resources 起始资源
     * @param name      Pattern 名称
     * @return Pattern；找不到时返回 null
     */
    private PDAbstractPattern findPattern(PDResources resources, COSName name) {
        if (resources == null || name == null) {
            return null;
        }
        try {
            PDAbstractPattern pattern = resources.getPattern(name);
            if (pattern != null) {
                return pattern;
            }
            for (COSName xobjectName : resources.getXObjectNames()) {
                PDXObject xobject = resources.getXObject(xobjectName);
                if (xobject instanceof PDFormXObject) {
                    PDAbstractPattern found = findPattern(((PDFormXObject) xobject).getResources(), name);
                    if (found != null) {
                        return found;
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            return null;
        }
        return null;
    }

    /**
     * 把平铺底纹渲染为覆盖指定区域（PDF 用户空间）的纹理绘制对象
     * <p>
     * 做法：把 pattern 单元的总变换 {@code [单元矩阵 × pattern 矩阵]} 展开为“单元空间 → 页面 mm”的
     * 完整仿射变换，先渲染一个单元位图，再按该仿射变换的线性部分求纹理锚点矩形，使相邻单元正好衔接。
     *
     * @param tiling 平铺底纹
     * @param area   需要覆盖的区域（PDF 用户空间，单位 pt）
     * @return 纹理绘制对象；单元渲染失败时返回 null
     */
    private TexturePaint tilingPaint(PDTilingPattern tiling, Rectangle2D area) {
        if (pageGraphics == null || pageTransform == null || parameters == null) {
            return null;
        }
        try {
            org.apache.pdfbox.util.Matrix pm = tiling.getMatrix();
            // pattern 的矩阵已把单元空间（1pt）映射到 PDF 用户空间；再叠加页内变换（pt → 页面 mm）
            AffineTransform unit = new AffineTransform(
                    pm.getScaleX(), pm.getShearY(), pm.getShearX(), pm.getScaleY(), 0d, 0d);
            AffineTransform toPage = new AffineTransform(pageTransform);
            unit.preConcatenate(toPage);
            double scale = Math.hypot(unit.getScaleX(), unit.getShearY());
            double xStep = tiling.getXStep();
            double yStep = tiling.getYStep();
            if (scale <= 0d || !(xStep > 0d) || !(yStep > 0d)) {
                return null;
            }
            int width = (int) Math.round(xStep * scale);
            int height = (int) Math.round(yStep * scale);
            if (width <= 0 || height <= 0 || width > 4000 || height > 4000) {
                return null;
            }
            BufferedImage tile = tilingTiles.get(tiling.hashCode() + "|" + width + "x" + height);
            if (tile == null) {
                /*
                 * 把 pattern 单元绘制到目标画布上按“完整变换”定位的一块小区域：
                 * OFDPageGraphics2D 直接把图形写成页面绝对坐标，没有像素缓冲区，
                 * 因此不能用 TexturePaint 平铺（需要光栅化时它拿不到像素），
                 * 这里把单元内容流渲染到一块与单元等大的离屏画布，再按页面坐标摆放。
                 */
                tile = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
                Graphics2D tg = tile.createGraphics();
                tg.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                tg.setRenderingHint(java.awt.RenderingHints.KEY_TEXT_ANTIALIASING,
                        java.awt.RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                tg.scale(scale, scale);
                new org.apache.pdfbox.rendering.PageDrawer(parameters).drawPage(tg, tiling.getBBox());
                tg.dispose();
                tilingTiles.put(tiling.hashCode() + "|" + width + "x" + height, tile);
            }
            // 锚点矩形：单元 (0,0)-(xStep,yStep) 在页面 mm 下的外接矩形
            double wide = Math.abs(unit.getScaleX()) * xStep + Math.abs(unit.getShearX()) * yStep;
            double high = Math.abs(unit.getShearY()) * xStep + Math.abs(unit.getScaleY()) * yStep;
            return new TexturePaint(tile, new Rectangle2D.Double(0d, 0d, wide, high));
        } catch (IOException | RuntimeException e) {
            logger.debug("平铺底纹渲染失败，回退为无填充：{}", e.toString());
            return null;
        }
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        if (tilingGraphics != null) {
            super.fillAndStrokePath(windingRule);
            return;
        }
        flushRun();
        if (skipInvisiblePaint(false) && skipInvisiblePaint(true)) {
            return;
        }
        if (fillTilingPattern(windingRule)) {
            return;
        }
        super.fillAndStrokePath(windingRule);
    }

    @Override
    public void strokePath() throws IOException {
        flushRun();
        if (skipInvisiblePaint(true)) {
            return;
        }
        super.strokePath();
    }

    @Override
    public void drawImage(PDImage pdImage) throws IOException {
        flushRun();
        super.drawImage(pdImage);
    }

    @Override
    public void shadingFill(COSName shadingName) throws IOException {
        flushRun();
        super.shadingFill(shadingName);
    }

    @Override
    public void showAnnotation(org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation annotation) throws IOException {
        flushRun();
        super.showAnnotation(annotation);
    }

    /**
     * 透明组：离屏画布上的文字一律回退
     *
     * @param group 透明组
     * @throws IOException PDF 解析异常
     */
    @Override
    protected void showTransparencyGroupOnGraphics(org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup group,
                                                   Graphics2D graphics) throws IOException {
        flushRun();
        super.showTransparencyGroupOnGraphics(group, graphics);
    }

    @Override
    protected void showType3Glyph(Matrix textRenderingMatrix, PDType3Font font, int code, Vector displacement) throws IOException {
        // Type3 字形本身是内容流，内部文字按路径绘制
        super.showType3Glyph(textRenderingMatrix, font, code, displacement);
    }

    @Override
    public void showTransparencyGroup(org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup form) throws IOException {
        flushRun();
        super.showTransparencyGroup(form);
    }

}
