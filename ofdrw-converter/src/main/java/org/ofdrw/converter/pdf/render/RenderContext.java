package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.PdfPageContent;

/**
 * 渲染上下文：一次页面转换过程中共享的状态
 * <p>
 * 上下文只承载“页面级”信息（页面尺寸、页面框偏移、当前容器 CTM、页面内容累积器），
 * 具体图元的绘制参数由 {@link GraphicsStateResolver} 计算，不放在此处，避免状态混杂。
 * <p>
 * 实现要求：所有 setter 返回 this 以支持链式调用；<b>不持有任何渲染库类型</b>。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface RenderContext {

    /**
     * 渲染管线告警日志前缀标记
     * <p>
     * 设计约束 4（可观测）：资源缺失、未实现特性统一通过 warning 日志输出；
     * 本标记用于在回归测试与日志筛查中定位管线产生的告警。
     */
    String DIAGNOSTIC_TAG = "OFD2PDF";

    /**
     * 获取页面宽度
     *
     * @return 页面宽度，单位 mm（OFD 原始单位）
     */
    double getPageWidthMm();

    /**
     * 获取页面高度
     *
     * @return 页面高度，单位 mm（OFD 原始单位）
     */
    double getPageHeightMm();

    /**
     * 获取页面框左上角横坐标偏移
     * <p>
     * 决策 26：{@code MediaBox = CropBox = PhysicalBox}；PhysicalBox 允许带非零左上角，
     * 图元与裁剪区域的页面坐标换算必须带上该偏移，否则内容整体错位。
     *
     * @return 页面框左上角横坐标，单位 mm；缺省 0
     */
    default double getPageX0Mm() {
        return 0;
    }

    /**
     * 获取页面框左上角纵坐标偏移
     *
     * @return 页面框左上角纵坐标，单位 mm；缺省 0
     */
    default double getPageY0Mm() {
        return 0;
    }

    /**
     * 获取当前容器（页面、模板页、图层、复合对象）的 CTM
     *
     * @return 当前 CTM，单位无量纲
     */
    AffineMatrix getCurrentCtm();

    /**
     * 叠加当前容器 CTM，返回新的上下文（不可变风格，避免并行渲染时互相污染）
     *
     * @param ctm 待叠加的 CTM
     * @return 新的上下文
     */
    RenderContext withCtm(AffineMatrix ctm);

    /**
     * 获取页面内容累积器
     *
     * @return 页面内容
     */
    PdfPageContent getPageContent();

    /**
     * 获取当前容器（图层）继承下来的绘制参数
     * <p>
     * OFD 8.2.1 的就近原则为：图元属性 &gt; 图元 DrawParam &gt; 图层 DrawParam &gt; Relative 链 &gt; 默认值。
     * 图层声明的 {@code DrawParam} 必须传给子图元作为继承基准，否则图元只能拿到默认值（典型现象：
     * 图层统一声明的描边色/线宽丢失，整页线全部变黑）。
     *
     * @return 图层有效绘制参数；未声明时返回 {@code null}
     */
    default EffectiveDrawParam getInheritedDrawParam() {
        return null;
    }

    /**
     * 派生出“继承指定图层绘制参数”的新上下文
     *
     * @param param 图层有效绘制参数，可为空
     * @return 新的渲染上下文；不支持继承的实现返回自身
     */
    default RenderContext withInheritedDrawParam(EffectiveDrawParam param) {
        return this;
    }

    /**
     * 是否处于注释外观（Appearance）渲染上下文
     * <p>
     * 注释外观的坐标系与页面正文不同：外观内容先按外观自身坐标系排版，再由注释定位到页面。
     * 底纹等“以坐标空间为锚点”的特性必须据此选择锚定方式，否则平铺相位与基准相差一整段偏移。
     *
     * @return true 表示当前正在渲染注释外观
     */
    default boolean isAppearanceContext() {
        return false;
    }

    /**
     * 获取字体选择器
     * <p>
     * 文字渲染需要把 OFD 字体映射为 PDF 字体引用，字体选择结果由 {@code org.ofdrw.converter.pdf.font} 包负责，
     * 因此通过上下文传递，避免渲染器各自缓存字体状态。
     *
     * @return 字体选择器，可为空（此时文字渲染器必须跳过并输出 warning）
     */
    default org.ofdrw.converter.pdf.font.FontSelector getFontSelector() {
        return null;
    }
}
