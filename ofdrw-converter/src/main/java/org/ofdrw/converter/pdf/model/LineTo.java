package org.ofdrw.converter.pdf.model;

/**
 * 直线段，对应 PDF 操作符 {@code l}
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class LineTo implements PathSegment {

    /**
     * 终点横坐标，单位 pt
     */
    private final double x;

    /**
     * 终点纵坐标，单位 pt
     */
    private final double y;

    /**
     * 构造直线段
     *
     * @param x 终点横坐标，单位 pt
     * @param y 终点纵坐标，单位 pt
     */
    public LineTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return 终点横坐标，单位 pt
     */
    public double getX() {
        return x;
    }

    /**
     * @return 终点纵坐标，单位 pt
     */
    public double getY() {
        return y;
    }

    @Override
    public double getEndX() {
        return x;
    }

    @Override
    public double getEndY() {
        return y;
    }

    @Override
    public String toString() {
        return "L " + x + " " + y;
    }
}
