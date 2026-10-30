package org.ofdrw.converter.pdf.model;

/**
 * 路径填充与描边指令：对应 PDF 的 {@code f}、{@code f*}、{@code S}、{@code B}、{@code B*}
 * <p>
 * 路径几何本身以 {@link PathSegment} 序列提供，{@link PathCommand} 承载完整的一次“路径构造 + 绘制”。
 * 若既要填充又要描边，不允许拆成两条 {@link PathCommand}（路径会被绘制两次），必须通过 {@link #isFill()} 与
 * {@link #isStroke()} 同时为 true 表达，由写入器选择 {@code B} 或 {@code B*}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PathCommand extends AbstractCommand {

    /**
     * 子路径序列
     */
    private final java.util.List<PathSegment> segments;

    /**
     * 是否填充
     */
    private final boolean fill;

    /**
     * 是否描边
     */
    private final boolean stroke;

    /**
     * 填充规则
     */
    private final FillRule fillRule;

    /**
     * 填充颜色，不填充时为空
     */
    private final PdfColor fillColor;

    /**
     * 描边颜色，不描边时为空
     */
    private final PdfColor strokeColor;

    /**
     * 构造路径指令
     *
     * @param segments    子路径序列，不能为空；首元素必须是 {@link MoveTo}
     * @param fill        是否填充
     * @param stroke      是否描边
     * @param fillRule    填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param fillColor   填充颜色，填充时不能为空
     * @param strokeColor 描边颜色，描边时不能为空
     */
    public PathCommand(java.util.List<PathSegment> segments, boolean fill, boolean stroke,
                       FillRule fillRule, PdfColor fillColor, PdfColor strokeColor) {
        this(CoordSpace.PAGE, segments, fill, stroke, fillRule, fillColor, strokeColor);
    }

    /**
     * 构造路径指令（指定坐标作用域）
     *
     * @param coordSpace  坐标作用域
     * @param segments    子路径序列，不能为空；首元素必须是 {@link MoveTo}
     * @param fill        是否填充
     * @param stroke      是否描边
     * @param fillRule    填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param fillColor   填充颜色，填充时不能为空
     * @param strokeColor 描边颜色，描边时不能为空
     */
    public PathCommand(CoordSpace coordSpace, java.util.List<PathSegment> segments, boolean fill, boolean stroke,
                       FillRule fillRule, PdfColor fillColor, PdfColor strokeColor) {
        super(coordSpace);
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("路径不能为空");
        }
        if (!(segments.get(0) instanceof MoveTo)) {
            throw new IllegalArgumentException("路径必须以 MoveTo 开始");
        }
        if (!fill && !stroke) {
            throw new IllegalArgumentException("路径必须至少执行填充或描边之一");
        }
        if (fill && fillColor == null) {
            throw new IllegalArgumentException("填充路径必须指定填充颜色");
        }
        if (stroke && strokeColor == null) {
            throw new IllegalArgumentException("描边路径必须指定描边颜色");
        }
        this.segments = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(segments));
        this.fill = fill;
        this.stroke = stroke;
        this.fillRule = fillRule == null ? FillRule.NON_ZERO : fillRule;
        this.fillColor = fillColor;
        this.strokeColor = strokeColor;
    }

    /**
     * @return 子路径序列（只读）
     */
    public java.util.List<PathSegment> getSegments() {
        return segments;
    }

    /**
     * @return 是否填充
     */
    public boolean isFill() {
        return fill;
    }

    /**
     * @return 是否描边
     */
    public boolean isStroke() {
        return stroke;
    }

    /**
     * @return 填充规则
     */
    public FillRule getFillRule() {
        return fillRule;
    }

    /**
     * @return 填充颜色，不填充时为空
     */
    public PdfColor getFillColor() {
        return fillColor;
    }

    /**
     * @return 描边颜色，不描边时为空
     */
    public PdfColor getStrokeColor() {
        return strokeColor;
    }

    @Override
    public String toString() {
        return "Path(" + segments.size() + " segments, fill=" + fill + ", stroke=" + stroke + ")";
    }
}
