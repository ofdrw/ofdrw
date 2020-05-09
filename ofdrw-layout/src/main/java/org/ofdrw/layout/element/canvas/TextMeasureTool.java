package org.ofdrw.layout.element.canvas;

import org.ofdrw.font.Font;

import java.util.Arrays;

/**
 * 字体测量工具
 *
 * @author 权观宇
 * @since 2020-05-08 21:30:59
 */
public final class TextMeasureTool {

    public static class MeasureBody {
        public Double[] offset;
        public double width;

        public MeasureBody() {
            offset = new Double[0];
            width = 0;
        }

        /**
         * 加上偏移量后的宽度
         *
         * @param charLen 字符宽度
         */
        public void with(double charLen) {
            width = Arrays.stream(offset).mapToDouble(Math::abs).sum() + charLen;
        }
    }


    /**
     * 分析字间距偏移量并计算文字宽度
     *
     * @param text        文本
     * @param fontSetting 文字设置
     * @return 测量结果
     */
    public static MeasureBody measureWithWith(String text, FontSetting fontSetting) {
        MeasureBody body = new MeasureBody();

        if (text.length() == 0) {
            return body;
        }

        int readDirection = fontSetting.getReadDirection();
        int charDirection = fontSetting.getCharDirection();

        char firstChar = text.charAt(0);
        char lastChar = text.charAt(text.length() - 1);

        if (readDirection == 0) {
            if (charDirection == 0) {
                body.offset = offset(text, fontSetting, 0, "W", 1);
                body.with(fontSetting.charWidth(lastChar));
            } else if (charDirection == 180) {
                body.offset = offset(text, fontSetting, 1, "W", 1);
                body.with(fontSetting.charWidth(firstChar));
            } else if (charDirection == 90 || charDirection == 270) {
                body.offset = offset(text, fontSetting, 0, "H", 1);
                body.with(fontSetting.getFontSize());
            }
        } else if (readDirection == 180) {
            if (charDirection == 0) {
                body.offset = offset(text, fontSetting, 1, "W", -1);
                body.with(fontSetting.charWidth(firstChar));
            } else if (charDirection == 180) {
                body.offset = offset(text, fontSetting, 0, "W", -1);
                body.with(fontSetting.charWidth(lastChar));
            } else if (charDirection == 90 || charDirection == 270) {
                body.offset = offset(text, fontSetting, 0, "H", -1);
                body.with(fontSetting.getFontSize());
            }
        } else if (readDirection == 90) {
            if (charDirection == 0 || charDirection == 180) {
                body.offset = offset(text, fontSetting, 0, "H", 1);
                body.with(fontSetting.getFontSize());
            } else if (charDirection == 90) {
                body.offset = offset(text, fontSetting, 0, "W", 1);
                body.with(fontSetting.charWidth(lastChar));
            } else if (charDirection == 270) {
                body.offset = offset(text, fontSetting, 1, "W", 1);
                body.with(fontSetting.charWidth(firstChar));
            }
        } else if (readDirection == 270) {
            if (charDirection == 0 || charDirection == 180) {
                body.offset = offset(text, fontSetting, 0, "H", -1);
                body.with(fontSetting.getFontSize());
            } else if (charDirection == 90) {
                body.offset = offset(text, fontSetting, 1, "W", -1);
                body.with(fontSetting.charWidth(firstChar));
            } else if (charDirection == 270) {
                body.offset = offset(text, fontSetting, 0, "W", -1);
                body.with(fontSetting.charWidth(lastChar));
            }
        }
        return body;
    }

    /**
     * 测量文本各个字符在特定排列方式下的偏移量数组
     *
     * @param text        需要测试文本
     * @param fontSetting 文字配置
     * @return 偏移量数组
     */
    public static Double[] measure(String text, FontSetting fontSetting) {
        return measureWithWith(text, fontSetting).offset;
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

        if (text.length() == 1) {
            return new Double[0];
        }

        Double fontSize = fontSetting.getFontSize();
        Double letterSpacing = fontSetting.getLetterSpacing();
        Double[] offsetArray = new Double[text.length() - 1];

        for (int i = indexOffset, len = indexOffset + text.length() - 1; i < len; i++) {
            int index = indexOffset == 0 ? i : i - 1;

            if ("w".equalsIgnoreCase(wOrH)) {
                offsetArray[index] = fontSetting.charWidth(text.charAt(i)) + letterSpacing;
            } else {
                offsetArray[index] = fontSize + letterSpacing;
            }
            // read direction 180 或 270
            offsetArray[index] = direction * offsetArray[index];
        }
        return offsetArray;
    }

}
