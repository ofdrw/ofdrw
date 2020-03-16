package org.ofdrw.layout;

import org.ofdrw.layout.element.ArrayParamTool;

/**
 * 虚拟页面样式
 *
 * @author 权观宇
 * @since 2020-02-28 03:25:54
 */
public class PageLayout {
    // w: 138  h: 225
    public static final PageLayout A4 = new PageLayout(210d, 297d);

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
     *  上 左 下 右
     * [0  1  2  3]
     * 默认值 36
     */
    private Double[] margin = {36d, 36d, 36d, 36d};

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

    public PageLayout setMargin(Double[] margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
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
}
