package org.ofdrw.layout.areaholder;

import org.ofdrw.core.basicType.STBase;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.layout.element.canvas.DrawContext;
import org.ofdrw.layout.element.canvas.Drawer;
import org.ofdrw.layout.element.canvas.TextMetrics;

import java.io.IOException;

/**
 * 类单元格特殊Canvas
 * <p>
 * 可以实现类似于单元个效果，用于简化区域占位区块绘制。
 *
 * @author 权观宇
 * @since 2023-11-13 18:48:53
 */
public class CellBlock extends Canvas implements Drawer {

    /**
     * 单元格文字内容
     */
    private String value;

//    /**
//     * 当行空间不足时是否自动换行
//     * <p>
//     * 默认：自动换行
//     */
//    private Boolean breakLine = true;
//
//    /**
//     * 文档浮动方式
//     * <p>
//     * 默认：左浮动
//     */
//    private TextAlign align = TextAlign.left;
//
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
//
//    /**
//     * 背景颜色
//     * <p>
//     * 支持16进制颜色值：#000000
//     * <p>
//     * RGB：rgb(0,0,0)
//     * <p>
//     * RGBA：rgba(0,0,0,1)
//     * <p>
//     * 默认：为空表示透明
//     */
//    private String backgroundColor = "";
//
    /**
     * 字体名称
     * <p>
     * 默认：宋体
     */
    private String fontName = "SimSun";

    /**
     * 字号
     * <p>
     * 默认：0.353 （单位：毫米）
     */
    private double fontSize = 0.353;
//
//    /**
//     * 是否加粗
//     */
//    private Boolean blob = false;
//
//    /**
//     * 是否斜体
//     */
//    private Boolean italic = false;

    public CellBlock(double x, double y, double w, double h) {
        super(x, y, w, h);
        this.setDrawer(this);
    }


    /**
     * 单元格内部绘制
     *
     * @param ctx 绘制上下文
     * @throws IOException 图形绘制异常
     */
    @Override
    public void draw(DrawContext ctx) throws IOException {


        // 绘制文字
        if (this.value == null || this.value.isEmpty()) {
            return;
        }

        ctx.font = STBase.fmt(this.fontSize) + "mm " + this.fontName;
        ctx.fillStyle = this.color;

        Double[] padding = this.getPadding();
        TextMetrics textMetrics = ctx.measureText(this.value);
        double textWidth = textMetrics.width;
        double textHeight = textMetrics.fontSize;



//        ctx.fillText(text, 3, 7);
    }

    /**
     * 获取单元格文字内容
     *
     * @return 单元格文字内容
     */
    public String getValue() {
        return value;
    }

    /**
     * 设置单元格文字内容
     *
     * @param value 单元格文字内容
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * 获取单元格颜色
     *
     * @return 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     */
    public String getColor() {
        return color;
    }

    /**
     * 设置单元格颜色
     *
     * @param color 单元格颜色，格式：#000000、rgb(0,0,0)、rgba(0,0,0,1)
     */
    public void setColor(String color) {
        if (color == null || color.isEmpty()) {
            throw new IllegalArgumentException("颜色(color)不能为空");
        }
        this.color = color;
    }

    /**
     * 获取字体名称
     * @return 字体名称
     */
    public String getFontName() {
        return fontName;
    }

    /**
     * 设置字体名称
     *
     * @param fontName 字体名称，仅支持系统安装字体，且不会嵌入到OFD中。
     */
    public void setFontName(String fontName) {
        this.fontName = fontName;
    }

    /**
     * 获取字号
     * @return 字号，默认：0.353 （单位：毫米）
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * 设置字号
     * @param fontSize 字号，默认：0.353 （单位：毫米）
     */
    public void setFontSize(double fontSize) {
        if (fontSize <= 0) {
            throw new IllegalArgumentException("字号(fontSize)必须大于0");
        }
        this.fontSize = fontSize;
    }
}
