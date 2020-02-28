package org.ofdrw.layout.element;

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
    private Span parent;

    public TxtGlyph(char txt, Span span) {
        this.txt = txt;
        this.parent = span;
    }

    /**
     * @return 字符宽度
     */
    public double getW() {
        double w = parent.getLetterSpacing();
        if ((txt > 'A' && txt < 'Z') || (txt > 'a' && txt < 'z')) {
            w += parent.getFontSize() / 2 + 1;
        } else {
            // 非英文字符
            w += parent.getFontSize();
        }
        return w;
    }

    /**
     * @return 字符高度
     */
    public double getH() {
        return parent.getFontSize();
    }

}
