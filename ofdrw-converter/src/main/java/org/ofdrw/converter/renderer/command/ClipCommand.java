package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.FillRule;

/**
 * 裁剪命令：用当前路径与裁剪区域求交。
 */
public final class ClipCommand extends DrawCommand {

    /**
     * 填充规则
     */
    private final FillRule rule;

    /**
     * 构造裁剪命令。
     *
     * @param rule 填充规则，不能为空
     */
    public ClipCommand(FillRule rule) {
        if (rule == null) {
            throw new IllegalArgumentException("填充规则不能为空");
        }
        this.rule = rule;
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
     * @return 空数组，裁剪区域由路径命令决定
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Clip(" + rule + ")";
    }
}
