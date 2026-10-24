package org.ofdrw.converter.renderer.command;

/**
 * 乘以透明度的命令。
 */
public final class MultiplyAlphaCommand extends DrawCommand {

    /**
     * 透明度系数，取值 0.0–1.0，无量纲
     */
    private final float alpha;

    /**
     * 构造乘以透明度命令。
     *
     * @param alpha 透明度系数，取值 0.0–1.0，无量纲
     */
    public MultiplyAlphaCommand(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 获取透明度系数。
     *
     * @return 透明度系数，取值 0.0–1.0，无量纲
     */
    public float getAlpha() {
        return alpha;
    }

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
        return "MultiplyAlpha(" + alpha + ")";
    }
}
