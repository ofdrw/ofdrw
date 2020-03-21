package org.ofdrw.layout;

import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.layout.element.ArrayParamTool;

import java.util.Arrays;

/**
 * 虚拟页面样式
 *
 * @author 权观宇
 * @since 2020-02-28 03:25:54
 */
public class PageLayout {

    public static final PageLayout A0 = new PageLayout(841d, 1189d);
    public static final PageLayout A1 = new PageLayout(594d, 841d);
    public static final PageLayout A2 = new PageLayout(420d, 594d);
    public static final PageLayout A3 = new PageLayout(297d, 420d);
    public static final PageLayout A4 = new PageLayout(210d, 297d);
    public static final PageLayout A5 = new PageLayout(148d, 210d);
    public static final PageLayout A6 = new PageLayout(105d, 148d);
    public static final PageLayout A7 = new PageLayout(74d, 105d);
    public static final PageLayout A8 = new PageLayout(52d, 74d);
    public static final PageLayout A9 = new PageLayout(37d, 52d);
    public static final PageLayout A10 = new PageLayout(26d, 37d);

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
        return width - margin[1] - margin[3];
    }

    /**
     * @return 实际能放置内容的高度
     */
    public double contentHeight() {
        return height - margin[0] - margin[2];
    }

    /**
     * 页面正文的工作区域
     *
     * @return 工作区域
     */
    public Rectangle getWorkerArea() {
        return new Rectangle(
                margin[1],
                margin[0],
                width - margin[1] - margin[3],
                height - margin[0] - margin[2]);
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
                // 显示区域为减去margin的区域
                .setApplicationBox(this.getMarginLeft(),
                        this.getMarginTop(),
                        this.contentWidth(),
                        this.contentHeight());
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
}
