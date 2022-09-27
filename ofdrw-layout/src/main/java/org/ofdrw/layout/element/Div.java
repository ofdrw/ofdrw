package org.ofdrw.layout.element;


import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.layout.RenderPrepare;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.engine.ElementSplit;

import java.time.LocalDateTime;
import java.util.Arrays;

/**
 * 盒式模型基础
 * <p>
 * 每个继承Div的对象都不必须提供泛型参数T,用于简化链式调用。
 *
 * @param <T> 链式调用返还值，Div的子类
 * @author 权观宇
 * @since 2020-02-03 12:46:15
 */
public class Div<T extends Div> implements RenderPrepare, ElementSplit {

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
    private double left = 0d;

    /**
     * 相对于段的右边界距离
     */
    private double right = 0d;

    /**
     * 相对坐标的top
     */
    private double top = 0d;

    /**
     * 元素整体透明度
     * <p>
     * null 表示不透明
     * <p>
     * 取值区间 [0,1]
     */
    private Double opacity = null;

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
     * 图层，默认为Body
     */
    private Type layer = Type.Body;

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

    /**
     * 获取透明度
     *
     * @return 透明度取值区间 [0,1]
     */
    public Double getOpacity() {
        return opacity;
    }

    /**
     * 设置透明度
     * <p>
     * 0 - 表示全透明， 1 - 表示不透明
     *
     * @param opacity 透明度取值区间 [0,1]
     * @return this
     */
    public T setOpacity(Double opacity) {
        if (opacity == null) {
        } else if (opacity > 1) {
            opacity = 1d;
        } else if (opacity < 0) {
            opacity = 0d;
        }
        this.opacity = opacity;
        return (T) this;
    }

    public int[] getBorderColor() {
        return borderColor;
    }

    public T setBorderColor(int r, int g, int b) {
        this.borderColor = new int[]{r, g, b};
        return (T) this;
    }

    public T setBorderColor(int[] rgb) {
        this.borderColor = rgb;
        return (T) this;
    }

    public Double getTop() {
        return top;
    }

    public T setTop(Double top) {
        this.top = top;
        return (T) this;
    }

    public Position getPosition() {
        return position;
    }

    public T setPosition(Position position) {
        this.position = position;
        return (T) this;
    }

    public int[] getBackgroundColor() {
        return backgroundColor;
    }

    public T setBackgroundColor(int r, int g, int b) {
        this.backgroundColor = new int[]{r, g, b};
        return (T) this;
    }

