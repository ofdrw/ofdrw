package org.ofdrw.layout.element.canvas;

import org.ofdrw.font.Font;

/**
 * 字体设置
 *
 * @author 权观宇
 * @since 2020-05-06 18:21:02
 */
public class FontSetting implements Cloneable {

    /**
     * 字体对象
     */
    private Font fontObj;

    /**
     * 规定字号 单位（毫米mm）
     */
    private double fontSize;

    /**
     * 斜体
     */
    private boolean italic = false;

    /**
     * 规定字体的粗细。
     * <p>
     * 可能值为： 100、200、300、400、500、600、700、800、900
     * <p>
     * 默认值： 400
     * <p>
     * 预设加粗为：800
     */
    private Integer fontWeight;

    public FontSetting(double fontSize, Font fontObj) {
        this.fontObj = fontObj;
        this.fontSize = fontSize;
    }

    private FontSetting() {
    }

    /**
     * 获取文字对象
     *
     * @return 文字对象
     */
    public Font getFontObj() {
        return fontObj;
    }

    /**
     * 设置文字对象
     *
     * @param fontObj 文字对象
     * @return this
     */
    public FontSetting setFontObj(Font fontObj) {
        this.fontObj = fontObj;
        return this;
    }


    /**
     * 获取文字字号
     *
     * @return 字号（单位毫米）
     */
    public double getFontSize() {
        return fontSize;
    }

    /**
     * 设置文字字号
     *
     * @param fontSize 字号（单位毫米）
     * @return this
     */
    public FontSetting setFontSize(double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    /**
     * 字体是否为斜体
     *
     * @return true - 斜体
     */
    public boolean isItalic() {
        return italic;
    }

    /**
     * 设置字体是否为斜体
     *
     * @param italic true - 斜体；false - 非斜体
     * @return this
     */
    public FontSetting setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    /**
     * 设置字体为加粗格式
     * <p>
     * 宽度：800
     *
     * @return this
     */
    public FontSetting setBold() {
        setFontWeight(800);
        return this;
    }

    /**
     * 获取字体宽度
     *
     * @return 字体宽度
     */
    public Integer getFontWeight() {
        return fontWeight;
    }

    /**
     * 设置字体宽度
     *
     * @param fontWeight 字体宽度，可选值：100、200、300、400、500、600、700、800、900
     * @return this
     */
    public FontSetting setFontWeight(Integer fontWeight) {
        switch (fontWeight) {
            case 100:
            case 200:
            case 300:
            case 400:
            case 500:
            case 600:
            case 700:
            case 800:
            case 900:
                this.fontWeight = fontWeight;
                break;
            default:
                throw new NumberFormatException("字体宽度(fontWeight)可选值： 100、200、300、400、500、600、700、800、900");
        }
        return this;
    }


    @Override
    public FontSetting clone()  {
        return new FontSetting()
                .setFontObj(this.fontObj)
                .setItalic(this.italic)
                .setFontSize(this.fontSize)
                .setFontWeight(this.fontWeight);
    }
}
