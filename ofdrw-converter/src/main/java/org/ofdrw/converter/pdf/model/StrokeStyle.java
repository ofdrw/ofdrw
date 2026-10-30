package org.ofdrw.converter.pdf.model;

/**
 * 写入显式描边参数：对应 PDF 的 {@code w}、{@code J}、{@code j}、{@code M}、{@code d}
 * <p>
 * 设计约束 1 与 3.3 节：有效绘制参数必须先由 {@code GraphicsStateResolver} 计算完毕，再一次性写入，
 * 不依赖 PDF 状态机残留。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class StrokeStyle extends AbstractCommand {

    /**
     * 线宽，单位 pt
     */
    private final double lineWidthPt;

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
     * 虚线样式
     */
    private final DashPattern dashPattern;

    /**
     * 构造描边参数指令
     *
     * @param lineWidthPt 线宽，单位 pt，小于 0 按 0 处理
     * @param lineCap     线端样式，为空按 {@link LineCap#BUTT}
     * @param lineJoin    线连接样式，为空按 {@link LineJoin#MITER}
     * @param miterLimit  斜接限制，非正数按 10 处理（PDF 默认值）
     * @param dashPattern 虚线样式，为空按实线
     */
    public StrokeStyle(double lineWidthPt, LineCap lineCap, LineJoin lineJoin, double miterLimit, DashPattern dashPattern) {
        super(CoordSpace.PAGE);
        this.lineWidthPt = (lineWidthPt < 0 || Double.isNaN(lineWidthPt)) ? 0 : lineWidthPt;
        this.lineCap = lineCap == null ? LineCap.BUTT : lineCap;
        this.lineJoin = lineJoin == null ? LineJoin.MITER : lineJoin;
        this.miterLimit = (miterLimit <= 0 || Double.isNaN(miterLimit)) ? 10 : miterLimit;
        this.dashPattern = dashPattern == null ? DashPattern.SOLID : dashPattern;
    }

    /**
     * @return 线宽，单位 pt
     */
    public double getLineWidthPt() {
        return lineWidthPt;
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
     * @return 虚线样式
     */
    public DashPattern getDashPattern() {
        return dashPattern;
    }

    @Override
    public String toString() {
        return "StrokeStyle(w=" + lineWidthPt + ", " + lineCap + ", " + lineJoin + ", " + miterLimit + ", " + dashPattern + ")";
    }
}
