package org.ofdrw.layout.element.canvas;

import org.ofdrw.layout.element.Position;

public class Line extends Canvas{
    
    private double[] beginPoint;
    
    private double[] endPoint;
    
    private String lineColor;
    
    private double lineWidth;
    
    /**
     * 线对象透明度
     */
    private double lineOpacity = 1.0d;
    
    /**
     * 线对象绘制器
     */
    private Drawer drawer = ctx -> {
        if(lineColor == null){
            lineColor = "#000000";
        }
        if(lineWidth == 0.0d){
            // 默认 1pt
            lineWidth = 0.353d;
        }
        if(beginPoint == null || endPoint.length != 2){
            throw new IllegalArgumentException("beginPoint is null or error");
        }
        if(endPoint == null || endPoint.length != 2){
            throw new IllegalArgumentException("endPoint is null or error");
        }
        ctx.save();
        ctx.setLineWidth(this.lineWidth);
        ctx.beginPath();
        ctx.setGlobalAlpha(lineOpacity);
        ctx.strokeStyle = lineColor;
        ctx.moveTo(beginPoint[0], beginPoint[1]);
        ctx.lineTo(endPoint[0] , endPoint[1]);
        ctx.stroke();
        ctx.restore();
    };
    
    /**
     * 线对象
     * @param width 画线区域的宽度
     * @param height 画线区域高度
     */
    public Line(Double width, Double height) {
        super(width, height);
        super.setBackgroundColor("#FFFFFF");
        super.setPosition(Position.Static);
        // 元素透明度
        super.setOpacity(0.0d);
        super.setDrawer(this.drawer);
    }
    
    /**
     * 线对象
     * @param x 画线区域左上角的x坐标
     * @param y 画线区域左上角的y坐标
     * @param w 画线区域的宽度
     * @param h 画线区域高度
     */
    public Line(double x, double y, double w, double h) {
        super(x, y, w, h);
        super.setBackgroundColor("#FFFFFF");
        // 元素透明度
        super.setOpacity(0.0d);
        super.setPosition(Position.Static);
        super.setDrawer(this.drawer);
    }
    
    /**
     * 获取线的起始点
     * @return 线的起始点坐标
     */
    public double[] getBeginPoint() {
        return beginPoint;
    }
    
    /**
     * 设置线的起始点
     * @param beginPoint 线的起始点在画布上的坐标
     * @return this
     */
    public Line setBeginPoint(double[] beginPoint) {
        this.beginPoint = beginPoint;
        return this;
    }
    
    /**
     * 设置线的起始点
     * @param beginX 线的起始点在画布上的x坐标
     * @param beginY 线的起始点在画布上的y坐标
     * @return
     */
    public Line setBeginPoint(double beginX, double beginY) {
        this.beginPoint = new double[]{beginX, beginY};
        return this;
    }
    
    /**
     * 获取线的结束点
     * @return 线的结束点坐标
     */
    public double[] getEndPoint() {
        return endPoint;
    }
    
    /**
     * 设置线的结束点
     * @param endPoint 线的结束点在画布上的坐标
     * @return this
     */
    public Line setEndPoint(double[] endPoint) {
        this.endPoint = endPoint;
        return this;
    }
    
    /**
     * 设置线的结束点
     * @param endX 线的结束点在画布上的x坐标
     * @param endY 线的结束点在画布上的y坐标
     * @return this
     */
    public Line setEndPoint(double endX, double endY) {
        this.endPoint = new double[]{endX, endY};
        return this;
    }
    
    /**
     * 获取线的颜色
     * @return 线的颜色
     */
    public String getLineColor() {
        return lineColor;
    }
    
    /**
     * 设置线的颜色,格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     * @param lineColor 线的颜色
     */
    public Line setLineColor(String lineColor) {
        this.lineColor = lineColor;
        return this;
    }
    
    /**
     * 获取线的宽度
     * @return 线的宽度
     */
    public double getLineWidth() {
        return lineWidth;
    }
    
    /**
     * 设置线的宽度
     * @param lineWidth 线的宽度
     */
    public Line setLineWidth(double lineWidth) {
        this.lineWidth = lineWidth;
        return this;
    }
    
    /**
     * 获取画线区域的绘制器
     * @return 画线区域的绘制器
     */
    @Override
    public Drawer getDrawer() {
        return drawer;
    }
    
    /**
     * 设置画线区域的绘制器
     * @param drawer 新的绘制器
     */
    @Override
    public Line setDrawer(Drawer drawer) {
        this.drawer = drawer;
        return this;
    }
    
    /**
     * 获取线的透明度
     * @return 线的透明度
     */
    public double getLineOpacity() {
        return lineOpacity;
    }
    
    /**
     * 设置线的透明度
     * @param lineOpacity 线的透明度，范围0-1
     * @return this
     */
    public Line setLineOpacity(double lineOpacity) {
        this.lineOpacity = lineOpacity;
        return this;
    }
}
