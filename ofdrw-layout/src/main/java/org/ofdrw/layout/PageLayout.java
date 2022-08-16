package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.element.ArrayParamTool;

import java.util.Arrays;

/**
 * 虚拟页面样式
 *
 * @author 权观宇
 * @since 2020-02-28 03:25:54
 */
public class PageLayout {

    public static PageLayout A0() {
        return new PageLayout(841d, 1189d);
    }

    public static PageLayout A1() {
        return new PageLayout(594d, 841d);
    }

    public static PageLayout A2() {
        return new PageLayout(420d, 594d);
    }

    public static PageLayout A3() {
        return new PageLayout(297d, 420d);
    }

    public static PageLayout A4() {
        return new PageLayout(210d, 297d);
    }

    public static PageLayout A5() {
        return new PageLayout(148d, 210d);
    }

    public static PageLayout A6() {
        return new PageLayout(105d, 148d);
    }

    public static PageLayout A7() {
        return new PageLayout(74d, 105d);
    }

    public static PageLayout A8() {
        return new PageLayout(52d, 74d);
    }

    public static PageLayout A9() {
        return new PageLayout(37d, 52d);
    }

    public static PageLayout A10() {
        return new PageLayout(26d, 37d);
    }

    /**
     * 页面宽度
     */
    Double width;
    /**
     * 页面高度
     */
    Double height;

    /**
     * 外边距
     * <p>
     * 页边距：上下都是2.54厘米，左右都是3.17厘米。
     *
     * <p>
     * 上 左 下 右
     * [0  1  2  3]
     * 默认值 36
     */
    private Double[] margin = {25.4, 31.7, 25.4, 31.7};

    public PageLayout(Double width, Double height) {
        this.width = width;
        this.height = height;
    }

    public PageLayout(ST_Box box) {
        if (box == null) {
            throw new IllegalArgumentException("box 为空");
        }
        this.width = box.getWidth();
        this.height = box.getHeight();
    }

    public Double getWidth() {
        return width;
    }

    public PageLayout setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public PageLayout setHeight(Double height) {
        this.height = height;
        return this;
    }

    public Double[] getMargin() {
        return margin;
    }

    public PageLayout setMargin(Double... margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
    }

    public PageLayout setMarginTop(double top) {
        margin[0] = top;
        return this;
    }

    public double getMarginTop() {
        return margin[0];
    }

    public PageLayout setMarginRight(double right) {
        margin[1] = right;
        return this;
    }

    public double getMarginRight() {
        return margin[1];
    }

    public PageLayout setMarginBottom(double bottom) {
        margin[2] = bottom;
        return this;
    }

    public double getMarginBottom() {
        return margin[2];
    }


    public PageLayout setMarginLeft(double left) {
        margin[3] = left;
        return this;
    }

    public double getMarginLeft() {
        return margin[3];
    }

    /**
     * @return 实际能放置内容的宽度
     */
    public double contentWidth() {
        return width - getMarginLeft() - getMarginRight();
    }

    /**
     * @return 实际能放置内容的高度
     */
    public double contentHeight() {
        return height - getMarginTop() - getMarginBottom();
    }

    /**
     * 绘制区域原点X
     *
     * @return 起始X坐标
     */
    public double getStartX() {
        return getMarginLeft();
    }

    /**
     * 绘制区域原点Y
     *
     * @return 起始Y坐标
     */
    public double getStartY() {
        return getMarginTop();
    }

    /**
     * 页面正文的工作区域
     *
     * @return 工作区域
     */
    public Rectangle getWorkerArea() {
        return new Rectangle(
                getStartX(),
                getStartY(),
                contentWidth(),
                contentHeight());
    }

    /**
     * 获取OFD页面区域
     *
     * @return OFD页面区域
     */
    public CT_PageArea getPageArea() {
        return new CT_PageArea()
                // 物理区域为实际页面大小
                .setPhysicalBox(0, 0, this.getWidth(), this.getHeight())
                // 为了兼容骑缝章，不减去页面边距
                .setApplicationBox(0, 0,
                        this.getWidth(),
                        this.getHeight());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof PageLayout) {
            PageLayout that = (PageLayout) obj;
            return (Arrays.equals(this.margin, that.margin)
                    && this.width.equals(that.width)
                    && this.height.equals(that.height));
        }
        return false;
    }

    @Override
    public PageLayout clone() {
        PageLayout copy = new PageLayout(width, height);
        copy.margin = margin.clone();
        return copy;
    }
}
