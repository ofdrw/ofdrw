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
     * 父类元素
     */
    private Span parent;

    public TxtGlyph(char txt, Span span) {
        this.txt = txt;
        this.parent = span;
    }

    /**
     * @return 字符宽度(字符宽度 + 字间距)
     */
    public double getW() {
        Font font = parent.getFont();
        double w = parent.getLetterSpacing();
        w += parent.getFontSize() * font.getCharWidthScale(txt);
        return w;
    }

    /**
     * @return 字符高度
     */
    public double getH() {
        return parent.getFontSize();
    }

}
