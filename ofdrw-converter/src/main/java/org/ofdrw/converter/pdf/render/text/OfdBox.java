package org.ofdrw.converter.pdf.render.text;

/**
 * OFD 矩形（左上角原点，单位 mm）
 * <p>
 * 文字渲染只需要“外接矩形”这一个几何值，为保持 {@code render} 包不依赖 OFD 核心模型而单独定义。
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class OfdBox {

    /**
     * 左上角横坐标，单位 mm
     */
    private final double x;

    /**
     * 左上角纵坐标，单位 mm
     */
    private final double y;

    /**
     * 宽度，单位 mm
     */
    private final double width;

    /**
     * 高度，单位 mm
     */
    private final double height;

    /**
     * 构造矩形
     *
     * @param x      左上角横坐标，单位 mm
     * @param y      左上角纵坐标，单位 mm
     * @param width  宽度，单位 mm
     * @param height 高度，单位 mm
     */
    public OfdBox(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * @return 左上角横坐标，单位 mm
     */
    public double getX() {
        return x;
    }

    /**
     * @return 左上角纵坐标，单位 mm
     */
    public double getY() {
        return y;
    }

    /**
     * @return 宽度，单位 mm
     */
    public double getWidth() {
        return width;
    }

    /**
     * @return 高度，单位 mm
     */
    public double getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "OfdBox[" + x + "," + y + "," + width + "x" + height + "]";
    }
}
