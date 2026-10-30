package org.ofdrw.converter.pdf.model;

import org.ofdrw.converter.pdf.font.ImageEncoding;

/**
 * 图像边框指令
 * <p>
 * 设计第 2 节与 3.7 节：{@literal ImageObject} 的 Border（线宽、圆角、虚线、颜色）不能依赖图像 XObject 表达，
 * 必须额外绘制路径；因此边框独立为一条指令，由渲染器在图像指令之外生成。
 * <p>
 * 语义：边框以内接于图像外接矩形的方式绘制，圆角半径不超过短边的一半；坐标为 PDF 页面坐标（单位 pt）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageBorderCommand extends AbstractCommand {

    /**
     * 边框矩形，单位 pt（PDF 页面坐标）
     */
    private final Box rect;

    /**
     * 线宽，单位 pt
     */
    private final double lineWidthPt;

    /**
     * 圆角半径，单位 pt，0 表示直角
     */
    private final double cornerRadiusPt;

    /**
     * 边框颜色
     */
    private final PdfColor color;

    /**
     * 虚线样式
     */
    private final DashPattern dashPattern;

    /**
     * 边框使用的图像编码（可从边框指令追溯图像资源，便于调试）
     */
    private final ImageEncoding sourceEncoding;

    /**
     * 构造边框指令
     *
     * @param rect            边框矩形，单位 pt（PDF 页面坐标），不能为空
     * @param lineWidthPt     线宽，单位 pt，小于 0 按 0 处理
     * @param cornerRadiusPt  圆角半径，单位 pt，小于 0 按 0 处理
     * @param color           边框颜色，不能为空
     * @param dashPattern     虚线样式，为空按实线
     * @param sourceEncoding  边框对应的图像编码，可为空
     */
    public ImageBorderCommand(Box rect, double lineWidthPt, double cornerRadiusPt,
                              PdfColor color, DashPattern dashPattern, ImageEncoding sourceEncoding) {
        super(CoordSpace.PAGE);
        if (rect == null) {
            throw new IllegalArgumentException("边框矩形不能为空");
        }
        if (color == null) {
            throw new IllegalArgumentException("边框颜色不能为空");
        }
        this.rect = rect;
        this.lineWidthPt = lineWidthPt < 0 ? 0 : lineWidthPt;
        this.cornerRadiusPt = cornerRadiusPt < 0 ? 0 : cornerRadiusPt;
        this.color = color;
        this.dashPattern = dashPattern == null ? DashPattern.SOLID : dashPattern;
        this.sourceEncoding = sourceEncoding;
    }

    /**
     * @return 边框矩形，单位 pt（PDF 页面坐标）
     */
    public Box getRect() {
        return rect;
    }

    /**
     * @return 线宽，单位 pt
     */
    public double getLineWidthPt() {
        return lineWidthPt;
    }

    /**
     * @return 实际生效的圆角半径，单位 pt（不超过短边的一半）
     */
    public double getEffectiveCornerRadiusPt() {
        double max = Math.min(rect.getWidth(), rect.getHeight()) / 2;
        return Math.min(cornerRadiusPt, max);
    }

    /**
     * @return 边框颜色
     */
    public PdfColor getColor() {
        return color;
    }

    /**
     * @return 虚线样式
     */
    public DashPattern getDashPattern() {
        return dashPattern;
    }

    /**
     * @return 边框对应的图像编码，可为空
     */
    public ImageEncoding getSourceEncoding() {
        return sourceEncoding;
    }

    @Override
    public String toString() {
        return "ImageBorder(" + rect + ", w=" + lineWidthPt + ", r=" + cornerRadiusPt + ")";
    }
}
