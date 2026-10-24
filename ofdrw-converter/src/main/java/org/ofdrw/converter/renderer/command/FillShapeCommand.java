package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.DrawStyle;
import org.ofdrw.converter.renderer.FillRule;
import org.ofdrw.converter.renderer.ResolvedPaint;

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * 用任意形状填充的命令。
 * <p>
 * 命令持有形状的 {@link Path2D} 副本，保证录制后回放结果稳定，不受调用方后续修改原始形状影响。
 */
public final class FillShapeCommand extends DrawCommand {

    /**
     * 填充形状副本
     */
    private final Path2D shape;

    /**
     * 绘制样式
     */
    private final DrawStyle style;

    /**
     * 填充着色源
     */
    private final ResolvedPaint paint;

    /**
     * 填充规则
     */
    private final FillRule rule;

    /**
     * 构造按形状填充的命令。
     *
     * @param shape 待填充形状，不能为空；坐标单位：像素
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则，不能为空
     */
    public FillShapeCommand(Shape shape, DrawStyle style, ResolvedPaint paint, FillRule rule) {
        if (shape == null || style == null || paint == null || rule == null) {
            throw new IllegalArgumentException("形状、绘制样式、着色源与填充规则均不能为空");
        }
        this.shape = new Path2D.Double(shape);
        this.style = style;
        this.paint = paint;
        this.rule = rule;
    }

    /**
     * 获取填充形状副本。
     *
     * @return 填充形状副本，坐标单位：像素
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
     * 获取填充着色源。
     *
     * @return 填充着色源
     */
    public ResolvedPaint getPaint() {
        return paint;
    }

    /**
     * 获取填充规则。
     *
     * @return 填充规则
     */
    public FillRule getRule() {
        return rule;
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
        return "FillShape(" + rule + ", " + paint + ")";
    }
}
