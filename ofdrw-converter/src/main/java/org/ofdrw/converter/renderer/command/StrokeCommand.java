package org.ofdrw.converter.renderer.command;

import org.ofdrw.converter.renderer.DrawStyle;
import org.ofdrw.converter.renderer.ResolvedPaint;

/**
 * 描边命令：用当前路径与着色源描边。
 */
public final class StrokeCommand extends DrawCommand {

    /**
     * 绘制样式
     */
    private final DrawStyle style;

    /**
     * 描边着色源
     */
    private final ResolvedPaint paint;

    /**
     * 构造描边命令。
     *
     * @param style 绘制样式，不能为空
     * @param paint 描边着色源，不能为空
     */
    public StrokeCommand(DrawStyle style, ResolvedPaint paint) {
        if (style == null || paint == null) {
            throw new IllegalArgumentException("绘制样式与着色源均不能为空");
        }
        this.style = style;
        this.paint = paint;
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
     * @return 空数组，描边区域由路径命令决定
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Stroke(" + style + ", " + paint + ")";
    }
}
