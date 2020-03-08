package org.ofdrw.layout.engine;

import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.*;

/**
 * 流式布局分析器
 *
 * @author 权观宇
 * @since 2020-03-07 12:08:41
 */
public class StreamingLayoutAnalyzer {

    /**
     * 页面的固定可用的工作区域
     */
    private Rectangle pageWorkArea;

    /**
     * 虚拟页面的尺寸
     */
    private PageLayout layout;

    /**
     * 布局最终产生的虚拟页面序列
     */
    private LinkedList<VirtualPage> vPageList;

    /**
     * 当前剩余工作区域
     */
    private Rectangle remainArea;

    /**
     * 当前布局中的虚拟页面
     */
    private VirtualPage vPage;

    public StreamingLayoutAnalyzer(PageLayout layout) {
        pageWorkArea = layout.getWorkerArea();
        this.layout = layout;
        vPageList = new LinkedList<>();
    }

    /**
     * 进行段的布局分析
     *
     * @param segmentSequence 段序列
     * @return 虚拟页面序列
     */
    List<VirtualPage> analyze(List<Segment> segmentSequence) {
        if (segmentSequence == null || segmentSequence.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedList<Segment> seq = new LinkedList<>(segmentSequence);
        // 初始化页面
        addNewPage();
        while (!seq.isEmpty()) {
            Segment segment = seq.pop();
            // 高度足够能够放入剩余空间中
            if (segment.getHeight() <= remainArea.getHeight()) {
                // 分配段空间
                Rectangle area = remainArea.reduce(segment.getHeight());
                // 定位元素，加入虚拟页面
                elementPositioning(segment, area);
                continue;
            }
            // 判断段是否可以拆分
            if (segment.isBlockable() == false) {
                if (segment.getHeight() > pageWorkArea.getWidth()) {
                    // 如果段不可拆分，并且高度大于整个页面的高度，那么这样的段应该舍弃
                } else {
                    // 如果段不可拆分，并且高度小于整个页面的高度，那么新起一个页面，重新加入队列布局
                    addNewPage();
                    seq.push(segment);
                }
                continue;
            }
            // 段可以拆分，那么通过剩余空间对段分块，这个分块只会分为两块
            List<Segment> blocks = segmentBlocking(segment, remainArea.clone());
            // 因为是栈的原因，需要把最后一个元素最新压到栈顶。
            Collections.reverse(blocks);
            // 重新进入队列中
            blocks.forEach(seq::push);
        }
        return null;
    }

    /**
     * 由于剩余空间不足且段可以分块
     * <p>
     * 对段进行分块，剩余的块将流转到下一个页面的段中
     *
     * @param segment    段
     * @param area 剩余空间
     * @return 分块后的段序列
     */
    private List<Segment> segmentBlocking(Segment segment, Rectangle area) {
        // 分为两块

        Segment sgm1 = new Segment(segment.getWidth());
        Segment sgmNext = new Segment(segment.getWidth());
        for (Map.Entry<Div, Rectangle> item : segment) {
            Div div = item.getKey();
            Rectangle rec = item.getValue();
            // 1. 能够放置到剩余空间中
            if (rec.getHeight() <= area.getHeight()) {
                // 向第一个段，加入实际的Div
                sgm1.tryAdd(div);
                // 向第下一个段加入占位符
                sgmNext.tryAdd(Div.placeholder(rec));
                continue;
            }
            // 2. 在剩余空间中无法放置，且元素本身不可分割
            if(div.getIntegrity()){
                // 向第一个段中加入占位符
                sgm1.tryAdd(Div.placeholder(rec.getWidth(), area.getHeight()));
                // 把实际元素加入下一个段中
                sgmNext.tryAdd(div);
                continue;
            }
            // TODO 3. 无法在剩余空间中放置，且可以分割的情况，需要对元素进行分割
        }
        return null;
    }

    /**
     * 段中的元素定位，并加入到虚拟页面中
     *
     * @param segment 待定位的段
     * @param area    分配给该段的页面空间
     */
    private void elementPositioning(Segment segment, Rectangle area) {
        // 将流式的自动定位转为绝对定位位置的Div
        for (Map.Entry<Div, Rectangle> item : segment) {
            Div itemDiv = item.getKey();
            Rectangle box = item.getValue();
            // 将流式的自动定位转为绝对定位位置的Div
            itemDiv.setPosition(Position.Absolute)
                    .setWidth(box.getWidth())
                    .setHeight(box.getHeight())
                    .setY(area.getY());
        }
        // 居中的布局分析
        if (segment.isCenterFloat()) {
            // 获取段内所有元素的宽度
            double totalWidth = segment.getSizeList().stream().mapToDouble(Rectangle::getWidth).sum();
            // 第一个元素偏移的X坐标
            double offsetX = area.getX() + ((area.getWidth() - totalWidth) / 2);
            for (Map.Entry<Div, Rectangle> item : segment) {
                Div itemDiv = item.getKey();
                Rectangle box = item.getValue();
                itemDiv.setX(offsetX);
                // 增加偏移量计算出下一个box应该出现的位置
                offsetX += box.getWidth();
            }
        } else {
            // 左右浮动的分析
            double startX = area.getX();
            double endX = startX + area.getWidth();
            for (Map.Entry<Div, Rectangle> item : segment) {
                Div itemDiv = item.getKey();
                Rectangle box = item.getValue();
                AFloat aFloat = itemDiv.getFloat();
                if (aFloat == AFloat.left) {
                    itemDiv.setX(startX);
                    startX += box.getWidth();
                } else if (aFloat == AFloat.right) {
                    itemDiv.setX(endX - box.getWidth());
                    endX -= box.getWidth();
                }// ignore center and null
            }
        }
        // 加入到虚拟页面中
        segment.getContent()
                .stream()
                // 排除 空间占位符
                .filter(item -> !item.isPlaceholder())
                .forEach(vPage::add);
    }

    /**
     * 创建新页面
     */
    private void addNewPage() {
        // 重置工作区域
        remainArea = this.pageWorkArea.clone();
        vPage = new VirtualPage(layout);
        vPageList.add(vPage);
    }

}
