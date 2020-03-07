package org.ofdrw.layout.engine;

import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.ArrayList;
import java.util.List;

/**
 * 段对象
 *
 * @author 权观宇
 * @since 2020-02-29 11:41:28
 */
public class Segment {
    /**
     * 段中的内容
     */
    private List<Div> content;

    /**
     * 段高度
     */
    private double height;
    /**
     * 段最大宽度
     */
    private double width;

    /**
     * 段剩余宽度
     */
    private double remainWidth;

    public Segment(double width) {
        this.width = width;
        this.remainWidth = width;
        content = new ArrayList<>(5);
    }

    private Segment() {
    }

    public double getHeight() {
        return height;
    }

    /**
     * 向段中添加元素
     *
     * @param div 元素
     * @return 元素是否能加入段中 true - 可加入；false - 不可加入
     */
    public boolean tryAdd(Div div) {
        if (div == null) {
            return true;
        }
        // 剩余空间已经不足
        if (this.remainWidth == 0) {
            return false;
        }

        Clear clear = div.getClear();
        AFloat aFloat = div.getFloat();

        // 段内已经有元素，并且新加入的元素为独占，那么不能加入段中
        if (content.size() != 0 && clear == Clear.both) {
            return false;
        }

        // 根据段宽度重置加入元素尺寸
        Rectangle blockSize = div.reSize(this.width);

        if (blockSize.getWidth() > this.remainWidth) {
            // 段剩余宽度不足无法放入元素，舍弃
            return false;
        }

        // 上面过程保证了元素一定可以加入到段中
        /*
        独占段的元素类型
        1. 独占
        2. 浮动 + Clear 对立
         */
        if (div.isBlockElement()) {
            this.remainWidth = 0;
            this.height = blockSize.getHeight();
            content.add(div);
            return true;
        }
        // 新加入的元素为居中元素，但是目前的段内不处于居中样式，那么无法加入
        if (aFloat == AFloat.center && !isCenterFloat()) {
            return false;
        }

        this.remainWidth -= blockSize.getWidth();
        content.add(div);
        if (height < blockSize.getHeight()) {
            this.height = blockSize.getHeight();
        }

        return true;
    }

    /**
     * 判断段内是否为居中布局
     *
     * @return true - 居中布局；false - 不居中
     */
    boolean isCenterFloat() {
        if (isEmpty()) {
            return true;
        }
        for (Div d : this.content) {
            AFloat aFloat = d.getFloat();
            if (aFloat != AFloat.center) {
                return false;
            }
        }
        return true;
    }

    /**
     * 段是否是空段，也就是没有任何元素
     *
     * @return true - 空段；false - 含有元素
     */
    public boolean isEmpty() {
        return this.content.isEmpty();
    }

    /**
     * 获取段中的元素
     *
     * @return 段中的元素
     */
    public List<Div> getContent() {
        return content;
    }
}
