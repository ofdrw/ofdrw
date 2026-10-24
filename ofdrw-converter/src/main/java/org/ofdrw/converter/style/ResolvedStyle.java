package org.ofdrw.converter.style;

/**
 * 已结算的最终绘制样式，不可变值对象。
 * <p>
 * 只包含渲染所需字段：线型、颜色与透明度，不再持有资源引用或继承关系。
 * 长度字段（线宽、虚线）单位为毫米（mm），{@link #getAlpha()} 为 0..1 的已合并透明度。
 */
public final class ResolvedStyle {

    /**
     * 线宽，单位：毫米（mm）
     */
    private final double lineWidth;

    /**
     * 线端点样式
     */
    private final LineCapStyle lineCap;

    /**
     * 线条连接样式
     */
    private final LineJoinStyle lineJoin;

    /**
     * 尖角截断值（Join 为 Miter 时有效），无单位
     */
    private final double miterLimit;

    /**
     * 虚线起始偏移，单位：毫米（mm）
     */
    private final double dashOffset;

    /**
     * 虚线重复样式（线段长度、间隔长度），单位：毫米（mm）；空数组表示实线
     */
    private final double[] dashPattern;

    /**
     * 填充颜色，null 表示无填充色
     */
    private final Integer fillColor;

    /**
     * 描边颜色，null 表示无描边色
     */
    private final Integer strokeColor;

    /**
     * 是否填充
     */
    private final boolean fill;

    /**
     * 是否描边
     */
    private final boolean stroke;

    /**
     * 透明度，取值 0..1
     */
    private final double alpha;

    /**
     * 创建已结算样式。
     *
     * @param lineWidth   线宽，单位：毫米（mm）
     * @param lineCap     线端点样式
     * @param lineJoin    线条连接样式
     * @param miterLimit  尖角截断值，无单位
     * @param dashOffset  虚线起始偏移，单位：毫米（mm）
     * @param dashPattern 虚线重复样式，单位：毫米（mm），可为 null 表示实线
     * @param fillColor   填充颜色（sRGB，0xRRGGBB），null 表示无
     * @param strokeColor 描边颜色（sRGB，0xRRGGBB），null 表示无
     * @param fill        是否填充
     * @param stroke      是否描边
     * @param alpha       透明度，取值 0..1
     */
    public ResolvedStyle(double lineWidth, LineCapStyle lineCap, LineJoinStyle lineJoin, double miterLimit,
                         double dashOffset, double[] dashPattern, Integer fillColor, Integer strokeColor,
                         boolean fill, boolean stroke, double alpha) {
        this.lineWidth = lineWidth;
        this.lineCap = lineCap == null ? LineCapStyle.BUTT : lineCap;
        this.lineJoin = lineJoin == null ? LineJoinStyle.MITER : lineJoin;
        this.miterLimit = miterLimit;
        this.dashOffset = dashOffset;
        this.dashPattern = dashPattern == null ? new double[0] : dashPattern.clone();
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
        this.fill = fill;
        this.stroke = stroke;
        this.alpha = Math.max(0, Math.min(1, alpha));
    }

    /**
     * 获取线宽。
     *
     * @return 线宽，单位：毫米（mm）
     */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * 获取线端点样式。
     *
     * @return 线端点样式
     */
    public LineCapStyle getLineCap() {
        return lineCap;
    }

    /**
     * 获取线条连接样式。
     *
     * @return 线条连接样式
     */
    public LineJoinStyle getLineJoin() {
        return lineJoin;
    }

    /**
     * 获取尖角截断值。
     *
     * @return 尖角截断值，无单位
     */
    public double getMiterLimit() {
        return miterLimit;
    }

    /**
     * 获取虚线起始偏移。
     *
     * @return 虚线起始偏移，单位：毫米（mm）
     */
    public double getDashOffset() {
        return dashOffset;
    }

    /**
     * 获取虚线重复样式。
     *
     * @return 虚线重复样式副本，单位：毫米（mm）；空数组表示实线
     */
    public double[] getDashPattern() {
        return dashPattern.clone();
    }

    /**
     * 获取填充颜色。
     *
     * @return 填充颜色（sRGB，0xRRGGBB），null 表示无填充色
     */
    public Integer getFillColor() {
        return fillColor;
    }

    /**
     * 获取描边颜色。
     *
     * @return 描边颜色（sRGB，0xRRGGBB），null 表示无描边色
     */
    public Integer getStrokeColor() {
        return strokeColor;
    }

    /**
     * 是否填充。
     *
     * @return true 表示填充
     */
    public boolean isFill() {
        return fill;
    }

    /**
     * 是否描边。
     *
     * @return true 表示描边
     */
    public boolean isStroke() {
        return stroke;
    }

    /**
     * 获取透明度。
     *
     * @return 透明度，取值 0..1
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * 基于当前样式派生新样式；未指定的字段沿用当前值。
     * <p>
     * 派生过程始终返回新实例，原样式保持不变。
     *
     * @param lineWidth   线宽，单位：毫米（mm），null 表示不变
     * @param lineCap     线端点样式，null 表示不变
     * @param lineJoin    线条连接样式，null 表示不变
     * @param miterLimit  尖角截断值，无单位，null 表示不变
     * @param dashOffset  虚线起始偏移，单位：毫米（mm），null 表示不变
     * @param dashPattern 虚线重复样式，单位：毫米（mm），null 表示不变
     * @param fillColor   填充颜色（sRGB，0xRRGGBB），null 表示不变
     * @param strokeColor 描边颜色（sRGB，0xRRGGBB），null 表示不变
     * @param fill        是否填充，null 表示不变
     * @param stroke      是否描边，null 表示不变
     * @param alpha       透明度，取值 0..1，null 表示不变
     * @return 派生出的新样式
     */
    public ResolvedStyle derive(Double lineWidth, LineCapStyle lineCap, LineJoinStyle lineJoin, Double miterLimit,
                                Double dashOffset, double[] dashPattern, Integer fillColor, Integer strokeColor,
                                Boolean fill, Boolean stroke, Double alpha) {
        return new ResolvedStyle(
                lineWidth == null ? this.lineWidth : lineWidth,
                lineCap == null ? this.lineCap : lineCap,
                lineJoin == null ? this.lineJoin : lineJoin,
                miterLimit == null ? this.miterLimit : miterLimit,
                dashOffset == null ? this.dashOffset : dashOffset,
                dashPattern == null ? this.dashPattern : dashPattern,
                fillColor == null ? this.fillColor : fillColor,
                strokeColor == null ? this.strokeColor : strokeColor,
                fill == null ? this.fill : fill,
                stroke == null ? this.stroke : stroke,
                alpha == null ? this.alpha : alpha);
    }
}
