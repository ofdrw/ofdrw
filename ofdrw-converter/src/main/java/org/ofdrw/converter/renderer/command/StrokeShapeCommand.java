package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.DrawStyle;
import org.ofdrw.converter.renderer.ResolvedPaint;

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * 用任意形状描边的命令。
 * <p>
 * 命令持有形状的 {@link Path2D} 副本，保证录制后回放结果稳定，不受调用方后续修改原始形状影响。
 */
public final class StrokeShapeCommand extends DrawCommand {

    /**
     * 描边形状副本
     */
    private final Path2D shape;

    /**
     * 绘制样式
     */
    private final DrawStyle style;

    /**
     * 描边着色源
     */
    private final ResolvedPaint paint;

    /**
     * 构造按形状描边的命令。
     *
     * @param shape 待描边形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    public StrokeShapeCommand(Shape shape, DrawStyle style, ResolvedPaint paint) {
        if (shape == null || style == null || paint == null) {
            throw new IllegalArgumentException("形状、绘制样式与着色源均不能为空");
        }
        this.shape = new Path2D.Double(shape);
        this.style = style;
        this.paint = paint;
    }

    /**
     * 获取描边形状副本。
     *
     * @return 描边形状副本，坐标单位：像素
     */
    public Shape getShape() {
        return new Path2D.Double(shape);
    }

    /**
     * 获取绘制样式。
     *
     * @return 绘制样式
     */
    public DrawStyle getStyle() {
        return style;
    }

    /**
     * 获取描边着色源。
     *
     * @return 描边着色源
     */
    public ResolvedPaint getPaint() {
        return paint;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 形状包围盒的两个对角点，坐标单位：像素
     */
    @Override
    public double[][] getBounds() {
        java.awt.geom.Rectangle2D bounds = shape.getBounds2D();
        return new double[][]{{bounds.getMinX(), bounds.getMinY()}, {bounds.getMaxX(), bounds.getMaxY()}};
    }

    @Override
    public String toString() {
        return "StrokeShape(" + style + ", " + paint + ")";
    }
}
