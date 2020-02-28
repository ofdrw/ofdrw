package org.ofdrw.layout.element;

import org.ofdrw.layout.Rectangle;

import java.util.LinkedList;
import java.util.List;

/**
 * 段落
 *
 * @author 权观宇
 * @since 2020-02-03 01:27:20
 */
public class Paragraph extends Div {

    /**
     * 行间距
     */
    private Double lineSpace = 2d;

    /**
     * 默认字体
     */
    private Font defaultFont;

    /**
     * 默认字号
     */
    private Double defaultFontSize;

    /**
     * 文字内容
     */
    private LinkedList<Span> contents;


    public Paragraph() {
        this.setClear(Clear.both);
        this.contents = new LinkedList<>();
    }

    /**
     * @param text 文字内容
     */
    public Paragraph(String text) {
        if (text == null) {
            throw new IllegalArgumentException("文字内容为null");
        }
        this.add(text);
    }

    /**
     * 增加段落中的文字
     * <p>
     * 文字样式使用span默认字体样式
     *
     * @param text 文本内容
     * @return this
     */
    public Paragraph add(String text) {
        if (text == null) {
            return this;
        }
        Span newTxt = new Span(text);
        if (this.defaultFont != null) {
            newTxt.setFont(defaultFont);
        }
        if (this.defaultFontSize != null) {
            newTxt.setFontSize(defaultFontSize);
        }
        return this.add(newTxt);
    }

    /**
     * 加入带有特殊样式文字内容
     *
     * @param content 特殊样式文字内容
     * @return this
     */
    public Paragraph add(Span content) {
        if (content == null) {
            return this;
        }
        this.contents.add(content);
        return this;
    }

    public Double getLineSpace() {
        return lineSpace;
    }

    public Paragraph setLineSpace(Double lineSpace) {
        this.lineSpace = lineSpace;
        return this;
    }

    public Font getDefaultFont() {
        return defaultFont;
    }

    public Paragraph setDefaultFont(Font defaultFont) {
        this.defaultFont = defaultFont;
        return this;
    }

    public Double getFontSize() {
        return defaultFontSize;
    }

    public Paragraph setFontSize(Double defaultFontSize) {
        this.defaultFontSize = defaultFontSize;
        return this;
    }

    public LinkedList<Span> getContents() {
        return contents;
    }

    public Paragraph setContents(List<Span> contents) {
        this.contents.clear();
        this.contents.addAll(contents);
        return this;
    }

    /**
     * 获取尺寸
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    @Override
    public Rectangle reSize(Double widthLimit) {
        Double width = this.getWidth();
        if (widthLimit == null) {
            throw new NullPointerException("widthLimit为空");
        }
        if (width == null || (width > widthLimit)) {
            width = widthLimit;
        }
        double widthRemain = width;
        double height = 0;
        double lineHeight = 0;
        for (Span s : this.contents) {
            for (TxtGlyph txt : s.glyphList()) {
                if (txt.getW() > widthRemain) {
                    // 剩余空间不足则需要换行
                    height += lineHeight + this.lineSpace;
                    // 重置行元素
                    widthRemain = width;
                    lineHeight = 0;
                }
                widthRemain -= txt.getW();
                if (txt.getH() > lineHeight) {
                    // 如果高度比原来行高高那么更新行高
                    lineHeight = txt.getH();
                }
            }
        }
        if (widthRemain != width) {
            // 剩余最后一行加入到高度中
            height += lineHeight;
        }
        width += widthPlus();
        height += heightPlus();
        return new Rectangle(width, height);

    }
}
