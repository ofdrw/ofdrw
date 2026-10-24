package org.ofdrw.converter.renderer.command;

/**
 * 直接设定透明度的命令，与 {@link MultiplyAlphaCommand} 的相乘语义相区别。
 */
public final class SetAlphaCommand extends DrawCommand {

    /**
     * 不透明度，取值 0.0–1.0，无量纲
     */
    private final float alpha;

    /**
     * 构造设定透明度命令。
     *
     * @param alpha 不透明度，取值 0.0–1.0，无量纲
     */
    public SetAlphaCommand(float alpha) {
        this.alpha = alpha;
    }

    /**
     * 获取不透明度。
     *
     * @return 不透明度，取值 0.0–1.0，无量纲
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
        return "SetAlpha(" + alpha + ")";
    }
}
