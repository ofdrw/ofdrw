package org.ofdrw.converter.pdf.model;

/**
 * 一段子路径
 * <p>
 * 每个实现对应 PDF 中“在坐标之间移动的绘制操作符”：{@code m}/{@code l}/{@code c}/{@code v}/{@code y}。
 * 第一条子路径必须由 {@link MoveTo} 开始（PDF 要求路径以 {@code m} 起始）。
 * <p>
 * 坐标单位为 pt，坐标系由所属 {@link PathCommand} 的 {@link CoordSpace} 决定。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public interface PathSegment {

    /**
     * 获取终点横坐标
     *
     * @return 终点横坐标，单位 pt
     */
    double getEndX();

    /**
     * 获取终点纵坐标
     *
     * @return 终点纵坐标，单位 pt
     */
    double getEndY();
}
