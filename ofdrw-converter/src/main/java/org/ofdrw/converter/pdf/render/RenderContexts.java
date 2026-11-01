package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.font.FontSelector;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.PdfPageContent;

/**
 * 渲染上下文工厂
 * <p>
 * 页面 CTM 保持恒等（决策 1：逐点换算），因此初始 CTM 固定为 {@link AffineMatrix#IDENTITY}；
 * 只有进入模板页、复合对象等容器时才通过 {@link RenderContext#withCtm(AffineMatrix)} 叠加容器 CTM。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class RenderContexts {

    private RenderContexts() {
    }

    /**
     * 创建一个以单位矩阵为初始 CTM 的页面级渲染上下文
     *
     * @param pageWidthMm  页面宽度，单位 mm
     * @param pageHeightMm 页面高度，单位 mm
     * @param pageContent  页面内容累积器，不能为空
     * @param fontSelector 字体选择器，可为空
     * @return 新的渲染上下文
     */
    public static RenderContext create(double pageWidthMm, double pageHeightMm,
                                       PdfPageContent pageContent, FontSelector fontSelector) {
        return new DefaultRenderContext(pageWidthMm, pageHeightMm, 0, 0, AffineMatrix.IDENTITY, pageContent, fontSelector);
    }

    /**
     * 创建注释外观渲染上下文
     * <p>
     * 与页面级上下文唯一的差别是 {@link RenderContext#isAppearanceContext()} 返回 true：
     * 外观内的底纹需要按外观坐标系锚定，见 {@code PatternRenderer#tilingMatrix}。
     *
     * @param pageX0Mm     页面框左上角横坐标，单位 mm
     * @param pageY0Mm     页面框左上角纵坐标，单位 mm
     * @param pageWidthMm  页面宽度，单位 mm
     * @param pageHeightMm 页面高度，单位 mm
     * @param pageContent  页面内容累积器，不能为空
     * @param fontSelector 字体选择器，可为空
     * @return 新的渲染上下文
     */
    public static RenderContext createAppearance(double pageX0Mm, double pageY0Mm, double pageWidthMm,
                                                 double pageHeightMm, PdfPageContent pageContent,
                                                 FontSelector fontSelector) {
        return new DefaultRenderContext(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm,
                AffineMatrix.IDENTITY, pageContent, fontSelector, true);
    }

    /**
     * 创建一个带有页面框偏移的页面级渲染上下文（对应决策 26：{@code MediaBox = CropBox = PhysicalBox}）
     *
     * @param pageX0Mm     页面框左上角横坐标，单位 mm
     * @param pageY0Mm     页面框左上角纵坐标，单位 mm
     * @param pageWidthMm  页面宽度，单位 mm
     * @param pageHeightMm 页面高度，单位 mm
     * @param pageContent  页面内容累积器，不能为空
     * @param fontSelector 字体选择器，可为空
     * @return 新的渲染上下文
     */
    public static RenderContext create(double pageX0Mm, double pageY0Mm, double pageWidthMm, double pageHeightMm,
                                       PdfPageContent pageContent, FontSelector fontSelector) {
        return new DefaultRenderContext(pageWidthMm, pageHeightMm, pageX0Mm, pageY0Mm,
                AffineMatrix.IDENTITY, pageContent, fontSelector);
    }
}
