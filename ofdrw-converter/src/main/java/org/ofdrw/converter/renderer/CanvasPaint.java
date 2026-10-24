package org.ofdrw.converter.renderer;

import java.awt.Color;
import java.awt.LinearGradientPaint;
import java.awt.Paint;
import java.awt.RadialGradientPaint;
import java.awt.TexturePaint;

/**
 * 画布着色源：纯色或渐变。
 * <p>
 * 不可变值对象，用于描述不依赖 {@link java.awt.Paint} 的着色语义，
 * 后端实现可据此构造自身的绘制对象，或直接还原为 AWT {@link Paint}。
 */
public final class CanvasPaint {

    /**
     * 纯色
     */
    private final Color color;

    /**
     * 线性渐变
     */
    private final LinearGradientPaint linearGradient;

    /**
     * 径向渐变
     */
    private final RadialGradientPaint radialGradient;

    /**
     * 底纹（平铺纹理）
     */
    private final TexturePaint texture;

    /**
     * 构造纯色着色源。
     *
     * @param color 颜色，不能为空
     */
    public CanvasPaint(Color color) {
        if (color == null) {
            throw new IllegalArgumentException("颜色不能为空");
        }
        this.color = color;
        this.linearGradient = null;
        this.radialGradient = null;
        this.texture = null;
    }

    /**
     * 构造线性渐变着色源。
     *
     * @param gradient 线性渐变，不能为空
     */
    public CanvasPaint(LinearGradientPaint gradient) {
        if (gradient == null) {
            throw new IllegalArgumentException("线性渐变不能为空");
        }
        this.color = null;
        this.linearGradient = gradient;
        this.radialGradient = null;
        this.texture = null;
    }

    /**
     * 构造径向渐变着色源。
     *
     * @param gradient 径向渐变，不能为空
     */
    public CanvasPaint(RadialGradientPaint gradient) {
        if (gradient == null) {
            throw new IllegalArgumentException("径向渐变不能为空");
        }
        this.color = null;
        this.linearGradient = null;
        this.radialGradient = gradient;
        this.texture = null;
    }

    /**
     * 构造底纹（平铺纹理）着色源。
     *
     * @param texture 平铺纹理，不能为空
     */
    public CanvasPaint(TexturePaint texture) {
        if (texture == null) {
            throw new IllegalArgumentException("平铺纹理不能为空");
        }
        this.color = null;
        this.linearGradient = null;
        this.radialGradient = null;
        this.texture = texture;
    }

    /**
     * 获取底纹（平铺纹理）。
     *
     * @return 平铺纹理，非底纹时为 null
     */
    public TexturePaint getTexture() {
        return texture;
    }

    /**
     * 判断是否为纯色。
     *
     * @return true 表示纯色，false 表示渐变
     */
    public boolean isSolidColor() {
        return color != null;
    }

    /**
     * 获取纯色。
     *
     * @return 纯色，非纯色时为 null
     */
    public Color getColor() {
        return color;
    }

    /**
     * 获取线性渐变。
     *
     * @return 线性渐变，非线性渐变时为 null
     */
    public LinearGradientPaint getLinearGradient() {
        return linearGradient;
    }

    /**
     * 获取径向渐变。
     *
     * @return 径向渐变，非径向渐变时为 null
     */
    public RadialGradientPaint getRadialGradient() {
        return radialGradient;
    }

    /**
     * 还原为 AWT 着色对象。
     *
     * @return AWT {@link Paint} 实例
     */
    public Paint toPaint() {
        if (color != null) {
            return color;
        }
        if (linearGradient != null) {
            return linearGradient;
        }
        return radialGradient != null ? radialGradient : texture;
    }

    /**
     * 构造纯色着色源的便捷方法。
     *
     * @param color 颜色
     * @return 着色源
     */
    public static CanvasPaint of(Color color) {
        return new CanvasPaint(color);
    }

    @Override
    public String toString() {
        if (color != null) {
            return "CanvasPaint{color=" + color + "}";
        }
        return linearGradient != null ? "CanvasPaint{linearGradient}" : "CanvasPaint{radialGradient}";
    }
}
