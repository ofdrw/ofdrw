package org.ofdrw.layout.element;

import org.ofdrw.font.EnvFont;
import org.ofdrw.font.Font;

import java.awt.geom.Rectangle2D;

/**
 * 文字字符块
 *
 * @author 权观宇
 * @since 2020-02-28 05:51:05
 */
public class TxtGlyph {

    /**
     * 文字本身大小
     */
    private Rectangle2D bounds;
    /**
     * 文字
     */
    private final char txt;
    /**
     * 文字字体信息
     */
    private final TextFontInfo textFontInfo;

    public TxtGlyph(char txt, TextFontInfo span) {
        this.txt = txt;
        this.textFontInfo = span;
    }

    /**
     * 返回字体宽度
     * <p>
     * 字体绑定了字体文件时优先使用该文件的真实字形宽度，与 {@link org.ofdrw.layout.element.canvas.FontSetting#box(char)}
     * 的取值保持一致，避免排版宽度与实际渲染字体不符。
     *
     * @return 字符宽度(字符宽度 + 字间距)
     */
    public double getW() {
        Font font = textFontInfo.getFont();
        if (font.hasRealGlyphWidth()) {
            double scale = font.getRealCharWidthScale(txt);
            if (scale >= 0) {
                return textFontInfo.getLetterSpacing() + textFontInfo.getFontSize() * scale;
            }
        }
        if (font.hasWidthMath()) {
            double w = textFontInfo.getLetterSpacing();
            w += textFontInfo.getFontSize() * font.getCharWidthScale(txt);
            return w;
        }
        if (this.bounds == null) {
            this.bounds = measureBounds(font);
        }
        return textFontInfo.getLetterSpacing() + bounds.getWidth();
    }

    /**
     * 返回字体高度
     *
     * @return 字符高度
     */
    public double getH() {
        Font font = textFontInfo.getFont();
        if (font.hasWidthMath()) {
            return textFontInfo.getFontSize();
        }
        if (this.bounds == null) {
            this.bounds = measureBounds(font);
        }
        return bounds.getHeight();
    }

    /**
     * 测量字符外接矩形
     * <p>
     * 字体已绑定字体文件时用该文件测量，结果不随运行环境的系统字体而变；
     * 未绑定字体文件时按字体名查找系统字体测量，找不到时回退默认字体。
     *
     * @param font 字体信息
     * @return 字符外接矩形
     */
    private Rectangle2D measureBounds(Font font) {
        java.awt.Font fontObj = font.getFontObj();
        if (fontObj != null) {
            return fontObj.deriveFont((float) textFontInfo.getFontSize().doubleValue())
                    .getStringBounds(String.valueOf(txt), EnvFont.FRCtx());
        }
        return EnvFont.strBounds(font.getName(), font.getFamilyName(),
                String.valueOf(txt), textFontInfo.getFontSize());
    }
}
