package org.ofdrw.layout.element;

import org.ofdrw.layout.Rectangle;

import java.util.LinkedList;
import java.util.List;

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
    private Double letterSpacing = 0d;

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

    /**
     * 当渲染空间不足时可能会拆分元素
     * <p>
     * true为不拆分，false为拆分。默认值为false
     */
    private Boolean integrity = false;

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

    public Boolean getIntegrity() {
        return integrity;
    }

    public Span setIntegrity(Boolean integrity) {
        this.integrity = integrity;
        return this;
    }

    /**
     * 获取字体图形列表
     *
     * @return 字体图形列表
     */
    public List<TxtGlyph> glyphList() {
        LinkedList<TxtGlyph> res = new LinkedList<>();
        for (char c : this.text.toCharArray()) {
            res.add(new TxtGlyph(c, this));
        }
        return res;
    }

}
