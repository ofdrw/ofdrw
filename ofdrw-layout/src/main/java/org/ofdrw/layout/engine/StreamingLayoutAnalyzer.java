package org.ofdrw.layout.engine;

import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

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
        newPage();
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
            // TODO 分段
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
        List<Div> content = segment.getContent();
        // 元素尺寸便于分析
        List<Rectangle> elementSizes = new ArrayList<>(content.size());
        // 分析元素尺寸并缓存，为下一步规划空间使用做准备
        content.forEach((item) -> elementSizes.add(item.reSize(area.getWidth())));
        // 将流式的自动定位转为绝对定位位置的Div
        for (int i = 0, len = content.size(); i < len; i++) {
            Div itemDiv = content.get(i);
            Rectangle box = elementSizes.get(i);
            // 将流式的自动定位转为绝对定位位置的Div
            itemDiv.setPosition(Position.Absolute)
                    .setWidth(box.getWidth())
                    .setHeight(box.getHeight())
                    .setY(area.getY());
        }

        // 居中的布局分析
        if (segment.isCenterFloat()) {
            // 获取段内所有元素的宽度
            double totalWidth = elementSizes.stream().mapToDouble(Rectangle::getWidth).sum();
            // 第一个元素偏移的X坐标
            double offsetX = area.getX() + ((area.getWidth() - totalWidth) / 2);
            for (int i = 0, len = content.size(); i < len; i++) {
                Div itemDiv = content.get(i);
                Rectangle box = elementSizes.get(i);
                itemDiv.setX(offsetX);
                // 增加偏移量计算出下一个box应该出现的位置
                offsetX += box.getWidth();
            }
        } else {
            // 左右浮动的分析
            double startX = area.getX();
            double endX = startX + area.getWidth();
            for (int i = 0, len = content.size(); i < len; i++) {
                Div itemDiv = content.get(i);
                Rectangle box = elementSizes.get(i);
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
        content.forEach(vPage::add);
    }

    private void newPage() {
        // 重置工作区域
        remainArea = this.pageWorkArea.clone();
        vPage = new VirtualPage(layout);
    }

}
