package org.ofdrw.layout.edit;

import org.ofdrw.core.basicType.STBase;
import org.ofdrw.layout.element.canvas.DrawContext;
import org.ofdrw.layout.element.canvas.Drawer;
import org.ofdrw.layout.element.canvas.TextMetrics;

import java.io.IOException;

/**
 * @author bhzhange
 * @date 2024/4/18 13:18
 */
public class WatermarkDrawer implements Drawer {
    
    private boolean DEBUG = false;
    
    /**
     * 单元格文字内容
     */
    private String value;
    
    
    private Annotation annotation;
    
    /**
     * 文字颜色
     * <p>
     * 支持16进制颜色值：#000000
     * <p>
     * RGB：rgb(0,0,0)
     * <p>
     * RGBA：rgba(0,0,0,1)
     * <p>
     * 默认：#000000 （黑色）
     */
    private String color = "#000000";
    
    /**
     * 字体名称
     * <p>
     * 默认：宋体
     */
    private String fontName = "宋体";
    
    /**
     * 字号
     * <p>
     * 默认：3 （单位：毫米）
     */
    private double fontSize = 4.5;
    
    /**
     * 是否加粗
     */
    private Boolean bold = false;
    
    /**
     * 字体宽度
     */
    private String fontWeight = "normal";
    
    /**
     * 是否斜体
     */
    private Boolean italic = false;
    
    /**
     * 文字之间的间距
     */
    private Double letterSpacing = 0d;
    
    /**
     * 透明度
     */
    private Double globalAlpha = 0.5d;
    
    /**
     * 旋转角度，0-360, 默认330（-30）
     */
    private Double angle = 330d;
    
    /**
     * 水印横向间距, 单位mm
     */
    private Double intervalX = 30d;
    
    /**
     * 水印纵向间距, 单位mm
     */
    private Double intervalY = 30d;
    
    /**
     * 初始化一个水印绘制器
     */
    public WatermarkDrawer() {
    
    }
    
    /**
     * 绘制水印
     * @param ctx 绘制上下文
     * @throws IOException
     */
    @Override
    public void draw(DrawContext ctx) throws IOException {
        if(this.value == null || this.value.isEmpty()){
            return;
        }
        if(this.annotation == null){
            return;
        }
        ctx.fillStyle = this.color;
        ctx.font = getFont();
        if (this.letterSpacing != 0) {
            // 设置字间距
            ctx.getFont().setLetterSpacing(this.letterSpacing);
        }
        // 设置字体颜色
        if (this.color != null && !this.color.isEmpty()) {
            ctx.fillStyle = this.color;
        }
        
        double width = annotation.getBoundary().getWidth();
        double height = annotation.getBoundary().getHeight();
       
        /**
         * 计算水印实际占位横向宽度和竖向高度，并自适应计算水印的起始坐标.
         */
        TextMetrics metrics = ctx.measureText(this.value);
     
        double x = annotation.getBoundary().getTopLeftX();
        while(x < width){
            double y = annotation.getBoundary().getTopLeftY();
            while(y < height){
                ctx.save();
                ctx.setGlobalAlpha(this.globalAlpha);
                ctx.translate(x, y);
                ctx.rotate(this.angle);
                ctx.fillText(this.value, 0, metrics.fontSize);
                ctx.restore();
                if(isDEBUG()){
                    debugBorder(ctx, metrics, x, y);
                }
                y += intervalY;
            }
            x += intervalX;
        }
    }
    
    private String getFont(){
      
        // 设置字体样式
        String fontStr = "";
        if (italic) {
            fontStr += "italic ";
        }
        if (bold) {
            fontStr += "bold ";
        }else if(fontWeight != null && !fontWeight.isEmpty()){
            fontStr += fontWeight + " ";
        }
        fontStr = fontStr + STBase.fmt(fontSize) + "mm " + fontName;
        
        return fontStr;
    }
    
    /**
     * 水印文字内容
     */
    public String getValue() {
        return value;
    }
    
    /**
     * 设置水印文字内容
     */
    public void setValue(String value) {
        this.value = value;
    }
    
    /**
     * 获取文字颜色
     */
    public String getColor() {
        return color;
    }
    
    /**
     * 设置文字颜色
     */
    public void setColor(String color) {
        this.color = color;
    }
    
    /**
     * 获取字体名称
     */
    public String getFontName() {
        return fontName;
    }
    
