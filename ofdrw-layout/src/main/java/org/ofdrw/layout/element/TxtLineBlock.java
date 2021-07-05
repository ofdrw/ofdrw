package org.ofdrw.layout.element;

import org.ofdrw.layout.Rectangle;

import java.util.LinkedList;
import java.util.List;

/**
 * 行内文字
 *
 * @author 权观宇
 * @since 2020-03-13 01:03:54
 */
public class TxtLineBlock {
    /**
     * 行内字体浮动方向
     * <p>
     * 默认：左浮动
     */
    private TextAlign textAlign;
    /**
     * 行内所在元素
     */
    private List<Span> inlineSpans;

    /**
     * 最高的文字高度
     */
    private Double maxSpanHeight = 0d;

    /**
     * 行宽度
     */
    private Double width = 0d;
    /**
     * 行间距
     */
    private Double lineSpace;

    /**
     * 行最大宽度
     */
    private double lineMaxAvailableWidth;

    private TxtLineBlock() {
    }

    /**
     * 创建行块
     *
     * @param lineMaxAvailableWidth 行内运行最大宽度
     * @param lineSpace             行间距
     */
    public TxtLineBlock(double lineMaxAvailableWidth, Double lineSpace) {
        this(lineMaxAvailableWidth, lineSpace, TextAlign.left);
    }

    /**
     * 创建行块
     *
     * @param lineMaxAvailableWidth 行内运行最大宽度
     * @param lineSpace             行间距
     * @param textAlign             行内字体浮动方式
     */
    public TxtLineBlock(double lineMaxAvailableWidth, Double lineSpace, TextAlign textAlign) {
        this.lineMaxAvailableWidth = lineMaxAvailableWidth;
        this.lineSpace = lineSpace;
        if (textAlign == null) {
            textAlign = TextAlign.left;
        }
        this.textAlign = textAlign;
        this.inlineSpans = new LinkedList<>();
    }


    /**
     * 尝试向行中加入文字单元
     *
     * @param span 文字单元
     * @return true - 足够容纳可以加入；false - 无法容纳无法加入
     */
    public boolean tryAdd(Span span) {
        Rectangle rec = span.blockSize();
        if (rec.getWidth() + width > lineMaxAvailableWidth) {
            // 空间不足不足以容纳元素
            return false;
        }
        width += rec.getWidth();
        if (rec.getHeight() > maxSpanHeight) {
            // 判断行高
            maxSpanHeight = rec.getHeight();
        }
        inlineSpans.add(span);
        return true;
    }

    /**
     * 尝试通过切分文字单元的方式加入行内
     * <p>
     * 如果文字单元能够被切分，那么会返回切分后剩余部分文字单元
     * <p>
     * 如果切分之后也无法加入行中，那么返回空。
     *
     * @param span 待切分文字单元
     * @return null 表示行的空间耗尽，无法加入新的文字；切分后的剩余的文字单元
     */
    public Span trySplitAdd(Span span) {
        if (span.isIntegrity()) {
            throw new IllegalStateException("文字单元（Span）不可拆分");
        }
        // 获取剩余宽度
        double remainWidth = lineMaxAvailableWidth - width;
        // 文字单元切分点
        int splitIndex = 0;
        // 行中的剩余空间不足且Span可以被换行等分割
        List<TxtGlyph> txtGlyphs = span.glyphList();
        for (int i = 0; i < txtGlyphs.size(); i++) {
            TxtGlyph txt = txtGlyphs.get(i);
            if (txt.getW() <= remainWidth) {
                remainWidth -= txt.getW();
                continue;
            }
            if (i == 0) {
                // 一个字符都无法加入到行内，说明行空间基本耗尽，需要换行
                return null;
            }
            // 找到前一个字符
            splitIndex = i;
            break;
        }
        // 切分文字单元
        Span[] split = span.split(splitIndex);
        // 前半段放入行中
        tryAdd(split[0]);
        // 后半段返回，准备加入下一段
        return split[1];
    }

    public boolean isEmpty() {
        return inlineSpans.isEmpty();
    }

    /**
     * 获取行内所有文字单元
     *
     * @return 行内所有文字单元
     */
    public List<Span> getInlineSpans() {
        return inlineSpans;
    }

    /**
     * 获取行所占据的区域大小
     *
     * @return 行占据区域
     */
    public Rectangle size() {
        return new Rectangle(width, maxSpanHeight + lineSpace);
    }

    /**
     * 获取整个行占据的高度（文字高度 + 行间距）
     *
     * @return 行占据的高度
     */
    public double getHeight() {
        return maxSpanHeight + lineSpace;
    }


    /**
     * 获取行间距
     *
     * @return 行间距
     */
    public Double getLineSpace() {
        return lineSpace;
    }

    /**
     * 行内最高元素高度
     *
     * @return 行内最高元素高度
     */
    public Double getMaxSpanHeight() {
        return maxSpanHeight;
    }

    /**
     * 行内容元素宽度
     *
     * @return 行宽度
     */
    public Double getWidth() {
        return width;
    }

    /**
     * 获取文字在行内的浮动方式
     * <p>
     * 默认：左浮动
     *
     * @return 浮动方式
     */
    public TextAlign getTextAlign() {
        return textAlign;
    }

    /**
     * 获取 行内可用最大宽度
     *
     * @return 行内可用最大宽度
     */
    public double getLineMaxAvailableWidth() {
        return lineMaxAvailableWidth;
    }
}
