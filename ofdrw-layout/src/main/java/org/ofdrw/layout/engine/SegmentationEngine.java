package org.ofdrw.layout.engine;

import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.element.BR;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Position;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * 分段引擎
 * <p>
 * 用于将流式文档中的元素划分为段。
 *
 * @author 权观宇
 * @since 2020-02-29 11:39:29
 */
public class SegmentationEngine {

    private PageLayout pageLayout;

    private SegmentationEngine() {
    }

    public SegmentationEngine(PageLayout pageLayout) {
        this.pageLayout = pageLayout;
    }

    /**
     * 将输入的流式布局元素队列分段
     *
     * @param streamLayoutQueue 流式布局元素队列
     * @return 分完段的布局队列
     */
    public List<Segment> process(List<Div> streamLayoutQueue) {
        // 可用于布局的宽度
        double width = pageLayout.contentWidth();
        if (streamLayoutQueue == null || streamLayoutQueue.isEmpty()) {
            return Collections.emptyList();
        }
        LinkedList<Segment> res = new LinkedList<>();
        LinkedList<Div> seq = new LinkedList<>(streamLayoutQueue);
        Segment segment = new Segment(width);
        while (!seq.isEmpty()) {
            Div div = seq.pop();
            if (div.getPosition() == Position.Absolute) {
                continue;
            }
            if (div instanceof BR) {
                if (segment.isEmpty()) {
                    // 如果段为空，那么不需要换行
                    continue;
                }
                // 换行符
                res.add(segment);
                segment = new Segment(width);
                continue;
            }
            // 尝试将元素加入段中
            boolean addSuccess = segment.tryAdd(div);
            // 如果段已经满了，那么加入了段队列中
            if (!addSuccess && !segment.isEmpty()) {
                // 段已经无法再容纳元素： 无法加入元素且不为空
                res.add(segment);
                segment =  new Segment(width);
                seq.push(div);
            }
        }
        // 处理最后一个段的情况
        if (!segment.isEmpty()) {
            res.add(segment);
        }
        return res;
    }
}
