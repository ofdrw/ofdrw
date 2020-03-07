package org.ofdrw.layout;


/**
 * 矩形区域
 *
 * @author 权观宇
 * @since 2020-02-28 04:12:39
 */
public class Rectangle {
    /**
     * 矩形位置坐标默认为0
     */
    private double x = 0;
    private double y = 0;
    private double width;
    private double height;

    public static final Rectangle Empty = new Rectangle(0, 0);

    public Rectangle(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 增加宽度
     *
     * @param delta 增加量（正为增，负为减）
     */
    public void addToWidth(double delta) {
        this.width += delta;
    }

    /**
     * 增加高度
     *
     * @param delta 增加量（正为增，负为减）
     */
    public void addToHeight(double delta) {
        this.height += delta;
    }

    public boolean isEmpty() {
        return this.width == 0 && this.height == 0;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public double getHeight() {
        return height;
    }

    public void setHeight(double height) {
        this.height = height;
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    /**
     * 缩减矩形区域
     * @param height 缩减高度
     * @return 减少的区域
     */
    public Rectangle reduce(double height) {
        Rectangle res = new Rectangle(x, y, width, height);
        this.height -= height;
        this.y += height;
        return res;
    }

    /**
     * @return 克隆对象
     */
    @Override
    public Rectangle clone() {
        return new Rectangle(x, y, width, height);
    }

    @Override
    public String toString() {
        return "Rectangle{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
