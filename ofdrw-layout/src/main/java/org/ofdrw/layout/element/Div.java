package org.ofdrw.layout.element;


import org.ofdrw.layout.Measure;
import org.ofdrw.layout.Rectangle;

/**
 * 盒式模型基础
 *
 * @author 权观宇
 * @since 2020-02-03 12:46:15
 */
public class Div implements Measure {

    /**
     * 背景颜色
     * <p>
     * (R,G,B) 三色数组
     */
    private int[] backgroundColor = null;
    /**
     * 内容宽度
     * <p>
     * 如果不设置，则为自适应。最大宽度不能大于页面版心宽度。
     */
    private Double width = null;

    /**
     * 内容高度
     * <p>
     * 如果不设置则为自适应。
     * <p>
     * 注意如果需要保证块完整，那么高度不能大于版心高度。
     */
    private Double height = null;

    /**
     * 内边距
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] padding = {0d, 0d, 0d, 0d};

    /**
     * 边框宽度
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] border = {0d, 0d, 0d, 0d};

    /**
     * 外边距
     * <p>
     * 数组中个元素意义：上、右、下、左
     */
    private Double[] margin = {0d, 0d, 0d, 0d};

    /**
     * 固定布局的盒式模型左上角X坐标
     */
    private Double x = null;

    /**
     * 固定布局的盒式模型左上角y坐标
     */
    private Double y = null;

    /**
     * 对段的占用情况
     */
    private Clear clear = Clear.both;

    /**
     * 在段中的浮动方向
     * <p>
     * 为了避免与float关键字冲突采用AFloat
     */
    private AFloat aFloat = AFloat.left;

    /**
     * 相对于段的左边界距离
     */
    private Double left = null;

    /**
     * 相对于段的右边界距离
     */
    private Double right = null;

    /**
     * 相对坐标的top
     */
    private Double top = null;

    /**
     * 元素定位方式
     * <p>
     * 默认为静态定位
     */
    private Position position = Position.Static;

    /**
     * 当渲染空间不足时可能会拆分元素
     * <p>
     * true为不拆分，false为拆分。默认值为false
     */
    private Boolean integrity = false;

    /**
     * 占位符
     * <p>
     * 不参与渲染
     */
    private boolean placeholder = false;

    /**
     * 是否是块级元素
     * <p>
     * 块元素将会独占整个段
     * <p>
     * 绝对定位默认不为块级元素
     *
     * @return true 独占; false 共享
     */
    public boolean isBlockElement() {
        if (position == Position.Absolute) {
            return false;
        }
        /*
         独占段的元素类型
         1. 独占
         2. 浮动 + Clear 对立
         */
        return (clear == Clear.both)
                || (aFloat == AFloat.right && clear == Clear.left)
                || (aFloat == AFloat.left && clear == Clear.right);
    }

    public Double getTop() {
        return top;
    }

    public void setTop(Double top) {
        this.top = top;
    }

    public Position getPosition() {
        return position;
    }

    public Div setPosition(Position position) {
        this.position = position;
        return this;
    }

    public int[] getBackgroundColor() {
        return backgroundColor;
    }

    public Div setBackgroundColor(int r, int g, int b) {
        this.backgroundColor = new int[]{r, g, b};
        return this;
    }

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

    /**
     * @return 而外宽度
     */
    protected double widthPlus() {
        return (this.margin[1] + this.margin[3])
                + (this.padding[1] + this.padding[3])
                + (this.border[1] + this.border[3]);
    }

    /**
     * @return 而外高度
     */
    protected double heightPlus() {
        return (this.margin[0] + this.margin[2])
                + (this.padding[0] + this.padding[2])
                + (this.border[0] + this.border[2]);
    }

    /**
     * 获取尺寸
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    @Override
    public Rectangle reSize(Double widthLimit) {
        if (this.height == null || this.width == null) {
            return Rectangle.Empty;
        }
        if (widthLimit == null) {
            throw new NullPointerException("widthLimit为空");
        }
        widthLimit -= widthPlus();
        if (this.width > widthLimit) {
            // TODO 尺寸重置警告日志
            this.setWidth(widthLimit);
        }
        double w = this.width + widthPlus();
        double h = this.height + heightPlus();

        return new Rectangle(w, h);
    }


    /**
     * 判断是否为占位符
     *
     * @return true 占位符，不参与渲染， false - 非占位符
     */
    public boolean isPlaceholder() {
        return placeholder;
    }

    public Div setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
        return this;
    }

    /**
     * 空间占位符
     * <p>
     * 共享段空间，且不可分割
     *
     * @param width  宽度
     * @param height 高度
     * @param aFloat 浮动方向
     * @return 空间占位符
     */
    public static Div placeholder(double width, double height, AFloat aFloat) {
        return new Div()
                .setPlaceholder(true)
                .setWidth(width)
                .setHeight(height)
                .setFloat(aFloat)
                .setClear(Clear.none)
                .setIntegrity(true);
    }

    /**
     * 空间占位符
     *
     * @param rec    矩形区域
     * @param aFloat 浮动方向
     * @return 空间占位符
     */
    public static Div placeholder(Rectangle rec, AFloat aFloat) {
        return placeholder(rec.getWidth(), rec.getHeight(), aFloat);
    }
}
