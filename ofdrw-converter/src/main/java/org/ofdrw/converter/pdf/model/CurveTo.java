package org.ofdrw.converter.pdf.model;

/**
 * 三次贝塞尔曲线段，对应 PDF 操作符 {@code c}
 * <p>
 * OFD 的 {@code B} 与 {@code C} 命令、以及圆弧 {@code A} 的转换结果都落到本类型。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class CurveTo implements PathSegment {

    /**
     * 第一个控制点横坐标，单位 pt
     */
    private final double x1;

    /**
     * 第一个控制点纵坐标，单位 pt
     */
    private final double y1;

    /**
     * 第二个控制点横坐标，单位 pt
     */
    private final double x2;

    /**
     * 第二个控制点纵坐标，单位 pt
     */
    private final double y2;

    /**
     * 终点横坐标，单位 pt
     */
    private final double x3;

    /**
     * 终点纵坐标，单位 pt
     */
    private final double y3;

    /**
     * 构造三次贝塞尔曲线段
     *
     * @param x1 第一个控制点横坐标，单位 pt
     * @param y1 第一个控制点纵坐标，单位 pt
     * @param x2 第二个控制点横坐标，单位 pt
     * @param y2 第二个控制点纵坐标，单位 pt
     * @param x3 终点横坐标，单位 pt
     * @param y3 终点纵坐标，单位 pt
     */
    public CurveTo(double x1, double y1, double x2, double y2, double x3, double y3) {
        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;
        this.x3 = x3;
        this.y3 = y3;
    }

    /**
     * @return 第一个控制点横坐标，单位 pt
     */
    public double getX1() {
        return x1;
    }

    /**
     * @return 第一个控制点纵坐标，单位 pt
     */
    public double getY1() {
        return y1;
    }

    /**
     * @return 第二个控制点横坐标，单位 pt
     */
    public double getX2() {
        return x2;
    }

    /**
     * @return 第二个控制点纵坐标，单位 pt
     */
    public double getY2() {
        return y2;
    }

    /**
     * @return 终点横坐标，单位 pt
     */
    public double getX3() {
        return x3;
    }

    /**
     * @return 终点纵坐标，单位 pt
     */
    public double getY3() {
        return y3;
    }

    @Override
    public double getEndX() {
        return x3;
    }

    @Override
    public double getEndY() {
        return y3;
    }

    @Override
    public String toString() {
        return "C " + x1 + " " + y1 + " " + x2 + " " + y2 + " " + x3 + " " + y3;
    }
}
