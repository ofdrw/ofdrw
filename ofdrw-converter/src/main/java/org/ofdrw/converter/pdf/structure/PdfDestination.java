package org.ofdrw.converter.pdf.structure;

/**
 * PDF 跳转目标（库无关描述），对应 OFD 表 54 的目标区域
 * <p>
 * 决策 25：大纲与 Goto 动作<b>仅</b>映射标题与目标，不做样式。
 * <p>
 * <b>坐标语义</b>：字段一律以 <b>mm</b> 保存，且是 OFD 页面坐标语义（原点左上、Y 轴向下）；
 * {@code null} 表示“该维度不指定”（由阅读器按 Fit 语义自适应），与 PDF 对应参数写 {@code null} 一致。
 * 转换为 PDF 页面坐标（原点左下、Y 向上、单位 pt）需要<b>目标页</b>的页面高度，
 * 因此统一由 {@link #toPdfCoordinates(double, double)} 在拿到目标页页面框后完成，
 * 避免在此处猜测页面高度（决策 1：坐标换算集中，不在映射器里做）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfDestination {

    /**
     * PDF 目标类型，与 OFD {@code DestType} 一一对应
     */
    public enum Type {
        /**
         * 由左上角位置与缩放比例确定（OFD XYZ）
         */
        XYZ,
        /**
         * 适合整个窗口区域（OFD Fit）
         */
        FIT,
        /**
         * 适合窗口宽度，由顶部位置确定（OFD FitH）
         */
        FIT_H,
        /**
         * 适合窗口高度，由左侧位置确定（OFD FitV）
         */
        FIT_V,
        /**
         * 适合窗口内的目标矩形（OFD FitR）
         */
        FIT_R
    }

    /**
     * 目标类型
     */
    private final Type type;

    /**
     * 目标页页码，从 0 起
     */
    private final int pageIndex;

    /**
     * 左侧坐标，单位 pt；为目标页坐标
     */
    private final Double left;

    /**
     * 顶部坐标，单位 pt；为目标页坐标
     */
    private final Double top;

    /**
     * 右侧坐标，单位 pt；为目标页坐标
     */
    private final Double right;

    /**
     * 底部坐标，单位 pt；为目标页坐标
     */
    private final Double bottom;

    /**
     * 缩放比例，1 表示原始大小；为空表示不指定
     */
    private final Double zoom;

    /**
     * @param type      目标类型
     * @param pageIndex 目标页页码，从 0 起
     * @param left      左侧坐标（mm），可为空
     * @param top       顶部坐标（mm），可为空
     * @param right     右侧坐标（mm），可为空
     * @param bottom    底部坐标（mm），可为空
     * @param zoom      缩放比例，可为空
     */
    public PdfDestination(Type type, int pageIndex, Double left, Double top, Double right, Double bottom, Double zoom) {
        if (type == null) {
            throw new IllegalArgumentException("目标类型不能为空");
        }
        if (pageIndex < 0) {
            throw new IllegalArgumentException("目标页页码不能为负: " + pageIndex);
        }
        this.type = type;
        this.pageIndex = pageIndex;
        this.left = left;
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.zoom = zoom;
    }

    /**
     * 以 OFD 页面坐标（mm，原点左上、Y 向下）构造目标
     *
     * @param type      目标类型
     * @param pageIndex 目标页页码，从 0 起
     * @param left      左侧坐标，单位 mm，可为空
     * @param top       顶部坐标，单位 mm，可为空
     * @param right     右侧坐标，单位 mm，可为空
     * @param bottom    底部坐标，单位 mm，可为空
     * @param zoom      缩放比例，可为空
     * @return 目标
     */
    public static PdfDestination ofdMm(Type type, int pageIndex, Double left, Double top,
                                       Double right, Double bottom, Double zoom) {
        return new PdfDestination(type, pageIndex, left, top, right, bottom, zoom);
    }

    /**
     * 构造“适合整页”目标
     *
     * @param pageIndex 目标页页码，从 0 起
     * @return 目标
     */
    public static PdfDestination fit(int pageIndex) {
        return new PdfDestination(Type.FIT, pageIndex, null, null, null, null, null);
    }

    /**
     * @return 目标类型
     */
    public Type getType() {
        return type;
    }

    /**
     * @return 目标页页码，从 0 起
     */
    public int getPageIndex() {
        return pageIndex;
    }

    /**
     * @return 左侧坐标（pt），可为空
     */
    public Double getLeft() {
        return left;
    }

    /**
     * @return 顶部坐标（pt），可为空
     */
    public Double getTop() {
        return top;
    }

    /**
     * @return 右侧坐标（pt），可为空
     */
    public Double getRight() {
        return right;
    }

    /**
     * @return 底部坐标（pt），可为空
     */
    public Double getBottom() {
        return bottom;
    }

    /**
     * @return 缩放比例，可为空
     */
    public Double getZoom() {
        return zoom;
    }

    /**
     * 换算为 PDF 页面坐标（原点左下、Y 轴向上、单位 pt）
     * <p>
     * Y 翻转按目标页页面框进行：{@code y_pdf = (pageHeight + pageY0 - y_ofd) × 72/25.4}；
     * 目标指向目标页自身，因此页面框取<b>目标页</b>的 PhysicalBox（决策 26）。
     * 仅转换该目标类型实际使用的字段，其余字段一律返回 null（PDF 对应位置写 null）。
     *
     * @param pageY0Mm     目标页页面框左上角纵坐标，单位 mm
     * @param pageHeightMm 目标页页面框高度，单位 mm
     * @return 已换算为 pt 的目标
     */
    public PdfDestination toPdfCoordinates(double pageY0Mm, double pageHeightMm) {
        switch (type) {
            case XYZ:
                return new PdfDestination(Type.XYZ, pageIndex,
                        toPt(left), toPtY(top, pageY0Mm, pageHeightMm), null, null, zoom);
            case FIT:
                return new PdfDestination(Type.FIT, pageIndex, null, null, null, null, null);
            case FIT_H:
                return new PdfDestination(Type.FIT_H, pageIndex, null, toPtY(top, pageY0Mm, pageHeightMm), null, null, null);
            case FIT_V:
                return new PdfDestination(Type.FIT_V, pageIndex, toPt(left), null, null, null, null);
            case FIT_R:
                // PDF FitR 的矩形按 (左, 下, 右, 上) 给出，OFD 给出的是 (Left, Top, Right, Bottom)
                return new PdfDestination(Type.FIT_R, pageIndex,
                        toPt(left), toPtY(bottom, pageY0Mm, pageHeightMm),
                        toPt(right), toPtY(top, pageY0Mm, pageHeightMm), null);
            default:
                return this;
        }
    }

    /**
     * X 方向换算：mm → pt
     *
     * @param value 值，单位 mm
     * @return 值，单位 pt；入参为空返回 null
     */
    private static Double toPt(Double value) {
        return value == null ? null : org.ofdrw.converter.pdf.CoordSystem.mmToPt(value);
    }

    /**
     * Y 方向换算：OFD 页面坐标（mm，原点左上）→ PDF 页面坐标（pt，原点左下）
     * <p>
     * 先减去页面框左上角纵坐标换算到“页面内坐标”，再做 Y 翻转（复用 {@link CoordSystem#yToPt(double, double)}），
     * 与页面图元的换算口径一致（决策 26：页面框可以带有非零原点）。
     *
     * @param value        值，单位 mm，OFD 页面坐标
     * @param pageY0Mm     页面框左上角纵坐标，单位 mm
     * @param pageHeightMm 页面框高度，单位 mm
     * @return 值，单位 pt，PDF 页面坐标；入参为空返回 null
     */
    private static Double toPtY(Double value, double pageY0Mm, double pageHeightMm) {
        return value == null ? null : org.ofdrw.converter.pdf.CoordSystem.yToPt(value - pageY0Mm, pageHeightMm);
    }

    @Override
    public String toString() {
        return "PdfDestination{" + type + " page=" + pageIndex + ", left=" + left + ", top=" + top
                + ", right=" + right + ", bottom=" + bottom + ", zoom=" + zoom + '}';
    }
}
