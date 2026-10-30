package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType3Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImage;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;
import org.apache.pdfbox.pdmodel.graphics.state.PDGraphicsState;
import org.apache.pdfbox.pdmodel.graphics.state.PDSoftMask;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.util.Matrix;
import org.apache.pdfbox.util.Vector;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.graphics2d.OFDPageGraphics2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.HashMap;
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
     * 字形使用登记函数：记录该字体实际使用的 PDF 原始 GID
     */
    private final GlyphRegistrar glyphRegistrar;

    /**
     * 降级告警聚合器
     */
    private final PdfTextWarningCollector warnings;

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
     * 当前画布的字形解析函数（未启用文字对象化时为 null，表示一律回退）
     */
    private GlyphResolver currentResolver = null;

    /**
     * 字形使用登记函数
     */
    @FunctionalInterface
    interface GlyphRegistrar {
        /**
         * 记录字形使用
         *
         * @param font PDF 字体
         * @param code PDF 字符编码
         * @param gid  PDF 原始字形索引
         */
        void register(PDFont font, int code, int gid);
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
     * 创建页面绘制器
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
                         GlyphRegistrar glyphRegistrar, PdfTextWarningCollector warnings, int pageNumber) throws IOException {
        super(parameters);
        this.pageGraphics = pageGraphics;
        this.fontRegistrar = fontRegistrar;
        this.glyphRegistrar = glyphRegistrar;
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
     * 页面画布当前变换（PDF 用户空间 → OFD 页面 mm），每页开始时刷新
     */
    private AffineTransform pageTransform = null;

    /**
     * 已登记的“安全处理器”名称（软掩码/混合模式相关算子）
     */
    private final Set<String> safeOperators = new HashSet<>();

    /**
     * 当前 run 使用的字体 → 字体资源 ID
     */
    private final Map<PDFont, ST_ID> fontIds = new HashMap<>();

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
        pageTransform = new AffineTransform(g.getTransform());
        started = true;
        currentResolver = (font, code, pdfGid) -> {
            ST_ID fontId = fontRegistrar.apply(font);
            if (fontId == null) {
                return null;
            }
            return new Resolved(fontId, pdfGid, isCidFont(font));
        };
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
        String reason = unsupportedReason(font, mode);
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
                glyphRegistrar.register(font, code, pdfGid);
            } catch (RuntimeException e) {
                logger.debug("字形使用登记失败：{}", PdfFontHelper.fontName(font), e);
            }
        }
        if (run == null || runFont != font) {
            flushRun();
            TextRunBuilder.TextRunStyle style = buildStyle(gs, mode, textRenderingMatrix);
            run = new TextRunBuilder(font, style);
            runFont = font;
        }
        run.add(glyph);
        recordFont(font, resolved.fontId);
        if (!run.accepts()) {
            flushRun();
        }
    }

    /**
     * 判断当前字形是否必须回退路径绘制
     *
     * @param font 字体
     * @param mode 文字渲染模式
     * @return 回退原因；可对象化时返回 null
     */
    private String unsupportedReason(PDFont font, RenderingMode mode) {
        if (font instanceof PDType3Font) {
            return "Type3 字体";
        }
        if (!PdfFontHelper.isTrueTypeLike(font)) {
            return "非 TrueType 轮廓字体";
        }
        if (font instanceof PDType0Font && ((PDType0Font) font).isVertical()) {
            // 竖排文字（WMode=1）：后续按 ReadDirection=90 精确映射，当前版本回退路径
            return "竖排文字";
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
        // N = Pg × TRM
        AffineTransform trmAwt = new AffineTransform(trm.getScaleX(), trm.getShearY(), trm.getShearX(), trm.getScaleY(),
                trm.getTranslateX(), trm.getTranslateY());
        AffineTransform n = new AffineTransform(pageTransform);
        n.concatenate(trmAwt);
        double originX = n.getTranslateX();
        double originY = n.getTranslateY();
        double em = Math.hypot(n.getShearX(), n.getScaleY());
        if (em <= 1e-9 || Double.isNaN(em)) {
            return null;
        }
        double sx = n.getScaleX() / em;
        double sy = n.getShearY() / em;
        double shx = n.getShearX() / em;
        double shy = n.getScaleY() / em;
        AffineTransform linear = new AffineTransform(sx, sy, shx, shy, 0d, 0d);

        String unicode = resolveUnicode(font, code);
        Rectangle2D box = glyphBox(font, pdfGid, n, em);
        // 字体垂直度量（设计单位）按 em 换算到页面 mm
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
        if (current.size() == 0) {
            return;
        }
        ST_ID fontId = fontIds.get(font);
        try {
            TextObject text = current.build(pageGraphics.newObjectId(), isCidFont(font));
            if (fontId != null) {
                text.setFont(fontId.ref());
            }
            pageGraphics.appendPageBlock(text);
        } catch (RuntimeException e) {
            // 文字对象构造失败：回退为路径绘制（不抛异常，避免整页转换失败）
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
        flushRun();
        super.fillPath(windingRule);
    }

    @Override
    public void fillAndStrokePath(int windingRule) throws IOException {
        flushRun();
        super.fillAndStrokePath(windingRule);
    }

    @Override
    public void strokePath() throws IOException {
        flushRun();
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
