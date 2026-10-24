package org.ofdrw.converter.renderer;

import java.awt.BasicStroke;

/**
 * 描边与填充的基础样式。
 * <p>
 * 不可变值对象；路径的填充色、描边色由 {@link ResolvedPaint} 单独传入，
 * 本类型只描述与颜色无关的线宽、端点、连接与虚线。
 */
public final class DrawStyle {

    /**
     * 线宽，单位：像素
     */
    private final float lineWidth;

    /**
     * 虚线序列，单位：像素；null 或空表示实线
     */
    private final float[] dashArray;

    /**
     * 虚线相位，单位：像素
     */
    private final float dashPhase;

    /**
     * 线段端点样式
     */
    private final int cap;

    /**
     * 线段连接样式
     */
    private final int join;

    /**
     * 斜接限制
     */
    private final float miterLimit;

    /**
     * 构造描边与填充样式。
     *
     * @param lineWidth  线宽，单位：像素，必须大于 0
     * @param dashArray  虚线序列，单位：像素，null 或空表示实线
     * @param dashPhase  虚线相位，单位：像素
     * @param cap        线段端点样式，取 {@link BasicStroke#CAP_BUTT}、{@link BasicStroke#CAP_ROUND}、{@link BasicStroke#CAP_SQUARE}
     * @param join       线段连接样式，取 {@link BasicStroke#JOIN_MITER}、{@link BasicStroke#JOIN_ROUND}、{@link BasicStroke#JOIN_BEVEL}
     * @param miterLimit 斜接限制，必须大于等于 1
     */
    public DrawStyle(float lineWidth, float[] dashArray, float dashPhase, int cap, int join, float miterLimit) {
        if (lineWidth <= 0) {
            throw new IllegalArgumentException("线宽必须大于 0，单位：像素");
        }
        this.lineWidth = lineWidth;
        this.dashArray = dashArray == null || dashArray.length == 0 ? null : dashArray.clone();
        this.dashPhase = dashPhase;
        this.cap = cap;
        this.join = join;
        this.miterLimit = miterLimit;
    }

    /**
     * 获取默认样式：线宽 1 像素的实线，平头端点、尖角连接。
     *
     * @return 默认样式
     */
    public static DrawStyle defaultStyle() {
        return new DrawStyle(1f, null, 0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f);
    }

    /**
     * 以当前样式为基础，设置新线宽。
     *
     * @param width 线宽，单位：像素
     * @return 新的样式对象
     */
    public DrawStyle withLineWidth(float width) {
        return new DrawStyle(width, dashArray, dashPhase, cap, join, miterLimit);
    }

    /**
     * 获取线宽。
     *
     * @return 线宽，单位：像素
     */
    public float getLineWidth() {
        return lineWidth;
    }

    /**
     * 获取虚线序列。
     *
     * @return 虚线序列（副本），实线时为 null
     */
    public float[] getDashArray() {
        return dashArray == null ? null : dashArray.clone();
    }

    /**
     * 获取虚线相位。
     *
     * @return 虚线相位，单位：像素
     */
    public float getDashPhase() {
        return dashPhase;
    }

    /**
     * 获取线段端点样式。
     *
     * @return {@link BasicStroke} 的端点常量
     */
    public int getCap() {
        return cap;
    }

    /**
     * 获取线段连接样式。
     *
     * @return {@link BasicStroke} 的连接常量
     */
    public int getJoin() {
        return join;
    }

    /**
     * 获取斜接限制。
     *
     * @return 斜接限制，无量纲
     */
    public float getMiterLimit() {
        return miterLimit;
    }

    /**
     * 还原为 AWT 描边对象。
     *
     * @return AWT {@link BasicStroke} 实例
     */
    public BasicStroke toStroke() {
        if (dashArray == null) {
            return new BasicStroke(lineWidth, cap, join, miterLimit);
        }
        return new BasicStroke(lineWidth, cap, join, miterLimit, dashArray, dashPhase);
    }

    @Override
    public String toString() {
        return "DrawStyle{lineWidth=" + lineWidth + ", dash=" + (dashArray == null ? "solid" : dashArray.length + "段") + "}";
    }
}
