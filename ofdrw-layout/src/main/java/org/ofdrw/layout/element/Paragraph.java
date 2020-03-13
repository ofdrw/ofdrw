package org.ofdrw.layout.element;

import org.ofdrw.layout.Rectangle;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

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

    private LinkedList<TxtLineBlock> lineCache;


    public Paragraph() {
        this.setClear(Clear.both);
        this.contents = new LinkedList<>();
        this.lineCache = new LinkedList<>();
    }

    /**
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

    /**
     * 创建新的行
     *
     * @return 行块
     */
    private TxtLineBlock newLine() {
        return new TxtLineBlock(getWidth(), lineSpace);
    }

    /**
     * 获取尺寸
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    @Override
    public Rectangle doPrepare(Double widthLimit) {
        this.lineCache.clear();
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
        while (!seq.isEmpty()) {
            Span s = seq.pop();

            // 获取Span整体的块的大小
            double blockWidth = s.blockSize().getWidth();
            if (blockWidth > width && s.isIntegrity()) {
                // TODO 警告 不可分割元素如果大于行宽度则忽略
                continue;
            }

            // 尝试向行中加入元素
            boolean added = line.tryAdd(s);
            if (added) {
                // 成功加入
                continue;
            }

            // 无法加入行内，且Span 不可分割，那么需要换行
            if (s.isIntegrity()) {
                lineCache.add(line);
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
            lineCache.add(line);
            line = newLine();
        }
        // 最后一行处理
        if (!line.isEmpty()) {
            lineCache.add(line);
        }
        // 合并所有行的高度
        double height = lineCache.stream()
                .mapToDouble(TxtLineBlock::getHeight)
                .sum();
        // 设置元素高度
        setHeight(height);
        width += widthPlus();
        height += heightPlus();
        return new Rectangle(width, height);
    }


    /**
     * 根据给定的切分段落
     * <p>
     * 段落会根据指定的宽度和高度，判断空间是否能够容纳该行，
     * 如果无法容纳，那么将会切分文字内容到两个独立的段落中。
     *
     * <p>
     * 截断元素前必须确定元素的宽度和高度，否则将会抛出异常
     * <p>
     * 截断的元素在截断出均无margin、border、padding
     *
     * @param sHeight 切分高度
     * @return 根据给定空间分割之后的新元素
     */
    @Override
    public Div[] split(double sHeight) {
        Double width = this.getWidth();
        Double height = getHeight();
        if (width == null || height == null) {
            throw new RuntimeException("切分元素必须要有固定的宽度（width）和高度（height），请先运行reSize方法");
        }
        if (height + heightPlus() <= sHeight) {
            // 不足够切分
            return new Div[]{this};
        }
        // TODO 通过行缓存决定如何切分
        throw new NotImplementedException();
    }
}
