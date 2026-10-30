package org.ofdrw.converter.pdf.model;

/**
 * 指令作用域
 * <p>
 * 区分“路径坐标是否已经换算到 PDF 页面坐标”：
 * <ul>
 *     <li>{@link #PAGE}：指令中的坐标已按设计 3.1 节逐点换算为 PDF 页面坐标（单位 pt，Y 轴向上），
 *     所有几何变换已烘焙进坐标，无需在 PDF 中再写 CTM；</li>
 *     <li>{@link #OBJECT}：指令中的坐标处于图元对象空间（外接矩形左上角为原点，单位 mm，Y 轴向下，OFD 8.1.4），
 *     渲染器需要先应用 {@link org.ofdrw.converter.pdf.CoordSystem#objectToPage(Box, AffineMatrix)} 换算，
 *     该模式用于<b>尚未烘焙几何变换</b>的中间结果与调试对比。</li>
 * </ul>
 * 中间模型默认由渲染器输出 {@link #PAGE} 作用域的指令：页面 CTM 保持恒等，
 * 从而规避部分渲染器对页面整体 Y 翻转矩阵的缺陷（设计约束 1 与决策 1）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public enum CoordSpace {
    /**
     * 坐标已换算为 PDF 页面坐标
     */
    PAGE,
    /**
     * 坐标处于图元对象空间，尚未换算
     */
    OBJECT
}
