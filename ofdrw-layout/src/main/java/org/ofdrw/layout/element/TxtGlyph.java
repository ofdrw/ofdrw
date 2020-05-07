package org.ofdrw.layout.element;

import org.ofdrw.font.Font;

/**
 * 文字字符块
 *
 * @author 权观宇
 * @since 2020-02-28 05:51:05
 */
public class TxtGlyph {
    /**
     * 文字
     */
    private char txt;
    /**
     * 文字字体信息
     */
    private TextFontInfo textFontInfo;

    public TxtGlyph(char txt, TextFontInfo span) {
        this.txt = txt;
        this.textFontInfo = span;
    }

    /**
     * @return 字符宽度(字符宽度 + 字间距)
     */
    public double getW() {
        Font font = textFontInfo.getFont();
        double w = textFontInfo.getLetterSpacing();
        w += textFontInfo.getFontSize() * font.getCharWidthScale(txt);
        return w;
    }

    /**
     * @return 字符高度
     */
    public double getH() {
        return textFontInfo.getFontSize();
    }

}
