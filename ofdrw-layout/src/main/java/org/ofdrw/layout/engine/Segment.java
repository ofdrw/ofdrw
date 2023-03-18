package org.ofdrw.layout.engine;

import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.PageAreaFiller;

import java.util.*;

/**
 * 段对象
 *
 * @author 权观宇
 * @since 2020-02-29 11:41:28
 */
public class Segment implements Iterable<Map.Entry<Div, Rectangle>>, Iterator<Map.Entry<Div, Rectangle>> {
    /**
     * 段中的内容
     */
    private List<Div> content;
    private List<Rectangle> sizeList;

    /**
     * 段高度
     */
    private double height = 0d;
    /**
     * 段最大宽度
     */
    private double width;

    /**
     * 段剩余宽度
     */
    private double remainWidth;

    /**
     * 段是否可以分块
     * <p>
     * true - 可拆分； false - 不可拆分
     * <p>
     * 默认值： false 不可拆分
     */
    private boolean blockable = false;

    /**
     * 剩余页面空间填充段
     * <p>
     * true - 填充剩余页面空间； false - 非填充段
     */
    private boolean isRemainAreaFiller = false;


    public Segment(double width) {
        this.width = width;
        this.remainWidth = width;
        content = new ArrayList<>(5);
        sizeList = new ArrayList<>(5);
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
        // 可是是应为: 段内已经有元素，并且新加入的元素为独占，那么不能加入段中
        //            也可能是刚好空间耗尽
        if (this.remainWidth == 0) {
            return false;
        }
        // 根据段宽度重置加入元素尺寸
        Rectangle blockSize = div.doPrepare(this.width);
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
            if (!isEmpty()) {
                // 独占类型如果已经存在元素那么则无法加入
                return false;
            }
            this.remainWidth = 0;
            add(div, blockSize);
            return true;
        }

        if (!this.isEmpty()) {
            if (isCenterFloat()) {
                // 段内含有居中元素:
                // 1. 新加入元素非居中元素
                // 2. 新加入元素设置 clear left
                // 3. 最后一个元素设置 clear right
                if (div.getFloat() != AFloat.center) {
                    return false;
                }
                if (Clear.left == div.getClear()) {
                    return false;
                }
                if (Clear.right == content.get(content.size() - 1).getClear()) {
                    return false;
                }
            } else {
                // 段内不含居中元素:
                // 1. Float == Clear Left: 段内已经含有左浮动元素
                // 2. Float == Clear Right: 段内已经含有右浮动元素
                // 3. 新加入元素为居中元素
                if (AFloat.left == div.getFloat() && Clear.left == div.getClear()) {
                    for (Div existDiv : content) {
                        if (existDiv.getFloat() == AFloat.left) {
                            return false;
                        }
                    }
                }
                if (AFloat.right == div.getFloat() && Clear.right == div.getClear()) {
                    for (Div existDiv : content) {
                        if (existDiv.getFloat() == AFloat.right) {
                            return false;
                        }
                    }
                }
                if (AFloat.center == div.getFloat()) {
                    return false;
                }
            }
        }
        // 段内不含元素时直接加入

        this.remainWidth -= blockSize.getWidth();
        add(div, blockSize);
        return true;
    }

    /**
     * 加入元素
     *
     * @param div       元素本身
     * @param blockSize 元素尺寸
     */
    private void add(Div<?> div, Rectangle blockSize) {
        if (height < blockSize.getHeight()) {
            this.height = blockSize.getHeight();
        }
        if (div.isIntegrity() == false) {
            // 判断是否可以拆分段，只要出现了一个可拆分的，那么该段就是可以拆分
            this.blockable = true;
        }
        if (div instanceof PageAreaFiller) {
            this.isRemainAreaFiller = true;
        }
        this.content.add(div);
        this.sizeList.add(blockSize);
    }


    /**
     * 段是否可拆分
     *
     * @return true - 可拆分；false - 不可拆分
     */
    public boolean isBlockable() {
        return blockable;
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
     * 是否是剩余填充空间
     *
     * @return true - 填充； false - 非填充
     */
    public boolean isRemainAreaFiller() {
        return isRemainAreaFiller;
    }

    /**
     * 获取段中的元素
     *
     * @return 段中的元素
     */
    public List<Div> getContent() {
        return content;
    }

    /**
     * 获取元素尺寸序列
     *
     * @return 段内元素尺寸序列
     */
    public List<Rectangle> getSizeList() {
        return sizeList;
    }


    /**
     * Foreach 循环技术器
     */
    private int cnt = 0;

    @Override
    public Iterator<Map.Entry<Div, Rectangle>> iterator() {
        return this;
    }

    @Override
    public boolean hasNext() {
        if (cnt < content.size()) {
            return true;
        }
        cnt = 0;
        return false;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    public double getWidth() {
        return width;
    }

    @Override
    public Map.Entry<Div, Rectangle> next() {
        if (cnt == content.size())
            throw new NoSuchElementException();
        cnt++;
        return new HashMap.SimpleEntry<>(
                content.get(cnt - 1), sizeList.get(cnt - 1)
        );
    }
}
