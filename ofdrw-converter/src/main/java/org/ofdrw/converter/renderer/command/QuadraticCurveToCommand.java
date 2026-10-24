package org.ofdrw.converter.renderer.command;

/**
 * 二次贝塞尔曲线路径命令。
 */
public final class QuadraticCurveToCommand extends DrawCommand {

    /**
     * 控制点 X 坐标，单位：像素
     */
    private final double cpx;

    /**
     * 控制点 Y 坐标，单位：像素
     */
    private final double cpy;

    /**
     * 终点 X 坐标，单位：像素
     */
    private final double x;

    /**
     * 终点 Y 坐标，单位：像素
     */
    private final double y;

    /**
     * 构造二次贝塞尔曲线命令。
     *
     * @param cpx 控制点 X 坐标，单位：像素
     * @param cpy 控制点 Y 坐标，单位：像素
     * @param x   终点 X 坐标，单位：像素
     * @param y   终点 Y 坐标，单位：像素
     */
    public QuadraticCurveToCommand(double cpx, double cpy, double x, double y) {
        this.cpx = cpx;
        this.cpy = cpy;
        this.x = x;
        this.y = y;
    }

    /**
     * 获取控制点 X 坐标。
     *
     * @return 控制点 X 坐标，单位：像素
     */
    public double getCpx() {
        return cpx;
    }

    /**
     * 获取控制点 Y 坐标。
     *
     * @return 控制点 Y 坐标，单位：像素
     */
    public double getCpy() {
        return cpy;
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
     * @return 控制点与终点坐标数组，单位：像素
     */
    @Override
    public double[][] getBounds() {
        return new double[][]{{cpx, cpy}, {x, y}};
    }

    @Override
    public String toString() {
        return "QuadraticCurveTo(" + cpx + ", " + cpy + ", " + x + ", " + y + ")";
    }
}
