package org.ofdrw.core.basicType;

/**
 * 点坐标，以格分割，前者为 x值，后者为 y值，可以是整数或浮点数
 * <p>
 * 示例：
 * <code>0 0</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 */
public class PosString {

    /**
     * X坐标
     * <p>
     * 从左 到 右
     */
    private Double x = 0d;
    /**
     * y坐标
     * <p>
     * 从上 到 下
     */
    private Double y = 0d;

    public PosString(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Double getX() {
        return x;
    }

    public PosString setX(Double x) {
        this.x = x;
        return this;
    }

    public Double getY() {
        return y;
    }

    public PosString setY(Double y) {
        this.y = y;
        return this;
    }

    @Override
    public String toString() {
        return (x + " " + y);
    }
}
