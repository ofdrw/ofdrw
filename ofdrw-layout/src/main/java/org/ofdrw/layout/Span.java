package org.ofdrw.layout;

/**
 * 字体基础单元
 * <p>
 * 用来设置字体样式等
 *
 * @author 权观宇
 * @since 2020-02-03 02:01:53
 */
public class Span {

    /**
     * 字体
     */
    private Font font;

    /**
     * 字体大小
     */
    private Double fontSize;

    /**
     * 字间距
     * <p>
     * 默认为 0
     */
    private Double letterSpacing;

    /**
     * 是否加粗
     * <p>
     * 默认不加粗 false
     */
    private boolean bold = false;

    /**
     * 是否斜体
     * <p>
     * 默认非斜体 false
     */
    private boolean italic = false;

    /**
     * 是否含有下划线
     * <p>
     * 默认不含下划线
     */
    private boolean underline = false;

    /**
     * 文本内容
     */
    private String text;

    private Span() {
        this.setFont(Font.getDefault());
    }

    public Span(Font font, Double fontSize, String text) {
        this.font = font;
        this.fontSize = fontSize;
        this.text = text;
    }

    public Span(String text) {
        this();
        if (text == null) {
            throw new IllegalArgumentException("text内容为空");
        }
        this.text = text;
    }

    public Font getFont() {
        return font;
    }

    public Span setFont(Font font) {
        this.font = font;
        return this;
    }

    public Double getFontSize() {
        return fontSize;
    }

    public Span setFontSize(Double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Double getLetterSpacing() {
        return letterSpacing;
    }

    public Span setLetterSpacing(Double letterSpacing) {
        this.letterSpacing = letterSpacing;
        return this;
    }

    public boolean isBold() {
        return bold;
    }

    public Span setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public Span setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }

    public boolean isUnderline() {
        return underline;
    }

    public Span setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    public String getText() {
        return text;
    }

    public Span setText(String text) {
        this.text = text;
        return this;
    }
}
