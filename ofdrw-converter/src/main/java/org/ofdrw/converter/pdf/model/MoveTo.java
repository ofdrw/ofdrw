package org.ofdrw.converter.pdf.model;

/**
 * 子路径起点，对应 PDF 操作符 {@code m}
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class MoveTo implements PathSegment {

    /**
     * 起点横坐标，单位 pt
     */
    private final double x;

    /**
     * 起点纵坐标，单位 pt
     */
    private final double y;

    /**
     * 构造起点
     *
     * @param x 起点横坐标，单位 pt
     * @param y 起点纵坐标，单位 pt
     */
    public MoveTo(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return 起点横坐标，单位 pt
     */
    public double getX() {
        return x;
    }

    /**
     * @return 起点纵坐标，单位 pt
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
        return "M " + x + " " + y;
    }
}
