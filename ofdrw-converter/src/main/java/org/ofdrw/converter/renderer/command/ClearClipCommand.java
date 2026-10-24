package org.ofdrw.converter.renderer.command;

/**
 * 清除当前裁剪区域的命令。
 */
public final class ClearClipCommand extends DrawCommand {

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
        return "ClearClip";
    }
}
