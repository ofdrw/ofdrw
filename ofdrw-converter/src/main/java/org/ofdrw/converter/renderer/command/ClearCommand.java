package org.ofdrw.converter.renderer.command;

/**
 * 清空矩形区域的命令，用于透明背景擦除。
 */
public final class ClearCommand extends DrawCommand {

    /**
     * 左上角 X 坐标，单位：像素
     */
    private final double x;

    /**
     * 左上角 Y 坐标，单位：像素
     */
    private final double y;

    /**
     * 宽度，单位：像素
     */
    private final double width;

    /**
     * 高度，单位：像素
     */
    private final double height;

    /**
     * 构造清空命令。
     *
     * @param x      左上角 X 坐标，单位：像素
     * @param y      左上角 Y 坐标，单位：像素
     * @param width  宽度，单位：像素
     * @param height 高度，单位：像素
     */
    public ClearCommand(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * 获取左上角 X 坐标。
     *
     * @return 左上角 X 坐标，单位：像素
     */
    public double getX() {
        return x;
    }

    /**
     * 获取左上角 Y 坐标。
     *
     * @return 左上角 Y 坐标，单位：像素
     */
    public double getY() {
        return y;
    }

    /**
     * 获取宽度。
     *
     * @return 宽度，单位：像素
     */
    public double getWidth() {
        return width;
    }

    /**
     * 获取高度。
     *
     * @return 高度，单位：像素
     */
    public double getHeight() {
        return height;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 矩形两个对角点坐标数组，单位：像素
     */
    @Override
    public double[][] getBounds() {
        return new double[][]{{x, y}, {x + width, y + height}};
    }

    @Override
    public String toString() {
        return "Clear(" + x + ", " + y + ", " + width + ", " + height + ")";
    }
}
