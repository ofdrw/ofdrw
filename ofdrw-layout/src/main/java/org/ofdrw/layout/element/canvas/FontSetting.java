package org.ofdrw.layout.element.canvas;

import org.ofdrw.font.EnvFont;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.TextFontInfo;

/**
 * 字体设置
 *
 * @author 权观宇
 * @since 2020-05-06 18:21:02
 */
public class FontSetting implements Cloneable, TextFontInfo {

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
    private Integer fontWeight = 400;

    /**
     * 字间距
     */
    private double letterSpacing = 0d;

    /**
     * 字符方向
     * <p>
     * 指定了文字放置的方式（基线方向）
     */
    private int charDirection = 0;

    /**
     * 阅读方向
     * <p>
     * 指定了文字排列的方向
     */
    private int readDirection = 0;

    /**
     * 文本内容的当前对齐方式。
     */
    private TextAlign textAlign = TextAlign.start;

    /**
     * 简化构造提供默认的字体配置
     * <p>
     * 字体类型为宋体
     *
     * @return 字体配置
     */
    public static FontSetting getInstance() {
        return new FontSetting(5, FontName.SimSun.font());
    }

    /**
     * 简化构造提供可选的字体配置
     * <p>
     * 字体类型为宋体
     *
     * @param fontSize 字体大小，单位：毫米（mm）
     * @return 字体配置
     */
    public static FontSetting getInstance(double fontSize) {
        return new FontSetting(fontSize, FontName.SimSun.font());
    }

    public FontSetting(double fontSize, Font fontObj) {
        this.fontObj = fontObj;
        this.fontSize = fontSize;
    }

    private FontSetting() {
    }

    /**
     * 获取文本对齐方式
     *
     * @return 文本对齐方式
     */
    public TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * 设置文本对齐方式
     *
     * @param textAlign 文本对齐方式
     * @return this
     */
    public FontSetting setTextAlign(TextAlign textAlign) {
        if (textAlign == null) {
            return this;
        }
        this.textAlign = textAlign;
        return this;
    }

    /**
     * 获取文字对象
     *
     * @return 文字对象
     */
    @Override
    public Font getFont() {
        return fontObj;
    }

    /**
     * 设置文字对象
     *
     * @param fontObj 文字对象
     * @return this
     */
    public FontSetting setFont(Font fontObj) {
        this.fontObj = fontObj;
        return this;
    }


    /**
     * 获取文字字号
     *
     * @return 字号（单位毫米）
     */
    @Override
    public Double getFontSize() {
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
     * 获取字符方向
     *
     * @return 字符方向
     */
    public int getCharDirection() {
        return charDirection;
    }

    /**
     * 设置字符方向
     *
     * @param charDirection 字符方向(charDirection) 允许值：0、90、180、270
     * @return this
     */
    public FontSetting setCharDirection(int charDirection) {
        if (charDirection == 0 || charDirection == 90 || charDirection == 180 || charDirection == 270) {
            this.charDirection = charDirection;
        } else {
            throw new IllegalArgumentException("字符方向(charDirection) 允许值：0、90、180、270，错误值：" + charDirection);
        }
        return this;
    }

    /**
     * 获取阅读方向
     *
     * @return 阅读方向
     */
    public int getReadDirection() {
        return readDirection;
    }


    /**
     * 获取字间距
     *
     * @return 字间距，单位毫米mm，默认值0
     */
    @Override
    public Double getLetterSpacing() {
        return letterSpacing;
    }

    /**
     * 设置字间距
     * <p>
     * 如果字间距小于0，那么将会自动修正为0
     *
     * @param letterSpacing 字间距，单位毫米mm
     * @return this
     */
    public FontSetting setLetterSpacing(double letterSpacing) {
        if (letterSpacing < 0) {
            letterSpacing = 0;
        }
        this.letterSpacing = letterSpacing;
        return this;
    }

    /**
     * 设置阅读方向
     *
     * @param readDirection 设置阅读方向(charDirection) 允许值：0、90、180、270
     * @return this
     */
    public FontSetting setReadDirection(int readDirection) {
        if (readDirection == 0 || readDirection == 90 || readDirection == 180 || readDirection == 270) {
            this.readDirection = readDirection;
        } else {
            throw new IllegalArgumentException("阅读方向(readDirection) 允许值：0、90、180、270，错误值：" + readDirection);
        }
        return this;
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

    /**
     * 字符宽度
     *
     * @param c 字符
     * @return 宽度单位毫米
     */
    public Double charWidth(char c) {
        if (fontObj.hasWidthMath()){
            // 如果存在预设的字符映射表那么查表计算
            return fontObj.getCharWidthScale(c) * fontSize;
        }
        // 从环境变量中加载字体并计算字体边界大小
        return EnvFont.strBounds(fontObj.getName(), fontObj.getFamilyName(), String.valueOf(c), fontSize).getWidth();
    }


    @Override
    public FontSetting clone() {
        return new FontSetting()
                .setFont(this.fontObj)
                .setItalic(this.italic)
                .setFontSize(this.fontSize)
                .setFontWeight(this.fontWeight)
                .setCharDirection(this.charDirection)
                .setReadDirection(this.readDirection)
                .setTextAlign(this.textAlign);
    }
}