    /**
     * 设置背景颜色 RGB
     *
     * @param backgroundColor RGB数组
     * @return this
     */
    Div setBackgroundColor(int[] backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public Double getWidth() {
        return width;
    }

    public T setWidth(Double width) {
        this.width = width;
        return (T) this;
    }

    public Double getHeight() {
        return height;
    }

    public T setHeight(Double height) {
        this.height = height;
        return (T) this;
    }

    public Double[] getPadding() {
        return padding;
    }

    /**
     * 设置 内边距
     * <p>
     * 根据参数的参数不同设置涉及到了到 上、右、下、左 各部分参数
     * 1个参数，上、右、下、左 都相同      {arr[0], arr[0], arr[0], arr[0]}
     * <p>
     * 2个参数，上和下相同、左和右相同       {arr[0], arr[1], arr[0], arr[1]}
     * <p>
     * 3个参数，上、右、下、左(0) 分别设置  {arr[0], arr[1], arr[2], 0}
     * <p>
     * 4个参数，上、右、下、左 分别设置     {arr[0], arr[1], arr[2], arr[3]}
     *
     * @param padding 内边距，可变参数。
     * @return this
     */
    public T setPadding(Double... padding) {
        this.padding = ArrayParamTool.arr4p(padding);
        return (T) this;
    }

    public Double[] getBorder() {
        return border;
    }

    /**
     * 设置 边框宽度
     * <p>
     * 根据参数的参数不同设置涉及到了到 上、右、下、左 各部分参数
     * 1个参数，上、右、下、左 都相同      {arr[0], arr[0], arr[0], arr[0]}
     * <p>
     * 2个参数，上和下相同、左和右相同       {arr[0], arr[1], arr[0], arr[1]}
     * <p>
     * 3个参数，上、右、下、左(0) 分别设置  {arr[0], arr[1], arr[2], 0}
     * <p>
     * 4个参数，上、右、下、左 分别设置     {arr[0], arr[1], arr[2], arr[3]}
     *
     * @param border 边框宽度，可变参数。
     * @return this
     */
    public T setBorder(Double... border) {
        this.border = ArrayParamTool.arr4p(border);
        return (T) this;
    }

    public Double[] getMargin() {
        return margin;
    }

    /**
     * 设置 外边距
     * <p>
     * 根据参数的参数不同设置涉及到了到 上、右、下、左 各部分参数
     * 1个参数，上、右、下、左 都相同      {arr[0], arr[0], arr[0], arr[0]}
     * <p>
     * 2个参数，上和下相同、左和右相同       {arr[0], arr[1], arr[0], arr[1]}
     * <p>
     * 3个参数，上、右、下、左(0) 分别设置  {arr[0], arr[1], arr[2], 0}
     * <p>
     * 4个参数，上、右、下、左 分别设置     {arr[0], arr[1], arr[2], arr[3]}
     *
     * @param margin 外边距，可变参数。
     * @return this
     */
    public T setMargin(Double... margin) {
        this.margin = ArrayParamTool.arr4p(margin);
        return (T) this;
    }

    public Double getMarginTop() {
        return margin[0];
    }

    public T setMarginTop(Double top) {
        margin[0] = top;
        return (T) this;
    }

    public Double getMarginRight() {
        return margin[1];
    }

    public T setMarginRight(Double right) {
        margin[1] = right;
        return (T) this;
    }

    public Double getMarginBottom() {
        return margin[2];
    }

    public T setMarginBottom(Double bottom) {
        margin[2] = bottom;
        return (T) this;
    }

    public Double getMarginLeft() {
        return margin[3];
    }

    public T setMarginLeft(Double left) {
        margin[3] = left;
        return (T) this;
    }

    public Double getBorderTop() {
        return border[0];
    }

    public T setBorderTop(Double top) {
        border[0] = top;
        return (T) this;
    }

    public Double getBorderRight() {
        return border[1];
    }

    public T setBorderRight(Double right) {
        border[1] = right;
        return (T) this;
    }

    public Double getBorderBottom() {
        return border[2];
    }

    public T setBorderBottom(Double bottom) {
        border[2] = bottom;
        return (T) this;
    }

    public Double getBorderLeft() {
        return border[3];
    }

    public T setBorderLeft(Double left) {
        border[3] = left;
        return (T) this;
    }


    public Double getPaddingTop() {
        return padding[0];
    }

    public T setPaddingTop(Double top) {
        padding[0] = top;
        return (T) this;
    }

    public Double getPaddingRight() {
        return padding[1];
    }

    public T setPaddingRight(Double right) {
        padding[1] = right;
        return (T) this;
    }

    public Double getPaddingBottom() {
        return padding[2];
    }

    public T setPaddingBottom(Double bottom) {
        padding[2] = bottom;
        return (T) this;
    }

    public Double getPaddingLeft() {
        return padding[3];
    }

    public T setPaddingLeft(Double left) {
        padding[3] = left;
        return (T) this;
    }


    public Double getX() {
        return x;
    }

    public T setX(Double x) {
        this.x = x;
        return (T) this;
    }

    public Double getY() {
        return y;
    }

    public T setY(Double y) {
        this.y = y;
        return (T) this;
    }

    /**
     * 设置位置
     *
     * @param x 左上角X坐标
     * @param y 左上角Y坐标
     * @return this
     */
    public T setXY(Double x, Double y) {
        setX(x);
        setY(y);
        return (T) this;
    }

    /**
     * 设置盒式模型的主要属性
     *
     * @param x      左上角X坐标
     * @param y      左上角Y坐标
     * @param width  宽度
     * @param height 高度
     * @return this
     */
    public T setBox(Double x, Double y, Double width, Double height) {
        setX(x);
        setY(y);
        setWidth(width);
        setHeight(height);
        return (T) this;
    }

    public Clear getClear() {
        return clear;
    }

    public T setClear(Clear clear) {
        this.clear = clear;
        return (T) this;
    }

    public AFloat getFloat() {
        return aFloat;
    }

    /**
     * 设置浮动样式
     * <p>
     * 注意：如果需要设置居中，那么还需要同时设置 {@link #setClear(Clear)}
     * 为{@link Clear#none}或{@link Clear#right}
     *
     * @param aFloat 浮动样式
     * @return this
     */
    public T setFloat(AFloat aFloat) {
        this.aFloat = aFloat;
        return (T) this;
    }

    public Double getLeft() {
        return left;
    }

    public T setLeft(Double left) {
        this.left = left;
        return (T) this;
    }

    public Double getRight() {
        return right;
    }

    public T setRight(Double right) {
        this.right = right;
        return (T) this;
    }

    /**
     * 元素是否可以拆分
     *
     * @return true - 可以拆分；false - 无法拆分
     */
    public Boolean isIntegrity() {
        return integrity;
    }

    public T setIntegrity(Boolean integrity) {
        this.integrity = integrity;
        return (T) this;
    }

    /**
     * @return 额外宽度
     */
    public double widthPlus() {
        return (this.margin[1] + this.margin[3])
                + (this.padding[1] + this.padding[3])
                + (this.border[1] + this.border[3]);
    }

    /**
     * @return 额外高度
     */
    public double heightPlus() {
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
     * 返回 图层
     * <p>
     * 默认为Body
     *
     * @return 图层
     */
    public Type getLayer() {
        return layer;
    }

    /**
     * 设置 图层
     *
     * @param layer 图层
     * @return this
     */
    public T setLayer(Type layer) {
        this.layer = layer;
        return (T) this;
    }

    /**
     * 判断是否为占位符
     *
     * @return true 占位符，不参与渲染， false - 非占位符
     */
    public boolean isPlaceholder() {
        return placeholder;
    }

    public T setPlaceholder(boolean placeholder) {
        this.placeholder = placeholder;
        return (T) this;
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
        div.setBorderColor(borderColor == null ? null : borderColor.clone());
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
        div.setLayer(layer);
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
                    .setMarginBottom(0d)
                    // 只有Margin那么只是一个占位符
                    .setPlaceholder(true);
            // 减去部分残留在上一个段的margin
            div2.setMarginTop(deltaM);
        } else if (getMarginTop() + getBorderTop() >= sHeight) {
            // Border + Margin 耗尽了空间 分段的情况
            double deltaB = getBorderTop() - (sHeight - getMarginTop());
            // 剩余空间除去Margin剩下都是border
            div1.setBorderTop(sHeight - getMarginTop())
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setBorderBottom(0d)
                    .setMarginBottom(0d);
            // 减去margin和border
            div2.setMarginTop(0d)
                    .setBorderTop(deltaB);
        } else if (getMarginTop() + getBorderTop() + getPaddingTop() >= sHeight) {
            double deltaP = getPaddingTop() - (sHeight - getMarginTop() - getBorderTop());
            div1.setPaddingTop(sHeight - getMarginTop() - getBorderTop())
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setMarginBottom(0d);
            div2.setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(deltaP);
        } else if (getMarginTop() + getBorderTop() + getPaddingTop() + getHeight() >= sHeight) {
            // 内容分割调整
            Div[] divs = contentSplitAdjust(sHeight, div1, div2);
            div1 = divs[0];
            div2 = divs[1];
        } else if (getMarginTop() + getBorderTop() + getPaddingTop() + getHeight() + getPaddingBottom() >= sHeight) {
            double deltaP = sHeight - (getMarginTop() + getBorderTop() + getPaddingTop() + getHeight());
            div1.setPaddingBottom(deltaP)
                    .setBorderBottom(0d)
                    .setMarginBottom(0d);
            div2.setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(div2.getPaddingBottom() - deltaP);
        } else if (
                getMarginTop() + getBorderTop() + getPaddingTop() +
                        getHeight() + getPaddingBottom() + getBorderBottom() >= sHeight) {
            double deltaB = sHeight - (getMarginTop() + getBorderTop() + getPaddingTop() +
                    getHeight() + getPaddingBottom());

            div1.setBorderBottom(deltaB)
                    .setMarginBottom(0d);
            div2.setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setBorderBottom(div2.getBorderBottom() - deltaB);

        } else {
            double deltaM = sHeight - (getMarginTop() + getBorderTop() + getPaddingTop() +
                    getHeight() + getPaddingBottom() + getBorderBottom());
            div1.setMarginBottom(deltaM);
            div2.setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(0d)
                    .setHeight(0d)
                    .setPaddingBottom(0d)
                    .setBorderBottom(0d)
                    .setMarginBottom(div2.getMarginBottom() - deltaM)
                    .setPlaceholder(true);
        }
        return new Div[]{div1, div2};
    }

    /**
     * 内容分割调整
     * <p>
     * 根据分割高度调整两个克隆元素，达成分割元素的效果
     *
     * @param <T>     Div子类泛型参数
     * @param sHeight 分割内容的高度
     * @param div1    克隆元素1
     * @param div2    克隆元素2
     * @return 分割调整后的两个Div
     */
    public <T extends Div> Div[] contentSplitAdjust(double sHeight, T div1, T div2) {
        /*
         * 调整边框等配置
         * div1 无下边，总高度为切分高度
         * div2 无上边，高度为剩余高度
         */
        // 减去顶边的布局区域
        double h1 = sHeight - (div1.getMarginTop() + div1.getBorderTop() + div1.getPaddingTop());
        div1.setHeight(h1)
                // 取消低边的所有布局
                .setMarginBottom(0d)
                .setBorderBottom(0d)
                .setPaddingBottom(0d);

        // 减去截断内容
        double h2 = div2.getHeight() - h1;
        div2.setHeight(h2)
                // 取消顶边的所有布局
                .setMarginTop(0d)
                .setBorderTop(0d)
                .setPaddingTop(0d);
        return new Div[]{div1, div2};
    }


    @Override
    public String toString() {
        return "Div{" +
                "backgroundColor=" + Arrays.toString(backgroundColor) +
                ", borderColor=" + Arrays.toString(borderColor) +
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
                ", opacity=" + opacity +
                ", position=" + position +
                ", integrity=" + integrity +
                ", placeholder=" + placeholder +
                ", layer=" + layer +
                '}';
    }
}
