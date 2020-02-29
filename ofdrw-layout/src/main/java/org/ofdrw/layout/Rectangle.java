package org.ofdrw.layout;


/**
 * 矩形区域
 *  @author 权观宇
  * @since 2020-02-28 04:12:39
 */
public class Rectangle {
    private double width;
    private double height;

    public static Rectangle Empty = new Rectangle(0, 0 );

    public Rectangle(double width, double height) {
        this.width = width;
        this.height = height;
    }

    /**
     * 增加宽度
     * @param delta 增加量（正为增，负为减）
     */
    public void addToWidth(double delta) {
        this.width += delta;
    }

    /**
     * 增加高度
     * @param delta 增加量（正为增，负为减）
     */
    public void addToHeight(double delta) {
        this.height += delta;
    }

    public boolean isEmpty(){
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Rectangle) {
            Rectangle that = (Rectangle) obj;
            if (that.getHeight() == this.height && that.getWidth() == this.width) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        return "Rectangle{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
}
