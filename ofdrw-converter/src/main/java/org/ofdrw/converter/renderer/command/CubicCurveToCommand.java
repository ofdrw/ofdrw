package org.ofdrw.converter.renderer.command;

/**
 * 三次贝塞尔曲线路径命令。
 */
public final class CubicCurveToCommand extends DrawCommand {

    /**
     * 第一个控制点 X 坐标，单位：像素
     */
    private final double cp1x;

    /**
     * 第一个控制点 Y 坐标，单位：像素
     */
    private final double cp1y;

    /**
     * 第二个控制点 X 坐标，单位：像素
     */
    private final double cp2x;

    /**
     * 第二个控制点 Y 坐标，单位：像素
     */
    private final double cp2y;

    /**
     * 终点 X 坐标，单位：像素
     */
    private final double x;

    /**
     * 终点 Y 坐标，单位：像素
     */
    private final double y;

    /**
     * 构造三次贝塞尔曲线命令。
     *
     * @param cp1x 第一个控制点 X 坐标，单位：像素
     * @param cp1y 第一个控制点 Y 坐标，单位：像素
     * @param cp2x 第二个控制点 X 坐标，单位：像素
     * @param cp2y 第二个控制点 Y 坐标，单位：像素
     * @param x    终点 X 坐标，单位：像素
     * @param y    终点 Y 坐标，单位：像素
     */
    public CubicCurveToCommand(double cp1x, double cp1y, double cp2x, double cp2y, double x, double y) {
        this.cp1x = cp1x;
        this.cp1y = cp1y;
        this.cp2x = cp2x;
        this.cp2y = cp2y;
        this.x = x;
        this.y = y;
    }

    /**
     * 获取第一个控制点 X 坐标。
     *
     * @return 第一个控制点 X 坐标，单位：像素
     */
    public double getCp1x() {
        return cp1x;
    }

    /**
     * 获取第一个控制点 Y 坐标。
     *
     * @return 第一个控制点 Y 坐标，单位：像素
     */
    public double getCp1y() {
        return cp1y;
    }

    /**
     * 获取第二个控制点 X 坐标。
     *
     * @return 第二个控制点 X 坐标，单位：像素
     */
    public double getCp2x() {
        return cp2x;
    }

    /**
     * 获取第二个控制点 Y 坐标。
     *
     * @return 第二个控制点 Y 坐标，单位：像素
     */
    public double getCp2y() {
        return cp2y;
    }

    /**
     * 获取终点 X 坐标。
     *
     * @return 终点 X 坐标，单位：像素
     */
    public double getX() {
        return x;
    }

    /**
     * 获取终点 Y 坐标。
     *
     * @return 终点 Y 坐标，单位：像素
     */
    public double getY() {
        return y;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 两个控制点与终点坐标数组，单位：像素
     */
    @Override
    public double[][] getBounds() {
        return new double[][]{{cp1x, cp1y}, {cp2x, cp2y}, {x, y}};
    }

    @Override
    public String toString() {
        return "CubicCurveTo(" + cp1x + ", " + cp1y + ", " + cp2x + ", " + cp2y + ", " + x + ", " + y + ")";
    }
}
