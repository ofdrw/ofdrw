package org.ofdrw.converter.pdf.render;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.PdfPageContent;

/**
 * 底纹单元格的渲染上下文：把“页面高度”替换为单元格高度
 * <p>
 * {@code CellContent} 使用独立坐标系（原点在单元格左上角，X 向右、Y 向下，单位 mm），
 * 但渲染器统一按“OFD 页面坐标 → PDF 坐标”做 Y 翻转，翻转量取的是上下文中的页面高度。
 * 如果直接沿用页面上下文，单元格里的图元会按整页高度翻转，被推到格子之外。
 * <p>
 * 本类只改写 {@link #getPageHeightMm()}，其余行为与原上下文一致。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class RegionShift implements RenderContext {

    /**
     * 被包装的页面级上下文
     */
    private final RenderContext delegate;

    /**
     * 单元格高度，单位 mm
     */
    private final double heightMm;

    /**
     * 构造单元格上下文
     *
     * @param delegate 页面级上下文，不能为空
     * @param heightMm 单元格高度，单位 mm
     */
    RegionShift(RenderContext delegate, double heightMm) {
        this.delegate = delegate;
        this.heightMm = heightMm;
    }

    @Override
    public double getPageWidthMm() {
        return delegate.getPageWidthMm();
    }

    @Override
    public double getPageHeightMm() {
        return heightMm;
    }

    @Override
    public double getPageX0Mm() {
        return 0;
    }

    @Override
    public double getPageY0Mm() {
        return 0;
    }

    @Override
    public AffineMatrix getCurrentCtm() {
        return delegate.getCurrentCtm();
    }

    @Override
    public RenderContext withCtm(AffineMatrix ctm) {
        return new RegionShift(delegate.withCtm(ctm), heightMm);
    }

    @Override
    public PdfPageContent getPageContent() {
        return delegate.getPageContent();
    }

    @Override
    public org.ofdrw.converter.pdf.font.FontSelector getFontSelector() {
        return delegate.getFontSelector();
    }
}
