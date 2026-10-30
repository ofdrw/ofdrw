package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.DashPattern;
import org.ofdrw.converter.pdf.model.LineCap;
import org.ofdrw.converter.pdf.model.LineJoin;

/**
 * 有效绘制参数：一次 {@code Relative} 继承链解析后的完整结果
 * <p>
 * 字段全部为“已填默认值”的最终值，渲染器据此一次性生成
 * {@link org.ofdrw.converter.pdf.model.StrokeStyle} 与 {@link org.ofdrw.converter.pdf.model.SetAlpha}，
 * 不依赖 PDF 图形状态的残留值（设计约束 1）。
 * <p>
 * 未解析颜色：OFD 颜色可能引用渐变、底纹等复杂色对象，由 P3 阶段（颜色/渐变/底纹）负责，
 * 因此本类只保留<b>引用</b>而不解释。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class EffectiveDrawParam {

    /**
     * OFD 线宽默认值，单位 mm（8.2 表 22，0.353mm = 1pt）
     */
    public static final double DEFAULT_LINE_WIDTH_MM = 0.353;

    /**
     * OFD 斜接限制默认值（8.2 表 22）
     */
    public static final double DEFAULT_MITER_LIMIT = 3.528;

    /**
     * OFD 图元透明度默认值（决策 2：缺省不透明）
     */
    public static final int DEFAULT_ALPHA = 255;

    /**
     * 线宽，单位 mm
     */
    private final double lineWidthMm;

    /**
     * 线端样式
     */
    private final LineCap lineCap;

    /**
     * 线连接样式
     */
    private final LineJoin lineJoin;

    /**
     * 斜接限制
     */
    private final double miterLimit;

    /**
     * 虚线样式，单位 mm
     */
    private final DashPattern dashPattern;

    /**
     * 图元透明度，取值 0~255
     */
    private final int alpha;

    /**
     * 填充颜色对象（可为路径颜色或渐变/底纹），{@code null} 表示未定义
     */
    private final Object fillColor;

    /**
     * 描边颜色对象，{@code null} 表示未定义
     */
    private final Object strokeColor;

    /**
     * 构造有效绘制参数
     *
     * @param lineWidthMm 线宽，单位 mm
     * @param lineCap     线端样式，不能为空
     * @param lineJoin    线连接样式，不能为空
     * @param miterLimit  斜接限制
     * @param dashPattern 虚线样式，单位 mm，不能为空
     * @param alpha       图元透明度，取值 0~255
     * @param fillColor   填充颜色对象，可为空
     * @param strokeColor 描边颜色对象，可为空
     */
    EffectiveDrawParam(double lineWidthMm, LineCap lineCap, LineJoin lineJoin, double miterLimit,
                       DashPattern dashPattern, int alpha, Object fillColor, Object strokeColor) {
        this.lineWidthMm = lineWidthMm;
        this.lineCap = lineCap;
        this.lineJoin = lineJoin;
        this.miterLimit = miterLimit;
        this.dashPattern = dashPattern;
        this.alpha = alpha;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
    }

    /**
     * @return OFD 规范默认绘制参数（线宽 0.353mm、平头、尖角、斜接 3.528、实线、不透明、无颜色）
     */
    public static EffectiveDrawParam defaults() {
        return new EffectiveDrawParam(DEFAULT_LINE_WIDTH_MM, LineCap.BUTT, LineJoin.MITER,
                DEFAULT_MITER_LIMIT, DashPattern.SOLID, DEFAULT_ALPHA, null, null);
    }

    /**
     * @return 线宽，单位 mm
     */
    public double getLineWidthMm() {
        return lineWidthMm;
    }

    /**
     * @return 线端样式
     */
    public LineCap getLineCap() {
        return lineCap;
    }

    /**
     * @return 线连接样式
     */
    public LineJoin getLineJoin() {
        return lineJoin;
    }

    /**
     * @return 斜接限制
     */
    public double getMiterLimit() {
        return miterLimit;
    }

    /**
     * @return 虚线样式，单位 mm
     */
    public DashPattern getDashPattern() {
        return dashPattern;
    }

    /**
     * @return 图元透明度，取值 0~255
     */
    public int getAlpha() {
        return alpha;
    }

    /**
     * @return 填充颜色对象，可为空
     */
    public Object getFillColor() {
        return fillColor;
    }

    /**
     * @return 描边颜色对象，可为空
     */
    public Object getStrokeColor() {
        return strokeColor;
    }

    /**
     * 以另一组参数覆盖本参数中“已定义”的字段
     * <p>
     * 用于实现 OFD 的“就近原则”：调用方先取得基准（父级 DrawParam 链），再用更近一层（图层/图元）的
     * 已定义字段覆盖；{@code null} 表示该字段在本层未定义，不参与覆盖。
     *
     * @param lineWidthMm                             线宽，单位 mm；为空不覆盖
     * @param lineCap                                 线端样式；为空不覆盖
     * @param lineJoin                                线连接样式；为空不覆盖
     * @param miterLimit                              斜接限制；为空不覆盖
     * @param dashPattern                             虚线样式，单位 mm；为空不覆盖
     * @param alpha                                   透明度 0~255；为空不覆盖
     * @param fillColor                               填充颜色对象；为空不覆盖
     * @param strokeColor                             描边颜色对象；为空不覆盖
     * @return 叠加后的新参数
     */
    EffectiveDrawParam override(Double lineWidthMm, LineCap lineCap, LineJoin lineJoin,
                                Double miterLimit, DashPattern dashPattern, Integer alpha,
                                Object fillColor, Object strokeColor) {
        return new EffectiveDrawParam(
                lineWidthMm == null ? this.lineWidthMm : lineWidthMm,
                lineCap == null ? this.lineCap : lineCap,
                lineJoin == null ? this.lineJoin : lineJoin,
                miterLimit == null ? this.miterLimit : miterLimit,
                dashPattern == null ? this.dashPattern : dashPattern,
                alpha == null ? this.alpha : alpha,
                fillColor == null ? this.fillColor : fillColor,
                strokeColor == null ? this.strokeColor : strokeColor);
    }

    @Override
    public String toString() {
        return "EffectiveDrawParam(w=" + lineWidthMm + "mm, " + lineCap + ", " + lineJoin + ", miter=" + miterLimit
                + ", " + dashPattern + ", alpha=" + alpha + ")";
    }
}
