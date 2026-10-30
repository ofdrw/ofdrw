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
     * 创建文字对象化转换器
     *
     * @param document    PDF 文档
     * @param ofdDoc      OFD 文档
     * @param fontRegistrar 字体登记函数：返回字体资源 ID，无法嵌入时返回 null
     * @param fontFlusher   字体结束动作：在 OFD 打包前调用，可为 null
     */
    public OfdTextObjectConverter(PDDocument document, OFDGraphicsDocument ofdDoc,
                                  Function<PDFont, ST_ID> fontRegistrar,
                                  BiConsumer<OFDGraphicsDocument, Map<PDFont, ST_ID>> fontFlusher) {
        this.document = document;
        this.ofdDoc = ofdDoc;
        this.fontFlusher = fontFlusher;
        this.fonts = new PdfTextFonts(font -> {
            ST_ID id = fontRegistrar == null ? null : fontRegistrar.apply(font);
            if (id != null) {
                registeredFonts.putIfAbsent(font, id);
            }
            return id;
        }, null, null);
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
            renderer = new TextObjectPdfRenderer(document, fonts, warnings);
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
            renderer = new TextObjectPdfRenderer(document, fonts, warnings);
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
