package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.graphics2d.OFDPageGraphics2D;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.function.Function;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * PDF 页面 → OFD 页面的文字对象化转换器（内部组件）
 * <p>
 * 组装 {@link TextObjectPdfRenderer}、{@link PdfTextFonts}、{@link PdfTextWarningCollector}，
 * 对外只暴露“渲染一页”与“结束（输出告警、回填字体文件）”两个动作。
 * 字体嵌入策略由 {@code fontRegistrar} / {@code fontFlusher} 注入：默认原样嵌入 PDF 内嵌字体。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OfdTextObjectConverter {

    private static final Logger logger = LoggerFactory.getLogger(OfdTextObjectConverter.class);

    /**
     * PDF 文档
     */
    private final PDDocument document;

    /**
     * OFD 文档
     */
    private final OFDGraphicsDocument ofdDoc;

    /**
     * 字体映射
     */
    private final PdfTextFonts fonts;

    /**
     * 字体结束动作：在 OFD 打包前调用，负责子集化/写字体文件/回填字体资源
     */
    private final BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher;

    /**
     * 降级告警聚合器
     */
    private final PdfTextWarningCollector warnings = new PdfTextWarningCollector();

    /**
     * CGTransform GID 延迟登记函数（传给页面绘制器），可为 null
     */
    private final OfdPageDrawer.GidTracker trackFinalGid;

    /**
     * 注释单元字体登记函数（允许系统字体兜底），可为 null
     */
    private final java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> annotationFontRegistrar;

    /**
     * 已登记字体 → 字体资源 ID
     */
    private final Map<PDFont, ST_ID> registeredFonts = new LinkedHashMap<>();

    /**
     * 渲染器
     */
    private TextObjectPdfRenderer renderer;

    /**
     * 是否已完成字体回填
     */
    private boolean flushed = false;

    /**
     * 创建文字对象化转换器（默认：GID 不变、不登记字形使用）
     *
     * @param document      PDF 文档
     * @param ofdDoc        OFD 文档
     * @param fontRegistrar 字体登记函数：返回字体资源 ID，无法嵌入时返回 null
     * @param fontFlusher   字体结束动作：在 OFD 打包前调用，可为 null
     */
    public OfdTextObjectConverter(PDDocument document, OFDGraphicsDocument ofdDoc,
                                  Function<PDFont, ST_ID> fontRegistrar,
                                  BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher) {
        this(document, ofdDoc, fontRegistrar, fontFlusher, null, null);
    }

    /**
     * 创建文字对象化转换器
     *
     * @param document      PDF 文档
     * @param ofdDoc        OFD 文档
     * @param fontRegistrar 字体登记函数：返回字体资源 ID，无法嵌入时返回 null
     * @param fontFlusher   字体结束动作：在 OFD 打包前调用，可为 null
     * @param gidResolver   “PDF 原始 GID → 最终 GID”解析器（字体子集化后 CGTransform 必须用它），
     *                      可为 null（GID 不变）
     * @param glyphRegistrar 字形使用登记函数（记录 PDF 原始 GID 与 Unicode，供子集化重建 cmap），可为 null
     */
    public OfdTextObjectConverter(PDDocument document, OFDGraphicsDocument ofdDoc,
                                  Function<PDFont, ST_ID> fontRegistrar,
                                  BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher,
                                  GidResolver gidResolver,
                                  OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar) {
        this(document, ofdDoc, fontRegistrar, fontFlusher, gidResolver, glyphRegistrar, false);
    }

    /**
     * 创建文字对象化转换器（带 CGTransform GID 延迟改写）
     *
     * @param document       PDF 文档
     * @param ofdDoc         OFD 文档
     * @param fontRegistrar  字体登记函数：返回字体资源 ID，无法嵌入时返回 null
     * @param fontFlusher    字体结束动作：在 OFD 打包前调用，可为 null
     * @param gidResolver    “PDF 原始 GID → 最终 GID”解析器（子集化时必须非 null）
     * @param glyphRegistrar 字形使用登记函数，可为 null
     * @param rewriteGidAfterSubset true 表示字体子集化：登记 CGTransform 节点，字体写出后改写为最终 GID
     */
    public OfdTextObjectConverter(PDDocument document, OFDGraphicsDocument ofdDoc,
                                  Function<PDFont, ST_ID> fontRegistrar,
                                  BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher,
                                  GidResolver gidResolver,
                                  OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar,
                                  boolean rewriteGidAfterSubset) {
        this(document, ofdDoc, fontRegistrar, fontFlusher, gidResolver, glyphRegistrar, rewriteGidAfterSubset, null);
    }

    /**
     * 创建文字对象化转换器（带注释单元字体登记）
     *
     * @param document                  PDF 文档
     * @param ofdDoc                    OFD 图形文档
     * @param fontRegistrar             正文字体登记函数
     * @param fontFlusher               字体写出函数
     * @param gidResolver               GID 解析器
     * @param glyphRegistrar            字形使用登记
     * @param rewriteGidAfterSubset     是否在子集化后改写 GID
     * @param annotationFontRegistrar   注释单元字体登记函数（允许系统字体兜底），可为 null
     */
    public OfdTextObjectConverter(PDDocument document, OFDGraphicsDocument ofdDoc,
                                  Function<PDFont, ST_ID> fontRegistrar,
                                  BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher,
                                  GidResolver gidResolver,
                                  OfdPageDrawer.GlyphUsageRegistrar glyphRegistrar,
                                  boolean rewriteGidAfterSubset,
                                  java.util.function.BiFunction<PDFont, java.util.Set<Integer>, ST_ID> annotationFontRegistrar) {
        this.annotationFontRegistrar = annotationFontRegistrar;
        this.document = document;
        this.ofdDoc = ofdDoc;
        this.fontFlusher = fontFlusher;
        this.fonts = new PdfTextFonts(font -> {
            ST_ID id = fontRegistrar == null ? null : fontRegistrar.apply(font);
            if (id != null) {
                registeredFonts.putIfAbsent(font, id);
            }
            return id;
        }, (font, unicodes) -> {
            ST_ID id = annotationFontRegistrar == null ? null : annotationFontRegistrar.apply(font, unicodes);
            if (id != null) {
                registeredFonts.putIfAbsent(font, id);
            }
            return id;
        }, glyphRegistrar == null ? null
                : (font, code, gid, unicode) -> glyphRegistrar.register(font, code, gid, unicode), gidResolver);
        this.trackFinalGid = rewriteGidAfterSubset
                ? (font, glyphsElement, pdfGids) -> fonts.trackGid(font, glyphsElement, pdfGids) : null;
    }

    /**
     * 渲染指定页到 OFD 页面画布
     *
     * @param pageIndex    页码（从 0 起）
     * @param pageGraphics OFD 页面画布
     * @param scale        缩放比例（PDF 用户空间 → 画布单位）
     * @throws IOException PDF 解析异常
     */
    public void renderPage(int pageIndex, OFDPageGraphics2D pageGraphics, float scale) throws IOException {
        if (renderer == null) {
            renderer = new TextObjectPdfRenderer(document, fonts, warnings, trackFinalGid);
        }
        renderer.renderPage(pageIndex, pageGraphics, scale);
    }

    /**
     * 设置渲染参数（图片插值、抗锯齿等），在首次渲染前调用
     *
     * @param hints 渲染参数
     */
    public void setRenderingHints(java.awt.RenderingHints hints) {
        if (renderer == null) {
            renderer = new TextObjectPdfRenderer(document, fonts, warnings, trackFinalGid);
        }
        renderer.setRenderingHints(hints);
    }

    /**
     * 结束转换：输出聚合告警，并在 OFD 打包前完成字体文件写入
     */
    public void finish() {
        if (flushed) {
            return;
        }
        flushed = true;
        warnings.flush();
        if (fontFlusher == null || registeredFonts.isEmpty()) {
            return;
        }
        try {
            fontFlusher.accept(ofdDoc, registeredFonts);
        } catch (RuntimeException e) {
            logger.warn("PDF 字体写入 OFD 失败，相关文字仍以文字对象保留（可能依赖查看器字体替换）", e);
        }
        /*
         * 字体子集化的 old→new 映射在字体写出后才确定，而文字对象在渲染阶段已写入页面内容，
         * 因此这里在 flush 之后统一把 CGTransform 的 GID 改写为最终值（未子集化时为恒等，不产生改动）。
         */
        if (trackFinalGid != null) {
            try {
                int changed = fonts.resolvePendingGids();
                if (changed > 0) {
                    logger.debug("CGTransform GID 已按子集字体改写：{} 个", changed);
                }
            } catch (RuntimeException e) {
                logger.warn("CGTransform GID 改写失败，子集化字体可能导致字形错位", e);
            }
        }
    }

    /**
     * 降级告警聚合器
     *
     * @return 告警聚合器
     */
    public PdfTextWarningCollector getWarnings() {
        return warnings;
    }
}
