package org.ofdrw.converter.renderer.command;

/**
 * 无状态绘制命令基类。
 * <p>
 * Java 8 没有 sealed 与模式匹配，故以抽象基类 + 具体子类表达命令联合类型，
 * 具体子类位于本包内（{@code SaveCommand}、{@code FillCommand} 等）。
 * 命令只记录参数，不持有渲染器状态，可重复回放。
 */
public abstract class DrawCommand {

    /**
     * 空坐标数组，表示该命令不产生包围盒贡献。
     */
    protected static final double[][] NO_BOUNDS = new double[0][];

    /**
     * 获取命令对包围盒的坐标贡献。
     * <p>
     * 每个元素是 {x, y} 坐标对（单位：像素），无法确定时返回空数组。
     *
     * @return 坐标点数组
     */
    public abstract double[][] getBounds();
}
