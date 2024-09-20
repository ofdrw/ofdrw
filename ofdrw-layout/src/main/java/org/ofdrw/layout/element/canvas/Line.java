package org.ofdrw.layout.element.canvas;

import java.io.IOException;

/**
 * OFDRW 线条元素，用于快速构建一条线条。
 * <p>
 * 若需要绘制复杂图形，请使用 {@link Canvas} 对象，并提供 {@link  Drawer}实现，在Drawer中使用绘制上下文绘制。
 * <p>
 * 若绘制简单矩形，可以使用 {@link org.ofdrw.layout.element.Div} 对象设置边框 实现。
 *
 * @author 张炳恒
 * @since 2024-5-27 18:50:52
 */
public class Line extends CanvasBase {

    /**
     * 线条起点坐标(x,y)
     */
    private double[] beginPoint = new double[]{0, 0};

    /**
     * 线条终点坐标(x,y)
     */
    private double[] endPoint = new double[]{0, 0};

    /**
     * 线条颜色
     * 支持格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)、black
     */
    private String lineColor = "#000000";

    /**
     * 线条宽度，默认 0.353mm
     */
    private double lineWidth = 0.353d;

    /**
     * 线对象透明度 [0,1]，默认为 1 不透明
     */
    private double lineOpacity = 1.0d;


    /**
     * 线对象
     *
     * @param width  画线区域的宽度，单位：毫米mm
     * @param height 画线区域高度，单位：毫米mm
     */
    public Line(Double width, Double height) {
        super(width, height);
    }

    /**
     * 线对象
     *
     * @param x 画线区域左上角的x坐标，单位：毫米mm
     * @param y 画线区域左上角的y坐标，单位：毫米mm
     * @param w 画线区域的宽度，单位：毫米mm
     * @param h 画线区域高度，单位：毫米mm
     */
    public Line(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    /**
     * 获取线的起始点
     *
     * @return 线的起始点坐标 [x, y]，可能为NULL。
     */
    public double[] getBeginPoint() {
        return beginPoint;
    }

    /**
     * 设置线的起始点
     *
     * @param beginPoint 线的起始点在画布上的坐标 [x, y]
     * @return this
     */
    public Line setBeginPoint(double[] beginPoint) {
        this.beginPoint = beginPoint;
        return this;
    }

    /**
     * 设置线的起始点
     *
     * @param beginX 线的起始点在画布上的x坐标，单位：毫米mm
     * @param beginY 线的起始点在画布上的y坐标，单位：毫米mm
     * @return this
     */
    public Line setBeginPoint(double beginX, double beginY) {
        this.beginPoint = new double[]{beginX, beginY};
        return this;
    }

    /**
     * 获取线的结束点
     *
     * @return 线的结束点坐标 [x, y]，可能为NULL。
     */
    public double[] getEndPoint() {
        return endPoint;
    }

    /**
     * 设置线的结束点
     *
     * @param endPoint 线的结束点在画布上的坐标 [x, y]
     * @return this
     */
    public Line setEndPoint(double[] endPoint) {
        this.endPoint = endPoint;
        return this;
    }

    /**
     * 设置线的结束点
     *
     * @param endX 线的结束点在画布上的x坐标，单位：毫米mm
     * @param endY 线的结束点在画布上的y坐标，单位：毫米mm
     * @return this
     */
    public Line setEndPoint(double endX, double endY) {
        this.endPoint = new double[]{endX, endY};
        return this;
    }

    /**
     * 获取线的颜色
     *
     * @return 线的颜色 格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)、black
     */
    public String getLineColor() {
        return lineColor;
    }

    /**
     * 设置线的颜色
     *
     * @param lineColor 线的颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)、black
     * @return this
     */
    public Line setLineColor(String lineColor) {
        this.lineColor = lineColor;
        return this;
    }

    /**
     * 获取线的宽度
     *
     * @return 线的宽度
     */
    public double getLineWidth() {
        return lineWidth;
    }

    /**
     * 设置线的宽度
     *
     * @param lineWidth 线的宽度，单位 毫米mm
     * @return this
     */
    public Line setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }

    /**
     * 获取线的透明度
     *
     * @return 线的透明度 [0,1]
     */
    public double getLineOpacity() {
        return lineOpacity;
    }

    /**
     * 设置线的透明度
     *
     * @param lineOpacity 线的透明度，范围 [0,1]
     * @return this
     */
    public Line setLineOpacity(double lineOpacity) {
        this.lineOpacity = lineOpacity;
        return this;
    }

    /**
     * 线条绘制器
     *
     * @param ctx 绘制上下文
     * @throws IOException 绘制异常
     */
    public void draw(DrawContext ctx) throws IOException {
        if (lineColor == null) {
            lineColor = "#000000";
        }
        if (lineWidth <= 0.0d) {
            // 线宽小于等于0则不绘制
            return;
        }
        if (beginPoint == null || endPoint.length < 2) {
            return;
        }
        if (endPoint == null || endPoint.length < 2) {
            return;
        }
        // 起点终点相同则不绘制
        if (beginPoint[0] == endPoint[0] && beginPoint[1] == endPoint[1]) {
            return;
        }

        ctx.save();
        try {
            ctx.setLineWidth(this.lineWidth);
            ctx.beginPath();
            ctx.setGlobalAlpha(lineOpacity);
            ctx.strokeStyle = lineColor;
            ctx.moveTo(beginPoint[0], beginPoint[1]);
            ctx.lineTo(endPoint[0], endPoint[1]);
            ctx.stroke();
        } finally {
            ctx.restore();
        }
    }
}
