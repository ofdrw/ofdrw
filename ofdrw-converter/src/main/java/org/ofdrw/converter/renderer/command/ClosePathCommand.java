package org.ofdrw.converter.renderer.command;

/**
 * 闭合当前子路径的命令。
 */
public final class ClosePathCommand extends DrawCommand {

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 空数组，闭合命令不产生新坐标
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "ClosePath";
    }
}
