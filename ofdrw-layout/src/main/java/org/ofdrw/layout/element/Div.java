package org.ofdrw.layout.element;


import org.ofdrw.layout.RenderPrepare;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.engine.ElementSplit;

import java.util.Arrays;

/**
 * 盒式模型基础
 *
 * @author 权观宇
 * @since 2020-02-03 12:46:15
 */
public class Div implements RenderPrepare, ElementSplit {

    /**
     * 背景颜色
     * <p>
     * (R,G,B) 三色数组
     */
    private int[] backgroundColor = null;

    /**
     * 边框颜色
     * <p>
     * (R,G,B) 三色数组
     */
    private int[] borderColor = null;

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

    public Div() {
    }

    public Div(Double width, Double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 是否存在边框
     *
     * @return true 不存在；false 存在
     */
    public boolean isNoBorder() {
        return getBorderTop() == 0d
                && getBorderRight() == 0d
                && getBorderBottom() == 0d
                && getBorderLeft() == 0d;
    }

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

    public int[] getBorderColor() {
        return borderColor;
    }

    public Div setBorderColor(int r, int g, int b) {
        this.borderColor = new int[]{r, g, b};
        return this;
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

    Div setBackgroundColor(int[] backgroundColor) {
        this.backgroundColor = backgroundColor;
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

    public Div setBorder(Double... border) {
        this.border = ArrayParamTool.arr4p(border);
        return this;
    }

    public Double[] getMargin() {
        return margin;
    }

    public Div setMargin(Double... margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return this;
    }

    public Double getMarginTop() {
        return margin[0];
    }

    public Div setMarginTop(Double top) {
        margin[0] = top;
        return this;
    }

    public Double getMarginRight() {
        return margin[1];
    }

    public Div setMarginRight(Double right) {
        margin[1] = right;
        return this;
    }

    public Double getMarginBottom() {
        return margin[2];
    }

    public Div setMarginBottom(Double bottom) {
        margin[2] = bottom;
        return this;
    }

    public Double getMarginLeft() {
        return margin[3];
    }

    public Div setMarginLeft(Double left) {
        margin[3] = left;
        return this;
    }

    public Double getBorderTop() {
        return border[0];
    }

    public Div setBorderTop(Double top) {
        border[0] = top;
        return this;
    }

    public Double getBorderRight() {
        return border[1];
    }

    public Div setBorderRight(Double right) {
        border[1] = right;
        return this;
    }

    public Double getBorderBottom() {
        return border[2];
    }

    public Div setBorderBottom(Double bottom) {
        border[2] = bottom;
        return this;
    }

    public Double getBorderLeft() {
        return border[3];
    }

    public Div setBorderLeft(Double left) {
        border[3] = left;
        return this;
    }


    public Double getPaddingTop() {
        return padding[0];
    }

    public Div setPaddingTop(Double top) {
        padding[0] = top;
        return this;
    }

    public Double getPaddingRight() {
        return padding[1];
    }

    public Div setPaddingRight(Double right) {
        padding[1] = right;
        return this;
    }

    public Double getPaddingBottom() {
        return padding[2];
    }

    public Div setPaddingBottom(Double bottom) {
        padding[2] = bottom;
        return this;
    }

    public Double getPaddingLeft() {
        return padding[3];
    }

    public Div setPaddingLeft(Double left) {
        padding[3] = left;
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

    /**
     * 元素是否可以拆分
     *
     * @return true - 可以拆分；false - 无法拆分
     */
    public Boolean isIntegrity() {
        return integrity;
    }

    public Div setIntegrity(Boolean integrity) {
        this.integrity = integrity;
        return this;
    }

    /**
     * @return 额外宽度
     */
    protected double widthPlus() {
        return (this.margin[1] + this.margin[3])
                + (this.padding[1] + this.padding[3])
                + (this.border[1] + this.border[3]);
    }

    /**
     * @return 额外高度
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
    public Rectangle doPrepare(Double widthLimit) {
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
     * 获取模型区域大小
     * <p>
     * 注意：该方法必须在元素内容大小确定的情况才能放回正确的尺寸
     * <p>
     * 也就是说必须在 {@link #doPrepare(Double)} 或是手动设置宽度和高度之后调用才能返还正确值
     *
     * @return 模型大小
     */
    public Rectangle box() {
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


    /**
     * 克隆Div
     *
     * @return 一模一样的全新Div对象
     */
    @Override
    public Div clone() {
        Div div = new Div();
        return copyTo(div);
    }

    /**
     * Clone到制定对象
     *
     * @param div 目标对象
     * @param <T> 泛型参数
     * @return 克隆复制后的对象
     */
    public <T extends Div> T copyTo(T div) {
        div.setBackgroundColor(backgroundColor == null ? null : backgroundColor.clone());
        div.setWidth(width);
        div.setHeight(height);
        div.setPadding(padding.clone());
        div.setBorder(border.clone());
        div.setMargin(margin.clone());
        div.setX(x);
        div.setY(y);
        div.setClear(clear);
        div.setFloat(aFloat);
        div.setLeft(left);
        div.setRight(right);
        div.setTop(top);
        div.setPosition(position);
        div.setIntegrity(integrity);
        div.setPlaceholder(placeholder);
        return div;

    }

    /**
     * 根据给定的高度切分元素
     * <p>
     * 截断元素前必须确定元素的宽度和高度，否则将会抛出异常
     * <p>
     * 元素的分割只作用于竖直方向上，水平方向不做分割每次只会截断1次。
     * <p>
     * 截断的元素在截断出均无margin、border、padding
     * <p>
     * 截断后的内容比截断高度高的多
     *
     * @param sHeight 切分高度
     * @return 根据给定空间分割之后的新元素
     */
    @Override
    public Div[] split(double sHeight) {
        if (width == null || height == null) {
            throw new RuntimeException("切分元素必须要有固定的宽度（width）和高度（height）");
        }
        double totalH = height + heightPlus();
        if (totalH <= sHeight) {
            // 小于切分高度时返还自身，表示不切分
            return new Div[]{this};
        }
        // 否则切分元素，首先克隆元素
        Div div1 = this.clone();
        Div div2 = this.clone();

        /*
         Margin border Padding 的考虑
         */
        if (getMarginTop() >= sHeight) {
            // Margin 分段情况
            double deltaM = getMarginTop() - sHeight;
            // 只留下一个Margin的div
            div1.setMarginTop(sHeight)
                    .setBorderTop(0d)
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setBorderBottom(0d)
                    .setMarginBottom(0d);
            // 减去部分残留在上一个段的margin
            div2.setMarginTop(deltaM);
            return new Div[]{div1, div2};
        } else if (getMarginTop() + getBorderTop() >= sHeight) {
            // Border + Margin 耗尽了空间 分段的情况
            double deltaB = getBorderTop() - (sHeight - getMarginTop());
            // 剩余空间除去Margin剩下都是border
            div1.setBorderTop(sHeight - getMarginTop())
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setMarginBottom(0d);
            // 减去margin和border
            div2.setMarginTop(0d)
                    .setBorderTop(deltaB);
            return new Div[]{div1, div2};
        } else if (getMarginTop() + getBorderTop() + getPaddingTop() >= sHeight) {
            double deltaP = getPaddingTop() - (sHeight - getMarginTop() - getBorderTop());
            div1.setPaddingTop(sHeight - getMarginTop() - getBorderTop())
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setMarginBottom(0d);
            div2.setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(deltaP);
            return new Div[]{div1, div2};
        }
        // 切割内容的情况
         /*
         调整边框等配置
         div1 无下边，总高度为切分高度
         div2 无上边，高度为剩余高度
          */
        // 减去顶边的布局区域
        double h1 = sHeight - (div1.getMarginTop() + div1.getBorderTop() + div1.getPaddingTop());
        div1.setHeight(h1)
                // 取消低边的所有布局
                .setMarginBottom(0d)
                .setBorderBottom(0d)
                .setPaddingBottom(0d);

        // 减去截断内容
        double h2 = div2.height - h1;
        div2.setHeight(h2)
                // 取消顶边的所有布局
                .setMarginTop(0d)
                .setBorderTop(0d)
                .setPaddingTop(0d);

        return new Div[]{
                div1, div2
        };
    }

    @Override
    public String toString() {
        return "Div{" +
                "backgroundColor=" + Arrays.toString(backgroundColor) +
                ", width=" + width +
                ", height=" + height +
                ", padding=" + Arrays.toString(padding) +
                ", border=" + Arrays.toString(border) +
                ", margin=" + Arrays.toString(margin) +
                ", x=" + x +
                ", y=" + y +
                ", clear=" + clear +
                ", aFloat=" + aFloat +
                ", left=" + left +
                ", right=" + right +
                ", top=" + top +
                ", position=" + position +
                ", integrity=" + integrity +
                ", placeholder=" + placeholder +
                '}';
    }
}
