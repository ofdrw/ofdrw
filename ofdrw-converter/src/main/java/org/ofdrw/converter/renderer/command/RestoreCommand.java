package org.ofdrw.converter.renderer.command;

/**
 * 恢复绘制状态命令。
 */
public final class RestoreCommand extends DrawCommand {

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 空数组，状态命令不产生坐标
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Restore";
    }
}
