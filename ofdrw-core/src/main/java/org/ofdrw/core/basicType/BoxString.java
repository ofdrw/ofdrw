package org.ofdrw.core.basicType;

/**
 * 矩形区域，以空格分割，前两个值代表了该矩形的
 * 左上角的坐标，后两个值依次表示该矩形的宽和高，
 * 可以是整数或者浮点数，后两个值应大于0
 * <p>
 * 示例：
 * <code>10 10 50 50</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 11:05:06
 */
public class BoxString {

    /**
     * 左上角 x坐标
     * <p>
     * 从左 到 右
     */
    private Double topLeftX = 0d;

    /**
     * 左上角 y坐标
     * <p>
     * 从上 到 下
     */
    private Double topLeftY = 0d;

    /**
     * 宽度
     */
    private Double width = 0d;

    /**
     * 高度
     */
    private Double height = 0d;


    public BoxString(double topLeftX, double topLeftY, double width, double height) {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        if (width <= 0) {
            throw new IllegalArgumentException("width 应大于0");
        }
        this.width = width;
        if (height <= 0) {
            throw new IllegalArgumentException("height 应大于0");
        }
        this.height = height;
    }


    public Double getTopLeftX() {
        return topLeftX;
    }

    public BoxString setTopLeftX(Double topLeftX) {
        this.topLeftX = topLeftX;
        return this;
    }

    public Double getTopLeftY() {
        return topLeftY;
    }

    public BoxString setTopLeftY(Double topLeftY) {
        this.topLeftY = topLeftY;
        return this;
    }

    public Double getWidth() {
        return width;
    }

    public BoxString setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public BoxString setHeight(Double height) {
        this.height = height;
        return this;
    }

    /**
     * 获取左上角坐标定点
     *
     * @return 左上角坐标
     */
    public PosString getTopLeftPos() {
        return new PosString(this.topLeftX, this.topLeftY);
    }

    @Override
    public String toString() {
        return topLeftX + " " + topLeftY + " " + width + " " + height;
    }
}
