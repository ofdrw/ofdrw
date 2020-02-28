package org.ofdrw.layout;

import org.ofdrw.layout.element.ArrayParamTool;

/**
 * 虚拟页面样式
 *
 * @author 权观宇
 * @since 2020-02-28 03:25:54
 */
public class PageCfg {
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
     * 上 左 下 右
     * 默认值 36
     */
    private Double[] margin = {36d, 36d, 36d, 36d};

    public PageCfg(Double width, Double height) {
        this.width = width;
        this.height = height;
    }

    public Double getWidth() {
        return width;
    }

    public PageCfg setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public PageCfg setHeight(Double height) {
        this.height = height;
        return this;
    }

    public Double[] getMargin() {
        return margin;
    }

    public PageCfg setMargin(Double[] margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
    }
}
