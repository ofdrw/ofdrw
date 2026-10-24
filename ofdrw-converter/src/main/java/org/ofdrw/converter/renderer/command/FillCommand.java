package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.DrawStyle;
import org.ofdrw.converter.renderer.FillRule;
import org.ofdrw.converter.renderer.ResolvedPaint;

/**
 * 填充命令：用当前路径与着色源填充。
 */
public final class FillCommand extends DrawCommand {

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
     * 构造填充命令。
     *
     * @param style 绘制样式，不能为空
     * @param paint 填充着色源，不能为空
     * @param rule  填充规则，不能为空
     */
    public FillCommand(DrawStyle style, ResolvedPaint paint, FillRule rule) {
        if (style == null || paint == null || rule == null) {
            throw new IllegalArgumentException("绘制样式、着色源与填充规则均不能为空");
        }
        this.style = style;
        this.paint = paint;
        this.rule = rule;
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
     * @return 空数组，填充区域由路径命令决定
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Fill(" + rule + ", " + paint + ")";
    }
}
