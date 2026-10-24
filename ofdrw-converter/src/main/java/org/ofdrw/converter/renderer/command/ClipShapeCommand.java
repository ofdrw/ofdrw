package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.FillRule;

import java.awt.Shape;
import java.awt.geom.Path2D;

/**
 * 用任意形状裁剪的命令。
 * <p>
 * 命令持有形状的 {@link Path2D} 副本，保证录制后回放结果稳定，不受调用方后续修改原始形状影响。
 */
public final class ClipShapeCommand extends DrawCommand {

    /**
     * 裁剪形状副本
     */
    private final Path2D shape;

    /**
     * 填充规则
     */
    private final FillRule rule;

    /**
     * 构造按形状裁剪的命令。
     *
     * @param shape 裁剪形状，不能为空；坐标单位：像素
     * @param rule  填充规则，不能为空
     */
    public ClipShapeCommand(Shape shape, FillRule rule) {
        if (shape == null || rule == null) {
            throw new IllegalArgumentException("裁剪形状与填充规则均不能为空");
        }
        // 拷贝为 Path2D，避免录制后调用方修改原形状导致回放不可复现
        this.shape = new Path2D.Double(shape);
        this.rule = rule;
    }

    /**
     * 获取裁剪形状副本。
     *
     * @return 裁剪形状副本，坐标单位：像素
     */
    public Shape getShape() {
        return new Path2D.Double(shape);
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
        return "ClipShape(" + rule + ")";
    }
}
