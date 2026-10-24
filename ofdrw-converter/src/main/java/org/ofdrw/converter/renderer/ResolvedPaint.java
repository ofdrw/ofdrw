package org.ofdrw.converter.renderer;

/**
 * 已解析的着色源：在 {@link CanvasPaint} 基础上附加整体透明度。
 * <p>
 * 不可变值对象；透明度用于表达 OFD 颜色、绘图参数中的 Alpha 值。
 */
public final class ResolvedPaint {

    /**
     * 着色源
     */
    private final CanvasPaint paint;

    /**
     * 整体不透明度，取值 0.0–1.0，1.0 表示完全 opaque
     */
    private final float opacity;

    /**
     * 构造已解析的着色源。
     *
     * @param paint   着色源，不能为空
     * @param opacity 整体不透明度，取值 0.0–1.0，越界时截断到边界值
     */
    public ResolvedPaint(CanvasPaint paint, float opacity) {
        if (paint == null) {
            throw new IllegalArgumentException("着色源不能为空");
        }
        this.paint = paint;
        this.opacity = Math.max(0f, Math.min(1f, opacity));
    }

    /**
     * 获取着色源。
     *
     * @return 着色源
     */
    public CanvasPaint getPaint() {
        return paint;
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
        return "ResolvedPaint{" + paint + ", opacity=" + opacity + "}";
    }
}
