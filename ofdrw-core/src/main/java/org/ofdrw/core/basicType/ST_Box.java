package org.ofdrw.core.basicType;

import java.io.Serializable;

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
public class ST_Box extends STBase implements Cloneable {

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


    public ST_Box(double topLeftX, double topLeftY, double width, double height) {
        this.topLeftX = topLeftX;
        this.topLeftY = topLeftY;
        this.width = width;
        this.height = height;
    }

    /**
     * 通用构造
     *
     * @param arr 任意类型可序列化参数
     */
    public ST_Box(Serializable... arr) {
        if (arr == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (arr.length == 4) {
            throw new IllegalArgumentException("Box 必须元素个数等4");
        }

        this.topLeftX = toDouble(arr[0].toString());
        this.topLeftY = toDouble(arr[1].toString());
        if (width <= 0) {
            throw new IllegalArgumentException("width 应大于0");
        }
        this.width = toDouble(arr[2].toString());
        if (height <= 0) {
            throw new IllegalArgumentException("height 应大于0");
        }
        this.height = toDouble(arr[3].toString());
    }


    /**
     * 获取 ST_Box 实例如果参数非法则返还null
     *
     * @param arrStr 数字字符串
     * @return 实例 或 null
     */
    public static ST_Box getInstance(String arrStr) {
        if (arrStr == null || arrStr.trim().length() == 0) {
            return null;
        }
        String[] values = arrStr.trim().split(" ");

        if (values.length != 4) {
            return null;
        }
        return new ST_Box(toDouble(values[0]),
                toDouble(values[1]),
                toDouble(values[2]),
                toDouble(values[3]));
    }

    public Double getTopLeftX() {
        return topLeftX;
    }

    public ST_Box setTopLeftX(Double topLeftX) {
        this.topLeftX = topLeftX;
        return this;
    }

    public Double getTopLeftY() {
        return topLeftY;
    }

    public ST_Box setTopLeftY(Double topLeftY) {
        this.topLeftY = topLeftY;
        return this;
    }

    public Double getWidth() {
        return width;
    }

    public ST_Box setWidth(Double width) {
        this.width = width;
        return this;
    }

    public Double getHeight() {
        return height;
    }

    public ST_Box setHeight(Double height) {
        this.height = height;
        return this;
    }

    /**
     * 获取左上角坐标定点
     *
     * @return 左上角坐标
     */
    public ST_Pos getTopLeftPos() {
        return new ST_Pos(this.topLeftX, this.topLeftY);
    }

    @Override
    public String toString() {
        return String.format("%s %s %s %s", fmt(topLeftX), fmt(topLeftY), fmt(width), fmt(height));
    }

    @Override
    public ST_Box clone() {
        return new ST_Box(topLeftX, topLeftY, width, height);
    }
}
