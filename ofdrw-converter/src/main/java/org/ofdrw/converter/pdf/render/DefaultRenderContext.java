package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.font.FontSelector;
import org.ofdrw.converter.pdf.model.PdfPageContent;

/**
 * 渲染上下文默认实现：页面尺寸 + 页面框偏移 + 当前容器 CTM + 页面内容累积器
 * <p>
 * 采用不可变风格：{@link #withCtm(AffineMatrix)} 返回新对象，
 * 因此同一页面内容可以在不同容器 CTM 下安全复用，不会互相污染。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class DefaultRenderContext implements RenderContext {

    /**
     * 页面宽度，单位 mm
     */
    private final double pageWidthMm;

    /**
     * 页面高度，单位 mm
     */
    private final double pageHeightMm;

    /**
     * 页面框左上角横坐标，单位 mm
     */
    private final double pageX0Mm;

    /**
     * 页面框左上角纵坐标，单位 mm
     */
    private final double pageY0Mm;

    /**
     * 当前容器 CTM
     */
    private final AffineMatrix ctm;

    /**
     * 页面内容累积器
     */
    private final PdfPageContent pageContent;

    /**
     * 字体选择器，可为空
     */
    private final FontSelector fontSelector;

    /**
     * 是否处于注释外观渲染上下文
     */
    private final boolean appearance;

    /**
     * 图层继承下来的绘制参数，可为空
     */
    private final EffectiveDrawParam inheritedDrawParam;

    /**
     * 构造上下文
     *
     * @param pageWidthMm  页面宽度，单位 mm
     * @param pageHeightMm 页面高度，单位 mm
     * @param pageX0Mm     页面框左上角横坐标，单位 mm
     * @param pageY0Mm     页面框左上角纵坐标，单位 mm
     * @param ctm          当前容器 CTM，为空按单位矩阵
     * @param pageContent  页面内容，不能为空
     * @param fontSelector 字体选择器，可为空
     */
    DefaultRenderContext(double pageWidthMm, double pageHeightMm, double pageX0Mm, double pageY0Mm,
                         AffineMatrix ctm, PdfPageContent pageContent, FontSelector fontSelector) {
        this(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm, ctm, pageContent, fontSelector, false);
    }

    /**
     * 构造上下文（带注释外观标记）
     *
     * @param pageWidthMm  页面宽度，单位 mm
     * @param pageHeightMm 页面高度，单位 mm
     * @param pageX0Mm     页面框左上角横坐标，单位 mm
     * @param pageY0Mm     页面框左上角纵坐标，单位 mm
     * @param ctm          当前容器 CTM，为空按单位矩阵
     * @param pageContent  页面内容，不能为空
     * @param fontSelector 字体选择器，可为空
     * @param appearance   是否处于注释外观渲染上下文
     */
    DefaultRenderContext(double pageWidthMm, double pageHeightMm, double pageX0Mm, double pageY0Mm,
                         AffineMatrix ctm, PdfPageContent pageContent, FontSelector fontSelector,
                         boolean appearance) {
        this(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm, ctm, pageContent, fontSelector, appearance, null);
    }

    /**
     * 构造上下文（带注释外观标记与图层绘制参数）
     *
     * @param pageWidthMm        页面宽度，单位 mm
     * @param pageHeightMm       页面高度，单位 mm
     * @param pageX0Mm           页面框左上角横坐标，单位 mm
     * @param pageY0Mm           页面框左上角纵坐标，单位 mm
     * @param ctm                当前容器 CTM，为空按单位矩阵
     * @param pageContent        页面内容，不能为空
     * @param fontSelector       字体选择器，可为空
     * @param appearance         是否处于注释外观渲染上下文
     * @param inheritedDrawParam 图层继承的绘制参数，可为空
     */
    DefaultRenderContext(double pageWidthMm, double pageHeightMm, double pageX0Mm, double pageY0Mm,
                         AffineMatrix ctm, PdfPageContent pageContent, FontSelector fontSelector,
                         boolean appearance,
                         EffectiveDrawParam inheritedDrawParam) {
        if (pageContent == null) {
            throw new IllegalArgumentException("页面内容不能为空");
        }
        this.pageWidthMm = pageWidthMm;
        this.pageHeightMm = pageHeightMm;
        this.pageX0Mm = pageX0Mm;
        this.pageY0Mm = pageY0Mm;
        this.ctm = ctm == null ? AffineMatrix.IDENTITY : ctm;
        this.pageContent = pageContent;
        this.fontSelector = fontSelector;
        this.appearance = appearance;
        this.inheritedDrawParam = inheritedDrawParam;
    }

    @Override
    public double getPageWidthMm() {
        return pageWidthMm;
    }

    @Override
    public double getPageHeightMm() {
        return pageHeightMm;
    }

    @Override
    public double getPageX0Mm() {
        return pageX0Mm;
    }

    @Override
    public double getPageY0Mm() {
        return pageY0Mm;
    }

    @Override
    public EffectiveDrawParam getInheritedDrawParam() {
        return inheritedDrawParam;
    }

    @Override
    public RenderContext withInheritedDrawParam(EffectiveDrawParam param) {
        return new DefaultRenderContext(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm, ctm, pageContent,
                fontSelector, appearance, param);
    }

    @Override
    public boolean isAppearanceContext() {
        return appearance;
    }

    @Override
    public AffineMatrix getCurrentCtm() {
        return ctm;
    }

    @Override
    public RenderContext withCtm(AffineMatrix newCtm) {
        return new DefaultRenderContext(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm, newCtm, pageContent,
                fontSelector, appearance, inheritedDrawParam);
    }

    @Override
    public PdfPageContent getPageContent() {
        return pageContent;
    }

    @Override
    public FontSelector getFontSelector() {
        return fontSelector;
    }
}
