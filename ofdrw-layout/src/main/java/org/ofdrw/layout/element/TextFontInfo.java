package org.ofdrw.layout.element;

import org.ofdrw.font.Font;

/**
 * 文本字体信息
 *
 * @author 权观宇
 * @since 2020-05-07 19:18:43
 */
public interface TextFontInfo {
    /**
     * 获取字体对象
     * @return 字体对象
     */
    Font getFont();

    /**
     * 获取字间距
     * @return 字间距，单位毫米mm
     */
    Double getLetterSpacing();

    /**
     * 获取文字字号
     * @return 字号，单位毫米mm
     */
    Double getFontSize();
}
