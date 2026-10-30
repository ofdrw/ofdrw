package org.ofdrw.converter.pdf.model;

/**
 * 页面坐标系中的点
 * <p>
 * 坐标单位为 pt，原点位于页面左下角，Y 轴向上（PDF 坐标系）。
 * 与 OFD 的“左上角原点、Y 轴向下、单位 mm”之间的换算必须通过 {@link org.ofdrw.converter.pdf.CoordSystem}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class Point {

    /**
     * 横坐标，单位 pt
     */
    private final double x;

    /**
     * 纵坐标，单位 pt
     */
    private final double y;

    /**
     * 构造点
     *
     * @param x 横坐标，单位 pt（PDF 页面坐标）
     * @param y 纵坐标，单位 pt（PDF 页面坐标）
     */
    public Point(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * @return 横坐标，单位 pt
     */
    public double getX() {
        return x;
    }

    /**
     * @return 纵坐标，单位 pt
     */
    public double getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Point)) {
            return false;
        }
        Point other = (Point) o;
        return Double.compare(x, other.x) == 0 && Double.compare(y, other.y) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(x) * 31 + Double.hashCode(y);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
