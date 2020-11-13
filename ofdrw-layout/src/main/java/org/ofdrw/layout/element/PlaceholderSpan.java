package org.ofdrw.layout.element;

import org.ofdrw.layout.Rectangle;

import java.util.LinkedList;

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
     * 占位符长度
     * <p>
     * holdWidth 和 holdNum 仅运行一个有效，如果存在holdWidth优先级高于holdNum
     * <p>
     * 单位：mm
     */
    private double holdWidth;

    /**
     * 创建占位符
     *
     * @param holdWidth 占位符宽度
     * @param height    占位符高度
     */
    public PlaceholderSpan(double holdWidth, double height) {
        this.holdWidth = holdWidth;
        this.setFontSize(height);
        // 占位符不可在分割
        setIntegrity(true);
    }

    /**
     * 创建占位符
     * <p>
     * 该方式创建的占位符字间距为0
     *
     * @param holdNum  占位符数目
     * @param fontSize 每个占位符字体大小
     */
    public PlaceholderSpan(int holdNum, double fontSize) {
        this.setFontSize(fontSize);
        // 占位符不可在分割
        setIntegrity(true);
        setHoldChars(holdNum);
    }

    /**
     * 通过复制span的方式创建占位符
     *
     * @param holdNum 占位符数量
     * @param sp      用于复制的原始span
     */
    public PlaceholderSpan(int holdNum, Span sp) {
        // 占位符不可在分割
        setIntegrity(true);
        this.setFontSize(sp.getFontSize());
        // 设置字间距
        this.setLetterSpacing(sp.getLetterSpacing());
        this.setHoldChars(holdNum);
    }

    /**
     * 设置占位的字符
     *
     * @param holdNum 占位符数量
     * @return this
     */
    public PlaceholderSpan setHoldChars(int holdNum) {
        this.holdNum = holdNum;
        // 占位符宽度为 占位符宽度
        this.holdWidth = holdNum * (this.getFontSize() + this.getLetterSpacing());
        return this;
    }

    /**
     * 获取位符数量
     * <p>
     * 仅在使用占位数量创建对象时有效 {@link #PlaceholderSpan(int, double)} 或 {@link #PlaceholderSpan(int, Span)}
     *
     * @return 占位符数量
     */
    public int getHoldNum() {
        return holdNum;
    }

    /**
     * 占位符宽度
     * <p>
     * 仅在使用占位宽度创建对象时有效 {@link #PlaceholderSpan(double, double)}
     *
     * @return 占位符宽度
     */
    public double getHoldWidth() {
        return holdWidth;
    }

    /**
     * 设置占位符宽度
     * <p>
     * 如果设置该了宽度，那么HoldNum将会失效
     *
     * @param holdWidth 占位符宽度，单位mm
     * @return this
     */
    public PlaceholderSpan setHoldWidth(double holdWidth) {
        this.holdWidth = holdWidth;
        return this;
    }

    /**
     * 占位符大小，如果holdWidth存在那么返还指定宽高的矩形
     * 否则根据字体进行计算
     *
     * @return 占位符大小
     */
    @Override
    public Rectangle blockSize() {
        return new Rectangle(holdWidth, this.getFontSize());
    }

    /**
     * 获取经过行内换行处理之后的Span列表
     *
     * @return 带有占满行剩余内容的Span序列
     */
    @Override
    public LinkedList<Span> splitLineBreak() {
        LinkedList<Span> res = new LinkedList<>();
        res.add(this);
        return res;
    }
}