    /**
     * 设置字体名称
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }
    
    /**
     * 获取字号
     */
    public double getFontSize() {
        return fontSize;
    }
    
    /**
     * 设置字号
     */
    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }
    
    /**
     * 是否加粗
     */
    public Boolean getBold() {
        return bold;
    }
    
    /**
     * 设置是否加粗
     */
    public void setBold(Boolean bold) {
        this.bold = bold;
    }
    
    /**
     * 获取字体宽度
     */
    public String getFontWeight() {
        return fontWeight;
    }
    
    /**
     * 设置字体宽度
     */
    public void setFontWeight(String fontWeight) {
        this.fontWeight = fontWeight;
    }
    
    /**
     * 是否斜体
     */
    public Boolean getItalic() {
        return italic;
    }
    
    /**
     * 设置是否斜体
     */
    public void setItalic(Boolean italic) {
        this.italic = italic;
    }
    
    /**
     * 获取文字之间的间距
     */
    public Double getLetterSpacing() {
        return letterSpacing;
    }
    
    /**
     * 设置文字之间的间距
     */
    public void setLetterSpacing(Double letterSpacing) {
        this.letterSpacing = letterSpacing;
    }
    
    /**
     * 获取透明度
     */
    public Double getGlobalAlpha() {
        return globalAlpha;
    }
    
    /**
     * 设置透明度
     */
    public void setGlobalAlpha(Double globalAlpha) {
        if(globalAlpha > 1 || globalAlpha < 0){
            throw new IllegalArgumentException("透明度超出范围0.0~1.0");
        }
        this.globalAlpha = globalAlpha;
    }
    
    /**
     * 获取旋转角度
     */
    public Double getAngle() {
        return angle;
    }
    
    /**
     * 设置旋转角度，基于坐标系(0,1)方向，向第4象限偏移的角度。
     * 与数学意义上的弧度角度存在
     * @param angle 旋转角度，0-360, 默认330（-30）
     */
    public void setAngle(Double angle) {
        if(angle > 360 || angle < -360){
            throw new IllegalArgumentException("旋转角度超出范围");
        }
        if(angle < 0){
            angle = 360 + angle;
        }
        this.angle = angle;
    }
    
    /**
     * 获取横向间隔
     * @return 横向间隔，单位mm
     */
    public Double getIntervalX() {
        return intervalX;
    }
    
    /**
     * 设置水印横向间距
     * @param intervalX 横向间距，单位mm
     */
    public void setIntervalX(Double intervalX) {
        this.intervalX = intervalX;
    }
    
    /**
     * 获取水印纵向间距，单位mm
     * @return 水印纵向间距，单位mm
     */
    public Double getIntervalY() {
        return intervalY;
    }
    
    /**
     * 设置水印纵向间距
     * @param intervalY 纵向间距，单位mm
     */
    public void setIntervalY(Double intervalY) {
        this.intervalY = intervalY;
    }
    
    /**
     * 是否调试
     */
    public boolean isDEBUG() {
        return DEBUG;
    }
    
    /**
     * 设置调试状态
     */
    public void setDEBUG(boolean DEBUG) {
        this.DEBUG = DEBUG;
    }
    
    /**
     * 水印注释信息
     */
    public Annotation getAnnotation() {
        return annotation;
    }
    
    /**
     * 设置水印注释信息
     * @param annotation 水印注释对象，{@link Annotation} 的实现实例
     */
    public void setAnnotation(Annotation annotation) {
        this.annotation = annotation;
    }
    
    /**
     * 绘制辅助线
     *
     * @param ctx 绘制上下文
     */
    private void debugBorder(DrawContext ctx, TextMetrics metrics, double offsetX, double offsetY) {
        double lineWidth = 0.353d;
        double width = metrics.width + lineWidth * 2;
        double height = metrics.fontSize + lineWidth * 2;
        
        ctx.save();
        ctx.translate(offsetX, offsetY);
        ctx.setLineDash(1.5d, 1.5d);
        ctx.setLineWidth(lineWidth);
        ctx.setGlobalAlpha(0.53);
        ctx.strokeStyle = "rgb(255,0,0)";
        ctx.moveTo(0, 0);
        ctx.lineTo(width, height);
        ctx.moveTo(width, 0);
        ctx.lineTo(0, height);
        ctx.rotate(angle);
        ctx.rect(offsetX, offsetY, width, height);
        ctx.stroke();
        ctx.restore();
    }
    
}
