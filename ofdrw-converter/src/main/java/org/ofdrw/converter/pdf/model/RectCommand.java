package org.ofdrw.converter.pdf.model;

/**
 * 矩形填充指令（快捷形式）
 * <p>
 * 用于图像边框以外的简单实心矩形，例如背景色块、模板底框。语义等价于一条只含 {@code m/l/l/l/h} 子路径的
 * {@link PathCommand}，但避免了调用方手写路径，同时便于写入器优化。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class RectCommand extends AbstractCommand {

    /**
     * 矩形区域，单位 pt（PDF 页面坐标，x/y 为左下角）
     */
    private final Box rect;

    /**
     * 填充颜色
     */
    private final PdfColor fillColor;

    /**
     * 构造矩形填充指令
     *
     * @param rect      矩形区域，单位 pt（PDF 页面坐标），不能为空
     * @param fillColor 填充颜色，不能为空
     */
    public RectCommand(Box rect, PdfColor fillColor) {
        super(CoordSpace.PAGE);
        if (rect == null) {
            throw new IllegalArgumentException("矩形区域不能为空");
        }
        if (fillColor == null) {
            throw new IllegalArgumentException("填充颜色不能为空");
        }
        this.rect = rect;
        this.fillColor = fillColor;
    }

    /**
     * @return 矩形区域，单位 pt（PDF 页面坐标）
     */
    public Box getRect() {
        return rect;
    }

    /**
     * @return 填充颜色
     */
    public PdfColor getFillColor() {
        return fillColor;
    }

    @Override
    public String toString() {
        return "Rect(" + rect + ", " + fillColor + ")";
    }
}
