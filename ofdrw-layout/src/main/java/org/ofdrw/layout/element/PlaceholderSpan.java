package org.ofdrw.layout.element;

/**
 * 占位符
 *
 * @author 权观宇
 * @since 2020-03-28 11:45:59
 */
public class PlaceholderSpan extends Span {


    /**
     * 占位数量
     */
    private int holdNum;
    /**
     * 占位符字符大小
     */
    private double fontSize;

    /**
     * 创建占位符
     *
     * @param holdNum  占位符数目
     * @param fontSize 每个占位符字体大小
     */
    public PlaceholderSpan(int holdNum, double fontSize) {
        this.holdNum = holdNum;
        this.fontSize = fontSize;
        this.setHoldChars(holdNum);
    }

    /**
     * 设置占位的字符
     *
     * @param holdNum 占位符数量
     * @return this
     */
    public PlaceholderSpan setHoldChars(int holdNum) {
        StringBuilder txt = new StringBuilder();
        for (int i = 0, len = holdNum * 2; i < len; i++) {
            txt.append(" ");
        }
        this.setText(txt.toString());
        return this;
    }

    @Override
    public PlaceholderSpan setFontSize(Double fontSize) {
        this.fontSize = fontSize;
        return this;
    }

    /**
     * 获取字符大小
     *
     * @return 字符大小
     */
    @Override
    public Double getFontSize() {
        return fontSize;
    }
}
