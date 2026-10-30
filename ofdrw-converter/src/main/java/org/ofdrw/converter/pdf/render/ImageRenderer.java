package org.ofdrw.converter.pdf.render;

/**
 * 图像渲染器：OFD ImageObject → 中间模型图像指令
 * <p>
 * 职责（设计 3.7 节、决策 23）：
 * <ul>
 *     <li>编码选择：JPEG 原样嵌入<b>不重编码</b>，其余（PNG 等）按无损 + Alpha/SoftMask 处理；</li>
 *     <li>{@code ImageMask} → 独立的蒙版图像指令（写入器映射为 PDF {@code /SMask}）；</li>
 *     <li>{@code Substitution} → 高分辨率替换图，按默认策略仅保留主图（决策 23：PDF 侧仅保留主图）；</li>
 *     <li>{@code Border}（线宽、圆角、虚线、颜色）单独绘制路径，不能依赖图像 XObject（决策见设计 3.7 节）。</li>
 * </ul>
 * 实现见 {@link ImageRendererImpl}（{@code @since 2.5.1}）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface ImageRenderer extends ContentRenderer {
}
