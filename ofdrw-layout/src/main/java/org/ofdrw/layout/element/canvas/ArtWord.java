package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.text.text.Weight;
import org.ofdrw.font.Font;

import java.io.IOException;

/**
 * 艺术字元素
 * 本元素在Span元素的效果上增加了艺术字常见效果，，比如斜体，文字左右拉伸，文字垂直拉伸
 *
 * @author 韩兴元
 * @since 2024/6/12
 */
public class ArtWord extends CanvasBase {
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
     * 文本内容
     */
    private String text;

    /**
     * 字体颜色
     */
    private int[] color = new int[]{0, 0, 0};

    /**
     * 文本内容的当前对齐方式
     * 即从文本的什么位置开始绘制
     * 注意不是文本整体相对于页面的对齐方式
     */
    private TextAlign textAlign;
    /**
     * 文本布局状态
     */
    private org.ofdrw.layout.element.TextAlign textLayoutAlign;

    /**
     * 水平缩放比例
     * 小于1-水平挤压，1-正常，大于1-水平拉伸，这是一个比例值，比如0.5表示字宽变为原来的50%
     */
    private double horizontalScaling = 1D;

    /**
     * 垂直缩放比例
     * 小于1-垂直挤压，1-正常，大于1-垂直拉伸，这是一个比例值，比如0.5表示字宽变为原来的50%
     */
    private double verticalScaling = 1D;
    /**
     * 水平倾斜程度
     * 文本区域的上边框固定，水平方向移动下边框，正数-向右移动下边框，0-不倾斜，负数-向左移动下边框，视觉效果上文本会有轻微的水平位移
     */
    private double horizontalInclination = 0D;
    /**
     * 垂直倾斜程度
     * 文本区域的左边框固定，垂直方向移动右边框，正数-向下移动右边框，0-不倾斜，负数-向上移动右边框，视觉效果上文本会有轻微的垂直位移
     */
    private double verticalInclination = 0D;
    /**
     * 水平偏移量，单位：毫米
     */
    private double offsetX = 0D;
    /**
     * 垂直偏移量，单位：毫米
     */
    private double offsetY = 0D;


    public ArtWord(Double width, Double height) {
        super(width, height);
    }

    public int[] getColor() {
        return color;
    }

    /**
     * 设置字体颜色
     *
     * @param rgb 颜色值
     * @return this
     */
    public ArtWord setColor(int[] rgb) {
        this.color = rgb;
        return this;
    }

    /**
     * 设置字体颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 懒
     * @return this
     */
    public ArtWord setColor(int r, int g, int b) {
        this.color = new int[]{r, g, b};
        return this;
    }

    /**
     * 设置字体颜色
     *
     * @param color 16进制颜色，如#FFFFFF
     * @return this
     */
    public ArtWord setColor(String color) {
        this.color = new int[]{
                Integer.parseInt(color.substring(1, 3), 16),
                Integer.parseInt(color.substring(3, 5), 16),
                Integer.parseInt(color.substring(5, 7), 16)};
        return this;
    }

    /**
     * @return 字符数量
     */
    public int length() {
        return text.length();
    }

    public Font getFont() {
        return font;
    }

    public ArtWord setFont(Font font) {
        this.font = font;
        return this;
    }

    public Double getFontSize() {
        return fontSize;
    }

