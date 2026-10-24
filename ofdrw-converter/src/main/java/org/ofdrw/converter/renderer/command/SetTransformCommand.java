package org.ofdrw.converter.renderer.command;

import java.awt.geom.AffineTransform;

/**
 * 整体替换当前变换的命令。
 */
public final class SetTransformCommand extends DrawCommand {

    /**
     * 新变换
     */
    private final AffineTransform transform;

    /**
     * 构造替换变换命令。
     *
     * @param transform 新变换，不能为空
     */
    public SetTransformCommand(AffineTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("变换不能为空");
        }
        this.transform = new AffineTransform(transform);
    }

    /**
     * 获取新变换。
     *
     * @return 新变换（副本）
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
        return "SetTransform" + transform;
    }
}
