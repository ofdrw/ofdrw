package org.ofdrw.converter.pdf.model;

/**
 * 中间模型矩形区域
 * <p>
 * 本类只做数值承载，<b>不决定原点位置与单位</b>：单位与方向由使用它的上下文约定（见各类 Javadoc）。
 * <p>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class Box {

    /**
     * 左上角横坐标
     */
    private final double x;

    /**
     * 左上角纵坐标
     */
    private final double y;

    /**
     * 宽度
     */
    private final double width;

    /**
     * 高度
     */
    private final double height;

    /**
     * 构造矩形
     *
     * @param x      左上角横坐标
     * @param y      左上角纵坐标
     * @param width  宽度
     * @param height 高度
     */
    public Box(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 创建以左上角为 (0, 0) 的矩形（对象空间外接矩形常用）
     *
     * @param width  宽度
     * @param height 高度
     * @return 矩形
     */
    public static Box ofSize(double width, double height) {
        return new Box(0, 0, width, height);
    }

    /**
     * @return 左上角横坐标
     */
    public double getX() {
        return x;
    }

    /**
     * @return 左上角纵坐标
     */
    public double getY() {
        return y;
    }

    /**
     * @return 宽度
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return 高度
     */
    public double getHeight() {
        return height;
    }

    /**
     * @return 右边界坐标
     */
    public double getRight() {
        return x + width;
    }

    /**
     * @return 下边界坐标（在左上角原点坐标系中）
     */
    public double getBottom() {
        return y + height;
    }

    /**
     * 平移矩形
     *
     * @param dx 横向位移
     * @param dy 纵向位移
     * @return 新的矩形
     */
    public Box translate(double dx, double dy) {
        return new Box(x + dx, y + dy, width, height);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Box)) {
            return false;
        }
        Box other = (Box) o;
        return Double.compare(x, other.x) == 0
                && Double.compare(y, other.y) == 0
                && Double.compare(width, other.width) == 0
                && Double.compare(height, other.height) == 0;
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(new double[]{x, y, width, height});
    }

    @Override
    public String toString() {
        return "Box[x=" + x + ", y=" + y + ", w=" + width + ", h=" + height + "]";
    }
}
