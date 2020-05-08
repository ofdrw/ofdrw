package org.ofdrw.layout.element.canvas;

import org.ofdrw.font.Font;

/**
 * 字体测量工具
 *
 * @author 权观宇
 * @since 2020-05-08 21:30:59
 */
public final class TextMeasureTool {

    /**
     * 测量文本各个字符在特定排列方式下的偏移量数组
     *
     * @param text        需要测试文本
     * @param fontSetting 文字配置
     * @return 偏移量数组
     */
    public static Double[] measure(String text, FontSetting fontSetting) {
        if (text.length() <= 1) {
            return new Double[]{};
        }

        int readDirection = fontSetting.getReadDirection();
        int charDirection = fontSetting.getCharDirection();


        if (readDirection == 0) {
            if (charDirection == 0) {
                return offset(text, fontSetting, 0, "W", 1);
            } else if (charDirection == 180) {
                return offset(text, fontSetting, 1, "W", 1);
            } else if (charDirection == 90 || charDirection == 270) {
                return offset(text, fontSetting, 0, "H", 1);
            }
        } else if (readDirection == 180) {
            if (charDirection == 0) {
                return offset(text, fontSetting, 1, "W", -1);
            } else if (charDirection == 180) {
                return offset(text, fontSetting, 0, "W", -1);
            } else if (charDirection == 90 || charDirection == 270) {
                return offset(text, fontSetting, 0, "H", -1);
            }
        } else if (readDirection == 90) {
            if (charDirection == 0 || charDirection == 180) {
                return offset(text, fontSetting, 0, "H", 1);
            } else if (charDirection == 90) {
                return offset(text, fontSetting, 0, "W", 1);
            } else if (charDirection == 270) {
                return offset(text, fontSetting, 1, "W", 1);
            }
        } else if (readDirection == 270) {
            if (charDirection == 0 || charDirection == 180) {
                return offset(text, fontSetting, 0, "H", -1);
            } else if (charDirection == 90) {
                return offset(text, fontSetting, 1, "W", -1);
            } else if (charDirection == 270) {
                return offset(text, fontSetting, 0, "W", -1);
            }
        }
        throw new IllegalArgumentException("非法阅读(readDirection)方向：" + readDirection);
    }


    /**
     * 获取字符偏移量
     *
     * @param text        文字
     * @param fontSetting 文字设置
     * @param indexOffset 相对偏移
     * @param wOrH        宽度或高度作为偏移量，W 或 H
     * @param direction   文字排列方向：1 正序递增，-1 逆序递减。
     * @return 偏移数组
     */
    private static Double[] offset(String text,
                                   FontSetting fontSetting,
                                   int indexOffset,
                                   String wOrH,
                                   int direction) {

        Double fontSize = fontSetting.getFontSize();
        Double letterSpacing = fontSetting.getLetterSpacing();
        Font font = fontSetting.getFont();
        Double[] offsetArray = new Double[text.length() - 1];

        for (int i = indexOffset, len = indexOffset + text.length() - 1; i < len; i++) {
            int index = indexOffset == 0 ? i : i - 1;

            if ("w".equalsIgnoreCase(wOrH)) {
                offsetArray[index] = font.getCharWidthScale(text.charAt(i)) * fontSize + letterSpacing;
            } else {
                offsetArray[index] = fontSize + letterSpacing;
            }
            // read direction 180 或 270
            offsetArray[index] = direction * offsetArray[index];
        }
        return offsetArray;
    }
}
