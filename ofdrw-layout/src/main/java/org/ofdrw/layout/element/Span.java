package org.ofdrw.layout.element;

import org.ofdrw.core.text.text.Weight;
import org.ofdrw.font.Font;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.canvas.NamedColor;

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
public class Span implements TextFontInfo {

    /**
     * 字体
     */
    private Font font;

    /**
     * 字体大小
     * 默认值3毫米
     */
    private Double fontSize = 3d;

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
     * <p>
     * 加粗等价于 {@link #setWeight(Weight)} 为 {@link Weight#W_800}
     */
    private boolean bold = false;

    /**
     * 文字粗细
     * <p>
     * 默认 {@link Weight#W_400}
     */
    private Weight weight = null;

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
     * 下划线与文字的偏移量
     */
    private double underlineOffset = 1.2d;
    /**
     * 下划线宽度，0表示保持默认，默认为字体大小的0.05倍
     */
    private double underlineWidth = 0d;


    /**
     * 是否填充
     * <p>
     * 默认值为：true
     */
    private boolean fill = true;

    /**
     * 文本内容
     */
    private String text;

    /**
     * 字体颜色
     */
    private int[] fillColor;

    /**
     * 是否占满剩下行空间
     */
    private boolean linebreak = false;

    /**
     * 当渲染空间不足时可能会拆分元素
     * <p>
     * true为不拆分，false为拆分。默认值为false
     */
    private Boolean integrity = false;

    LinkedList<TxtGlyph> txtGlyphsCache = null;


    protected Span() {
        this.setFont(Font.getDefault());
    }

    public Span(Font font, Double fontSize, String text) {
        this.font = font;
        this.fontSize = fontSize;
        setText(text);
    }

    public Span(String text) {
        this();
        if (text == null) {
            throw new IllegalArgumentException("text内容为空");
        }
        setText(text);
    }

    public int[] getColor() {
        return fillColor;
    }

    /**
     * 设置字体颜色
     *
     * @param rgb 颜色值
     * @return this
     */
    public Span setColor(int[] rgb) {
        this.fillColor = rgb;
        return this;
    }

    /**
     * 设置 文字颜色
     *
     * @param color 颜色值，可以是 16进制值 "#FFFFFF"、 16进制值缩写 "#FF"F、RGB "rgb(255,255,255)"、颜色名称 "white"
     * @return this
     */
    public Span setColor(String color) {
        int[] c = NamedColor.rgb(color);
        if (c == null || c.length < 3) {
            throw new IllegalArgumentException("边框颜色 颜色值错误：" + color);
        }
       return setColor(c);
    }

    /**
     * 设置字体颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 懒
     * @return this
     */
    public Span setColor(int r, int g, int b) {
        this.fillColor = new int[]{
                r,
                g,
                b
        };
        return this;
    }

    /**
     * @return 字符数量
     */
    public int length() {
        return text.length();
    }

    @Override
    public Font getFont() {
        return font;
    }

    public Span setFont(Font font) {
        this.font = font;
        return this;
    }

    @Override
    public Double getFontSize() {
        return fontSize;
    }

    public Span setFontSize(Double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    @Override
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

    /**
     * 设置是否加粗
     * <p>
     * 若需要更加细致的控制，可以使用 {@link #setWeight(Weight)}
     *
     * @param bold 是否加粗
     * @return this
     */
    public Span setBold(boolean bold) {
        this.bold = bold;
        return this;
    }

    /**
     * 获取字体粗细
     *
     * @return 字体粗细，可能为null。
     */
    public Weight getWeight() {
        return weight;
    }

    /**
     * 设置字体粗细
     *
     * @param weight 字体粗细
     * @return this
     */
    public Span setWeight(Weight weight) {
        this.weight = weight;
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

    /**
     * 设置 下划线
     *
     * @param underline 是否启用下划线
     * @return this
     */
    public Span setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }

    /**
     * 设置下划线
     *
     * @param underline 是否启用下划线
     * @param offset    下划线与文字的偏移量，可以为负值，默认值为1.2，单位毫米。
     * @param width     下划线线宽，默认为0，为0时默认为字体大小的0.05倍。
     * @return this
     */
    public Span setUnderline(boolean underline, double offset, double width) {
        this.underline = underline;
        this.underlineOffset = offset;
        this.underlineWidth = width;
        return this;
    }


    public Boolean isFill() {
        return fill;
    }

    public Span setFill(boolean fill) {
        this.fill = fill;
        return this;
    }

    public String getText() {
        return text;
    }

    public Span setText(String text) {
        this.text = text;
        if (txtGlyphsCache != null) {
            // 如果已经存在缓存，那么重新建立缓存
            glyphList();
        }
        return this;
    }

    /**
     * 元素是否可以拆分
     * <p>
     * 特殊的：
     * 如果没有或只有一个文字，那么无论如何设置integrity都为不可拆分
     *
     * @return true 可以拆分；false 不能拆分
     */
    public Boolean isIntegrity() {
        if (text == null || text.length() <= 1) {
            integrity = true;
        }
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
        if (txtGlyphsCache == null) {
            txtGlyphsCache = new LinkedList<>();
            for (char c : this.text.toCharArray()) {
                txtGlyphsCache.add(new TxtGlyph(c, this));
            }
        }
        return txtGlyphsCache;
    }

    /**
     * 获取字符X坐标偏移值队列
     * <p>
     * 队列中的每个值代表后一个文字与前一个文字之间在X方向上的偏移值
     *
     * @return 字符在X坐标偏移值队列
     */
    public Double[] getDeltaX() {
        List<TxtGlyph> list = glyphList();
        int len = list.size();
        if (len <= 1) {
            // 只有一个字符时，不存在字符偏移所以返还空数组
            return new Double[]{};
        }
        // 队列中的每个值代表后一个文字与前一个文字之间在X方向上的偏移值
        // 因此不要计算最后一个元素的偏移值，偏移量忽略最后一个字符的大小 len - 1
        Double[] res = new Double[len - 1];
        for (int i = 0; i < len - 1; i++) {
            res[i] = list.get(i).getW();
        }
        return res;
    }

    /**
     * @return 不拆分情况下都放在一行内占用的大小
     */
    public Rectangle blockSize() {
        List<TxtGlyph> txtGlyphs = glyphList();
        double width = 0;
        double height = 0;
        for (TxtGlyph glyph : txtGlyphs) {
            width += glyph.getW();
            if (glyph.getH() > height) {
                height = glyph.getH();
            }
        }
        return new Rectangle(width, height);
    }

    /**
     * 切分元素
     *
     * @param index 字符坐标
     * @return 切分后的两个全新元素
     */
    public Span[] split(int index) {
        if (index < 0 || index >= text.length()) {
            throw new IllegalArgumentException("非法的切分数组坐标(index): " + index);
        }
        Span s1 = this.clone().setText(this.text.substring(0, index));
        Span s2 = this.clone().setText(this.text.substring(index));
        return new Span[]{
                s1,
                s2
        };
    }

    /**
     * 设置Span为占满剩下行空间的元素
     * <p>
     * 等价于在字符串末尾增加\n，当字符串末尾存在\n 时该参数无效。
     *
     * @param linebreak 是否占满剩下行空间 true 标识占满；false标识不占满
     * @return this
     */
    public Span setLinebreak(boolean linebreak) {
        this.linebreak = linebreak;
        return this;
    }

    /**
     * 是否是一个占满剩余行空间的Span
     *
     * @return true 标识span会占满剩余的行空间； false 不占满
     */
    public boolean hasLinebreak() {
        return linebreak;
    }

    /**
     * 获取经过行内换行处理之后的Span列表
     *
     * @return 带有占满行剩余内容的Span序列
     */
    public LinkedList<Span> splitLineBreak() {
        LinkedList<Span> res = new LinkedList<>();
        if (!this.text.contains("\n")) {
            res.add(this);
        } else {
            String[] split = this.text.split("\n");
            for (int i = 0; i < split.length; i++) {
                Span lineSpan = this.clone().setText(split[i]);
                if (i != split.length - 1) {
                    lineSpan.setLinebreak(true);
                } else if (this.text.endsWith("\n")) {
                    lineSpan.setLinebreak(true);
                }
                res.add(lineSpan);
            }
        }
        return res;
    }

    /**
     * 获取下划线与文字的偏移量
     *
     * @return 下划线与文字的偏移量，单位毫米
     */
    public double getUnderlineOffset() {
        return underlineOffset;
    }

    /**
     * 获取下划线宽度
     *
     * @return 下划线宽度, 0表示保持默认，默认为字体大小的0.05倍，单位毫米
     */
    public double getUnderlineWidth() {
        return underlineWidth;
    }

    @Override
    public Span clone() {
        Span span = new Span();
        span.font = font;
        span.fontSize = fontSize;
        span.letterSpacing = letterSpacing;
        span.bold = bold;
        span.weight = weight;
        span.italic = italic;
        span.underline = underline;
        span.underlineOffset = underlineOffset;
        span.underlineWidth = underlineWidth;
        span.fill = fill;
        span.text = text;
        span.integrity = integrity;
        span.linebreak = linebreak;
        span.fillColor = fillColor == null ? null : fillColor.clone();
        return span;
    }
}
