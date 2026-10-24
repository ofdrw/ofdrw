package org.ofdrw.converter.renderer.command;

/**
 * 移动到指定位置的路径命令。
 */
public final class MoveToCommand extends DrawCommand {

    /**
     * X 坐标，单位：像素
     */
    private final double x;

    /**
     * Y 坐标，单位：像素
     */
    private final double y;

    /**
     * 构造移动命令。
     *
     * @param x X 坐标，单位：像素
     * @param y Y 坐标，单位：像素
     */
    public MoveToCommand(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 获取 X 坐标。
     *
     * @return X 坐标，单位：像素
     */
    public double getX() {
        return x;
    }

    /**
     * 获取 Y 坐标。
     *
     * @return Y 坐标，单位：像素
     */
    public double getY() {
        return y;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 包含该点坐标的数组，单位：像素
     */
    @Override
    public double[][] getBounds() {
        return new double[][]{{x, y}};
    }

    @Override
    public String toString() {
        return "MoveTo(" + x + ", " + y + ")";
    }
}
