package org.ofdrw.converter.renderer;

/**
 * 已解析的绘制样式：描边参数与透明度组合。
 * <p>
 * 不可变值对象，用于把 OFD 的绘图参数（DrawParam）解析结果一次性传给渲染器。
 */
public final class ResolvedStyle {

    /**
     * 描边样式
     */
    private final DrawStyle strokeStyle;

    /**
     * 整体不透明度，取值 0.0–1.0
     */
    private final float opacity;

    /**
     * 构造已解析的绘制样式。
     *
     * @param strokeStyle 描边样式，不能为空
     * @param opacity     整体不透明度，取值 0.0–1.0，越界时截断到边界值
     */
    public ResolvedStyle(DrawStyle strokeStyle, float opacity) {
        if (strokeStyle == null) {
            throw new IllegalArgumentException("描边样式不能为空");
        }
        this.strokeStyle = strokeStyle;
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    /**
     * 获取描边样式。
     *
     * @return 描边样式
     */
    public DrawStyle getStrokeStyle() {
        return strokeStyle;
    }

    /**
     * 获取整体不透明度。
     *
     * @return 不透明度，取值 0.0–1.0，无量纲
     */
    public float getOpacity() {
        return opacity;
    }

    @Override
    public String toString() {
        return "ResolvedStyle{" + strokeStyle + ", opacity=" + opacity + "}";
    }
}
