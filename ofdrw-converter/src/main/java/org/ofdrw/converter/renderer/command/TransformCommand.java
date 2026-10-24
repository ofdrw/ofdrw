package org.ofdrw.converter.renderer.command;

import java.awt.geom.AffineTransform;

/**
 * 在当前变换之后叠加变换的命令。
 */
public final class TransformCommand extends DrawCommand {

    /**
     * 叠加的变换
     */
    private final AffineTransform transform;

    /**
     * 构造叠加变换命令。
     *
     * @param transform 叠加的变换，不能为空
     */
    public TransformCommand(AffineTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("变换不能为空");
        }
        this.transform = new AffineTransform(transform);
    }

    /**
     * 获取叠加的变换。
     *
     * @return 叠加的变换（副本）
     */
    public AffineTransform getTransform() {
        return new AffineTransform(transform);
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 空数组，变换命令不直接产生几何
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Transform" + transform;
    }
}
