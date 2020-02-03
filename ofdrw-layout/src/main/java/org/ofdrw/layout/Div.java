package org.ofdrw.layout;

/**
 * 盒式模型基础
 *
 * @author 权观宇
 * @since 2020-02-03 12:46:15
 */
public class Div {
    /**
     * 内容宽度
     * <p>
     * 如果不设置，则为自适应。最大宽度不能大于页面版心宽度。
     */
    private Double width;

    /**
     * 内容高度
     * <p>
     * 如果不设置则为自适应。
     * <p>
     * 注意如果需要保证块完整，那么高度不能大于版心高度。
     */
    private Double height;

    /**
     * 内边距
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] padding;

    /**
     * 边框宽度
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] border;

    /**
     * 外边距
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] margin;

    /**
     * 固定布局的盒式模型左上角X坐标
     */
    private Double x;

    /**
     * 固定布局的盒式模型左上角y坐标
     */
    private Double y;

    /**
     * 对段的占用情况
     */
    private Clear clear;

    /**
     * 在段中的浮动方向
     * <p>
     * 为了避免与float关键字冲突采用AFloat
     */
    private AFloat aFloat;

    /**
     * 相对于段的左边界距离
     */
    private Double left;

    /**
     * 相对于段的右边界距离
     */
    private Double right;
    /**
     * 当渲染空间不足时可能会拆分元素
     * <p>
     * true为不拆分，false为拆分。默认值为false
     */
    private Boolean integrity;

    public Double getWidth() {
        return width;
    }

    public Div setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public Div setHeight(Double height) {
        this.height = height;
        return this;
    }

    public Double[] getPadding() {
        return padding;
    }

    public Div setPadding(Double... padding) {
        this.padding = ArrayParamTool.arr4p(padding);
        return this;
    }

    public Double[] getBorder() {
        return border;
    }

    public Div setBorder(Double[] border) {
        this.border = ArrayParamTool.arr4p(border);
        return this;
    }

    public Double[] getMargin() {
        return margin;
    }

    public Div setMargin(Double[] margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
    }

    public Double getX() {
        return x;
    }

    public Div setX(Double x) {
        this.x = x;
        return this;
    }

    public Double getY() {
        return y;
    }

    public Div setY(Double y) {
        this.y = y;
        return this;
    }

    public Clear getClear() {
        return clear;
    }

    public Div setClear(Clear clear) {
        this.clear = clear;
        return this;
    }

    public AFloat getFloat() {
        return aFloat;
    }

    public Div setFloat(AFloat aFloat) {
        this.aFloat = aFloat;
        return this;
    }

    public Double getLeft() {
        return left;
    }

    public Div setLeft(Double left) {
        this.left = left;
        return this;
    }

    public Double getRight() {
        return right;
    }

    public Div setRight(Double right) {
        this.right = right;
        return this;
    }

    public Boolean getIntegrity() {
        return integrity;
    }

    public Div setIntegrity(Boolean integrity) {
        this.integrity = integrity;
        return this;
    }
}
