package org.ofdrw.layout.edit;

import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.PageLayout;

/**
 * @description 水印，是一种特殊的注释。
 * 水印只能在编辑的情况下被添加到文档中。
 * @author bhzhange
 * @date 2024/4/18 13:16
 */
public class Watermark extends Annotation {
    
    private WatermarkDrawer drawer;
    
    /**
     * 根据页面布局样式构建水印对象
     * @param style {@link PageLayout}文档页面布局样式
     */
    public Watermark(PageLayout style) {
        // 默认的，让绘制画布包裹页面，使得水印在可见区域得到比较美观的效果
        this(0, 0, style.getWidth() + 50, style.getHeight() + 50);
    }
    
    /**
     * 根据水印绘制区域坐标和大小构建水印对象
     * @param x 绘制区域左上角横坐标, 单位mm
     * @param y 绘制区域左上角纵坐标, 单位mm
     * @param width 绘制区域宽度, 单位mm
     * @param height 绘制区域高度, 单位mm
     */
    public Watermark(double x, double y,
                      double width, double height) {
        super(new ST_Box(x, y, width, height), AnnotType.Watermark, new WatermarkDrawer());
        this.drawer = (WatermarkDrawer) super.getDrawer();
        this.drawer.setAnnotation(this);
    }
    
    /**
     * 获取绘制器
     */
    @Override
    public WatermarkDrawer getDrawer() {
        return drawer;
    }
    
    /**
     * 设置水印绘制器
     * @param drawer 水印绘制器
     * @return
     */
    public Watermark setDrawer(WatermarkDrawer drawer) {
        this.drawer = drawer;
        return this;
    }
    
    /**
     * 设置文字水印内容
     * @param value 文字内容
     * @return this
     */
    public Watermark setValue(String value){
        this.drawer.setValue(value);
        return this;
    }
    
    /**
     * 获取水印文字颜色
     * @return 颜色
     */
    public String getColor() {
        return drawer.getColor();
    }
    
    /**
     * 设置水印文字颜色
     * @param color 颜色, 符合CSS3样式的颜色值被支持，可选值 颜色英文单词,例如red,blue等 | #RGB | #RRGGBB
     * @return this
     */
    public Watermark setColor(String color) {
        drawer.setColor(color);
        return this;
    }
    
    /**
     * 获取字体名称
     * @return 字体名称
     */
    public String getFontName() {
        return drawer.getFontName();
    }
    
    /**
     * 设置字体名称
     * @param fontName 字体名称
     * @return this
     */
    public Watermark setFontName(String fontName) {
        drawer.setFontName(fontName);
        return this;
    }
    
    /**
     * 获取字体大小
     * @return 字体大小,单位mm
     */
    public double getFontSize() {
        return drawer.getFontSize();
    }
    
    /**
     * 设置字体大小
     * @param fontSize 字体大小，单位mm
     * @return this
     */
    public Watermark setFontSize(double fontSize) {
        drawer.setFontSize(fontSize);
        return this;
    }
    
    /**
     * 获取文字是否粗体标识
     * @return  true 粗体， false-非粗体
     */
    public Boolean getBold() {
        return drawer.getBold();
    }
    
    /**
     * 是否粗体
     * @param bold true 粗体， false-非粗体
     * @return this
     */
    public Watermark setBold(Boolean bold) {
        drawer.setBold(bold);
        return this;
    }
    
    /**
     * 字体宽度，遵循 CSS3标准，可选值为 normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     * @return 字体宽度
     */
    public String getFontWeight() {
        return drawer.getFontWeight();
    }
    
    /**
     * 字体宽度
     * @param fontWeight 字体宽度，应遵循 CSS3标准，可选值为 normal | bold | bolder | lighter | 100 | 200 | 300 | 400 | 500 | 600 | 700 | 800 | 900
     */
    public Watermark setFontWeight(String fontWeight) {
        drawer.setFontWeight(fontWeight);
        return this;
    }
    
    /**
     * 获取文字是否斜体标识
     * @return true 斜体， false-非斜体
     */
    public Boolean getItalic() {
        return drawer.getItalic();
    }
    
    /**
     * 是否斜体
     * @param italic true 斜体， false-非斜体
     * @return this
     */
    public Watermark setItalic(Boolean italic) {
        drawer.setItalic(italic);
        return this;
    }
    
    /**
     * 获取文字字间距
     * @return 文字字间距
     */
    public Double getLetterSpacing() {
        return drawer.getLetterSpacing();
    }
    
    /**
     * 设置文字字间距
     * @param letterSpacing 文字字间距
     * @return this
     */
    public Watermark setLetterSpacing(Double letterSpacing) {
        drawer.setLetterSpacing(letterSpacing);
        return this;
    }
    
    /**
     * 水印透明度，可取值 0.0~1.0，默认 0.5
     */
    public Double getGlobalAlpha() {
        return drawer.getGlobalAlpha();
    }
    
    /**
     * 水印透明度，可取值 0.0~1.0，默认 0.5
     */
    public Watermark setGlobalAlpha(Double globalAlpha) {
        drawer.setGlobalAlpha(globalAlpha);
        return this;
    }
    
    /**
     * 旋转角度
     */
    public Double getAngle() {
        return drawer.getAngle();
    }
    
    /**
     * 设置旋转角度，表示基于坐标系(0,1)方向，向顺时针方向偏移的角度。
     * 与数学意义上的圆弧角度存在360度偏差，即 360-angle=数学弧度。
     * @param angle 旋转角度，0-360, 默认330(对应数学弧度30度)
     */
    public Watermark setAngle(Double angle) {
        drawer.setAngle(angle);
        return this;
    }
    
    /**
     * 获取水印文字内容
     * @return 文字内容
     */
    public String getValue() {
        return drawer.getValue();
    }
    
    /**
     * 获取水印横向间距，单位mm
     * @return 水印横向间距，单位mm
     */
    public Double getIntervalX() {
        return drawer.getIntervalX();
    }
    
    /**
     * 水印横向间距
     * @param intervalX 水印横向间距，单位mm
     * @return
     */
    public Watermark setIntervalX(Double intervalX) {
        drawer.setIntervalX(intervalX);
        return this;
    }
    
    /***
     * 获取水印纵向间距，单位mm
     * @return 水印纵向间距，单位mm
     */
    public Double getIntervalY() {
        return drawer.getIntervalY();
    }
    
    /***
     * 水印纵向间距
     * @param intervalY 水印纵向间距，单位mm
     * @return this
     */
    public Watermark setIntervalY(Double intervalY) {
        drawer.setIntervalY(intervalY);
        return this;
    }
    
    /**
     * 开启调试
     */
    public Watermark debug(){
        drawer.setDEBUG(true);
        return this;
    }
}
