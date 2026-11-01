package org.ofdrw.converter.pdf.textobj;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.rendering.PageDrawer;
import org.apache.pdfbox.rendering.PageDrawerParameters;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.io.IOException;

/**
 * 输出 OFD 文字对象的 PDF 渲染器
 * <p>
 * 与 {@link PDFRenderer} 的唯一区别是注入 {@link OfdPageDrawer}：文字走文字对象旁路，
 * 图形与图片仍由父类绘制到 {@link OFDPageGraphics2D}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class TextObjectPdfRenderer extends PDFRenderer {

    /**
     * 当前页面画布
     */
    private OFDPageGraphics2D pageGraphics;

    /**
     * 字体映射
     */
    private final PdfTextFonts fonts;

    /**
     * 降级告警聚合器
     */
    private final PdfTextWarningCollector warnings;

    /**
     * 当前页码（从 1 起）
     */
    private int pageNumber = 0;

    /**
     * @param document PDF 文档
     * @param fonts    字体映射
     * @param warnings 降级告警聚合器
     */
    TextObjectPdfRenderer(PDDocument document, PdfTextFonts fonts,
                          PdfTextWarningCollector warnings) {
        this(document, fonts, warnings, null);
    }

    /**
     * @param document  PDF 文档
     * @param fonts     字体映射
     * @param warnings  降级告警聚合器
     * @param trackFinalGid CGTransform GID 延迟登记函数，可为 null（GID 不变）
     */
    TextObjectPdfRenderer(PDDocument document, PdfTextFonts fonts,
                          PdfTextWarningCollector warnings, OfdPageDrawer.GidTracker trackFinalGid) {
        super(document);
        this.fonts = fonts;
        this.warnings = warnings == null ? new PdfTextWarningCollector() : warnings;
        this.trackFinalGid = trackFinalGid;
    }

    /**
     * CGTransform GID 延迟登记函数
     */
    private final OfdPageDrawer.GidTracker trackFinalGid;

    /**
     * 渲染指定页到 OFD 页面画布
     *
     * @param pageIndex    页码（从 0 起）
     * @param pageGraphics OFD 页面画布
     * @param scale        缩放比例（PDF 用户空间 → 画布单位）
     * @throws IOException PDF 解析异常
     */
    void renderPage(int pageIndex, OFDPageGraphics2D pageGraphics, float scale) throws IOException {
        this.pageGraphics = pageGraphics;
        this.pageNumber = pageIndex + 1;
        renderPageToGraphics(pageIndex, pageGraphics, scale);
    }

    @Override
    protected PageDrawer createPageDrawer(PageDrawerParameters parameters) throws IOException {
        OfdPageDrawer drawer = new OfdPageDrawer(parameters, pageGraphics,
                this::registerFont, this::registerGlyph, warnings, pageNumber, null, trackFinalGid);
        // 注释/底纹单元允许系统字体兜底（正文仍走严格策略）
        drawer.setAnnotationFontRegistrar(this::registerAnnotationFont);
        return drawer;
    }

    /**
     * 登记注释单元（平铺底纹/水印）的字体
     * <p>
     * 与 {@link #registerFont} 分离：单元字体常无内嵌程序，允许在配置了字体目录时用系统字体替代。
     *
     * @param font PDF 字体
     * @return OFD 字体资源 ID；无法登记时返回 null
     */
    private ST_ID registerAnnotationFont(PDFont font, java.util.Set<Integer> usedUnicodes) {
        return fonts == null ? null : fonts.registerAnnotationFont(font, usedUnicodes);
    }

    /**
     * 登记 PDF 字体
     *
     * @param font PDF 字体
     * @return OFD 字体资源 ID；无法嵌入时返回 null
     */
    private ST_ID registerFont(PDFont font) {
        return fonts == null ? null : fonts.register(font, null);
    }

    /**
     * 记录字形使用
     *
     * @param font    PDF 字体
     * @param code    PDF 字符编码
     * @param gid     PDF 原始字形索引
     * @param unicode 该字形对应的 Unicode 码位，未知时为 -1
     */
    private void registerGlyph(PDFont font, int code, int gid, int unicode) {
        if (fonts != null) {
            fonts.use(font, code, gid, unicode);
        }
    }
}
