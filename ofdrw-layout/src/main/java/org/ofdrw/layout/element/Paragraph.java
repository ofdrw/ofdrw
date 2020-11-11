package org.ofdrw.layout.element;

import org.ofdrw.font.Font;
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
     * 首行缩进字符数
     * <p>
     * null 标识没有缩进
     */
    private Integer firstLineIndent = null;

    /**
     * 首行缩进数值
     * 单位：mm
     */
    private Double firstLineIndentWidth = null;

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

    /**
     * 元素内行缓存
     */
    private LinkedList<TxtLineBlock> lines;

    /**
     * 创建一个固定大小段落对象
     *
     * @param width  内容宽度
     * @param height 内容高度
     */
    public Paragraph(Double width, Double height) {
        this();
        setWidth(width)
                .setHeight(height);
    }

    public Paragraph() {
        this.setClear(Clear.both);
        this.contents = new LinkedList<>();
        this.lines = new LinkedList<>();
    }

    /**
     * 新建一个段落对象
     *
     * @param text 文字内容
     */
    public Paragraph(String text) {
        this();
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

    public LinkedList<TxtLineBlock> getLines() {
        return lines;
    }

    /**
     * 创建新的行
     *
     * @return 行块
     */
    private TxtLineBlock newLine() {
        return new TxtLineBlock(getWidth(), lineSpace);
    }


    /**
     * 获取首行缩进字符数
     *
     * @return 首行缩进字符数 null表示没有缩进
     */
    public Integer getFirstLineIndent() {
        return firstLineIndent;
    }

    /**
     * 设置段落首行缩进
     * <p>
     * 默认不缩进
     *
     * @param firstLineIndent 缩进字符数null或0表示不缩进
     * @return this
     */
    public Paragraph setFirstLineIndent(Integer firstLineIndent) {
        this.firstLineIndent = firstLineIndent;
        return this;
    }

    /**
     * 获取首行缩进数值
     * <p>
     * 该方法仅在已经设置了首行缩进数值的情况下才会返还正确的数值，否则返还null
     *
     * @return 首行缩进数值或null
     */
    public Double getFirstLineIndentWidth() {
        return firstLineIndentWidth;
    }

    /**
     * 设置段落首行缩进数值
     *
     * @param firstLineIndentWidth 首行缩进数值（单位：mm）
     * @return this
     */
    public Paragraph setFirstLineIndentWidth(double firstLineIndentWidth) {
        this.firstLineIndentWidth = firstLineIndentWidth;
        return this;
    }

    /**
     * 清除缩进格式
     *
     * @return this
     */
    public Paragraph clearFirstLineIndent() {
        this.firstLineIndent = null;
        return this;
    }

    /**
     * 处理Span内部含有换行符转换为占满剩余行空间的多个元素队列
     *
     * @param seq 待处理Span队列
     * @return 处理后含有占满剩余行空间的Span队列
     */
    private LinkedList<Span> spanLinebreakSplit(LinkedList<Span> seq) {
        LinkedList<Span> sps = new LinkedList<>();
        if (seq == null || seq.isEmpty()) {
            return sps;
        }
        while (!seq.isEmpty()) {
            Span pop = seq.pop();
            // 通过换行符对span内容进行分割，获取新的队列
            LinkedList<Span> spans = pop.splitLineBreak();
            sps.addAll(spans);
        }
        return sps;
    }

    /**
     * 处理占位符
     *
     * @param seq 段落中的span队列
     */
    private void processPlaceholder(LinkedList<Span> seq) {
        if (seq == null || seq.isEmpty()) {
            return;
        }
        // 不需要加入占位符
        if ((firstLineIndent == null || firstLineIndent == 0)
                && (firstLineIndentWidth == null || firstLineIndentWidth == 0)) {
            return;
        }

        Span firstSpan = seq.peek();
        if (firstSpan instanceof PlaceholderSpan) {
            PlaceholderSpan h = (PlaceholderSpan) firstSpan;
            // 已经存在段落缩进并且设置的段落缩进为0，那么删除该占位符
            if (h.getHoldWidth() == 0 && h.getHoldNum() == 0) {
                seq.pop();
            }
            if (firstLineIndentWidth != null && firstLineIndentWidth > 0) {
                h.setHoldWidth(firstLineIndentWidth);
            } else {
                // 重设占位符的宽度
                h.setHoldChars(firstLineIndent);
            }
            return;
        }

        if (firstLineIndentWidth != null) {
            // 使用指定宽度和高度的占位符
            seq.push(new PlaceholderSpan(firstLineIndentWidth, firstSpan.getFontSize()));
        } else {
            // 如果第一个不是占位符，并且占位符数目大于0 那么创建新的占位符,并且加入渲染队列
            seq.push(new PlaceholderSpan(firstLineIndent, firstSpan));
        }
    }

    /**
     * 如果元素高度不存在那么设置高度
     * <p>
     * 如果已经设置了高度，该方法不会对该高度造成影响
     *
     * @param height 高度
     */
    private void setHeightIfNotExist(double height) {
        if (getHeight() == null || getHeight() == 0) {
            setHeight(height);
        }
    }

    /**
     * 预布局
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    @Override
    public Rectangle doPrepare(Double widthLimit) {
        this.lines.clear();
        Double width = this.getWidth();
        if (widthLimit == null) {
            throw new NullPointerException("widthLimit为空");
        }
        widthLimit -= widthPlus();
        if (width == null || (width > widthLimit)) {
            // TODO 尺寸重设警告日志
            width = widthLimit;
        }

        setWidth(width);
        TxtLineBlock line = newLine();
        LinkedList<Span> seq = new LinkedList<>(contents);
        // 处理相对列中插入或调整首行缩进占位符
        processPlaceholder(seq);
        // 处理Span中含有换行符的情况
        seq = spanLinebreakSplit(seq);
        while (!seq.isEmpty()) {
            Span s = seq.pop();

            // 获取Span整体的块的大小
            double blockWidth = s.blockSize().getWidth();
            if (blockWidth > width && s.isIntegrity()) {
                // TODO 警告 不可分割元素如果大于行宽度则忽略
                continue;
            }
            // 特殊的如果文字可以被分割，但是第一个
            // 文字的大小就已经超过可用最大空间限制
            // 那么丢弃系列文字
            if (!s.isIntegrity() && width < s.glyphList().get(0).getW()) {
                continue;
            }

            // 尝试向行中加入元素
            boolean added = line.tryAdd(s);
            if (added) {
                // 如果加入的Span是一个需要占满剩余行空间的元素，那么新起一行
                if (s.hasLinebreak()) {
                    lines.add(line);
                    line = newLine();
                }
                // 成功加入
                continue;
            }

            // 无法加入行内，且Span 不可分割，那么需要换行
            if (s.isIntegrity()) {
                lines.add(line);
                line = newLine();
                // 重新进入队列
                seq.push(s);
                continue;
            }
            // 由于文字单元可以分割，那么尝试通过分割的方式填充该行
            Span toNextLineSpan = line.trySplitAdd(s);
            if (toNextLineSpan == null) {
                // 行空间耗尽，重新加入队列
                seq.push(s);
            } else {
                // 将切分后的文字单元加入队列
                seq.push(toNextLineSpan);
            }
            lines.add(line);
            line = newLine();
        }
        // 最后一行处理
        if (!line.isEmpty()) {
            lines.add(line);
        }
        // 合并所有行的高度
        double height = lines.stream()
                .mapToDouble(TxtLineBlock::getHeight)
                .sum();
        // 设置元素高度，如果元素已经预设高度那么则不设置
        setHeightIfNotExist(height);
        /*
         * 段落中只有一行的情况，以行内元素确定元素宽度
         */
        if (lines.size() == 1) {
            width = lines.get(0).getWidth();
            setWidth(width);
        }

        width += widthPlus();
        height += heightPlus();
        return new Rectangle(width, height);
    }

    /**
     * 设置行元素，并行中文字单元加入到内容中
     *
     * @param lines 行序列
     * @return this
     */
    private Paragraph setLines(LinkedList<TxtLineBlock> lines) {
        this.lines = lines;
        for (TxtLineBlock item : lines) {
            this.contents.addAll(item.getInlineSpans());
        }
        return this;
    }

    @Override
    public <T extends Div> Div[] contentSplitAdjust(double sHeight, T div1, T div2) {
        // 文字内容或是Bottom 耗尽了空间 分段的情况
        LinkedList<TxtLineBlock> seq2 = new LinkedList<>(this.lines);
        LinkedList<TxtLineBlock> seq1 = new LinkedList<>();
        double remainHeight = sHeight - getMarginTop() - getBorderTop() - getPaddingTop();
        while (!seq2.isEmpty()) {
            TxtLineBlock line = seq2.pop();
            if (remainHeight < line.getHeight()) {
                // 空间不足
                seq2.push(line);
                break;
            } else {
                seq1.add(line);
                remainHeight -= line.getHeight();
            }
        }
        Paragraph p1 = (Paragraph) div1;
        Paragraph p2 = (Paragraph) div2;
        // seq2 为空可能由于Margin等参数导致的空间不足
        if (seq2.isEmpty()) {
            p1.setLines(seq1)
                    .setMarginBottom(0d)
                    .setBorderBottom(0d)
                    .setPaddingBottom(0d);
            p2.setHeight(0d)
                    .setMarginTop(0d)
                    .setBorderTop(0d)
                    .setPaddingTop(0d);
            return new Div[]{p1, p2};
        }
        // 剩余空间一行都无法放下的情况，整个对象放到下一个段中，并使用占位符占有上一个段的空间
        if (seq1.isEmpty()) {
            Div placeholder = Div.placeholder(this.getWidth() + widthPlus(), sHeight, this.getFloat());
            return new Div[]{placeholder, this};
        }
        // 正常情况下的分割
        p1.setLines(seq1)
                .setMarginBottom(0d)
                .setBorderBottom(0d)
                .setPaddingBottom(0d);
        p2.setLines(seq2)
                .setMarginTop(0d)
                .setBorderTop(0d)
                .setPaddingTop(0d);
        return new Div[]{p1, p2};
    }

    /**
     * 请勿调用该方法克隆段落，除非你知道你在干什么
     *
     * @return 属性一模一样，但是没有任何内容的段落
     */
    @Override
    public Paragraph clone() {
        Paragraph p = new Paragraph();
        p = this.copyTo(p);
        p.lineSpace = lineSpace;
        p.defaultFont = defaultFont;
        p.defaultFontSize = defaultFontSize;

        p.lines = new LinkedList<>();
        p.contents = new LinkedList<>();
        return p;
    }
}