    public ArtWord setFontSize(Double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    public Double getLetterSpacing() {
        return letterSpacing;
    }

    public ArtWord setLetterSpacing(Double letterSpacing) {
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
    public ArtWord setBold(boolean bold) {
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
    public ArtWord setWeight(Weight weight) {
        this.weight = weight;
        return this;
    }

    public boolean isItalic() {
        return italic;
    }

    public ArtWord setItalic(boolean italic) {
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
    public ArtWord setUnderline(boolean underline) {
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
    public ArtWord setUnderline(boolean underline, double offset, double width) {
        this.underline = underline;
        this.underlineOffset = offset;
        this.underlineWidth = width;
        return this;
    }

    public String getText() {
        return text;
    }

    public ArtWord setText(String text) {
        this.text = text;
        return this;
    }

    public double getOffsetY() {
        return offsetY;
    }

    public void setOffsetY(double offsetY) {
        this.offsetY = offsetY;
    }

    public double getOffsetX() {
        return offsetX;
    }

    public void setOffsetX(double offsetX) {
        this.offsetX = offsetX;
    }

    public double getVerticalInclination() {
        return verticalInclination;
    }

    public void setVerticalInclination(double verticalInclination) {
        this.verticalInclination = verticalInclination;
    }

    public double getHorizontalInclination() {
        return horizontalInclination;
    }

    public void setHorizontalInclination(double horizontalInclination) {
        this.horizontalInclination = horizontalInclination;
    }

    public double getVerticalScaling() {
        return verticalScaling;
    }

    public void setVerticalScaling(double verticalScaling) {
        this.verticalScaling = verticalScaling;
    }

    public double getHorizontalScaling() {
        return horizontalScaling;
    }

    public void setHorizontalScaling(double horizontalScaling) {
        this.horizontalScaling = horizontalScaling;
    }

    public TextAlign getTextAlign() {
        return textAlign;
    }

    public void setTextAlign(TextAlign textAlign) {
        this.textAlign = textAlign;
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

    public org.ofdrw.layout.element.TextAlign getTextLayoutAlign() {
        return textLayoutAlign;
    }

    public void setTextLayoutAlign(org.ofdrw.layout.element.TextAlign textLayoutAlign) {
        this.textLayoutAlign = textLayoutAlign;
    }

    @Override
    public ArtWord clone() {
        ArtWord artWord = new ArtWord(getWidth(), getHeight());
        artWord.font = font;
        artWord.fontSize = fontSize;
        artWord.letterSpacing = letterSpacing;
        artWord.bold = bold;
        artWord.weight = weight;
        artWord.italic = italic;
        artWord.underline = underline;
        artWord.underlineOffset = underlineOffset;
        artWord.underlineWidth = underlineWidth;
        artWord.text = text;
        artWord.color = color == null ? null : color.clone();
        return artWord;
    }

    @Override
    public void draw(DrawContext ctx) throws IOException {
        // 添加外部字体
//        if (font != null && !StringUtils.isBlank(font.getName()) && font.getFontFile() != null) {
//            ctx.addFont(font.getName(), font.getFontFile());
//        }
        // 字体
        FontSetting fontSetting = new FontSetting(fontSize, font);
        fontSetting.setLetterSpacing(letterSpacing);
        if (bold) {
            fontSetting.setBold();
        }
        if (weight != null) {
            fontSetting.setFontWeight(weight.getWeight());
        }
        fontSetting.setItalic(italic);
        fontSetting.setTextAlign(textAlign);
        ctx.setFont(fontSetting);

        // 字体颜色
        ctx.setFillColor(color);
        /**
         *  a 水平缩放绘图 小于1-水平挤压，1-正常，大于1-水平拉伸，这是一个比例值，比如0.5表示字宽变为原来的50%
         *  b 水平倾斜绘图 文本区域的左边框固定，垂直方向移动右边框，正数-向下移动右边框，0-不倾斜，负数-向上移动右边框，视觉效果上文本会有轻微的垂直位移
         *  c 垂直倾斜绘图 文本区域的上边框固定，水平方向移动下边框，正数-向右移动下边框，0-不倾斜，负数-向左移动下边框，视觉效果上文本会有轻微的水平位移
         *  d 垂直缩放绘图 小于1-垂直挤压，1-正常，大于1-垂直拉伸，这是一个比例值，比如0.5表示字宽变为原来的50%
         *  e 水平移动绘图 水平偏移量，单位：毫米
         *  f 垂直移动绘图 垂直偏移量，单位：毫米
         */
        ctx.setTransform(horizontalScaling, verticalInclination, horizontalInclination, verticalScaling, offsetX, offsetY);
        // 计算文本宽度
        double textWidth = ctx.measureText(text).width;
        // 根据对齐方式计算绘制文本的起始位置
        double x = 0;
        if (textLayoutAlign != null) {
            switch (textLayoutAlign) {
                case left: {
                    x = 0;
                    break;
                }
                case right: {
                    x = getWidth() - textWidth;
                    break;
                }
                case center: {
                    x = (getWidth() - textWidth) / 2;
                    break;
                }
                default: {
                    x = 0;
                }
            }
        }
        ctx.fillText(text, x, fontSize);

        // 绘制下滑线
        if (underline) {
            ctx.setLineWidth(underlineWidth == 0D ? fontSize * 0.05 : underlineWidth);
            ctx.beginPath();
            ctx.setGlobalAlpha(1D);
            ctx.strokeStyle = CT_Color.rgb(color);
            ctx.moveTo(x, fontSize + offsetY + underlineOffset);
            ctx.lineTo(x + textWidth, fontSize + offsetY + underlineOffset);
            ctx.stroke();
        }
    }
}
