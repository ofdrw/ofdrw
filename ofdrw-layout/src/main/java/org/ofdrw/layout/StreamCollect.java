package org.ofdrw.layout;

import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.Segment;
import org.ofdrw.layout.engine.SegmentationEngine;
import org.ofdrw.layout.engine.StreamingLayoutAnalyzer;

import java.util.LinkedList;
import java.util.List;

/**
 * 流式布局的页面内容
 * <p>
 * 用于编辑文档时插入文档内容
 * <p>
 * 如果内容超过一页，那么分页并添加新的一页在原页面后面
 *
 * @author 权观宇
 * @since 2021-06-11 19:26:00
 */
public class StreamCollect {
    /**
     * 开始页面位置
     */
    private Integer pageNum = null;
    /**
     * 流式页面内容
     */
    private LinkedList<Div> content = new LinkedList<>();

    public StreamCollect() {
    }

    /**
     * 创建插入页面流
     *
     * @param pageNum 页码（从1开始）
     */
    public StreamCollect(Integer pageNum) {
        this.pageNum = pageNum;
    }

    /**
     * 获取内容队列
     *
     * @return 内容队列
     */
    public LinkedList<Div> getContent() {
        return content;
    }

    public StreamCollect add(Div element) {
        if (element != null) {
            this.content.add(element);
        }
        return this;
    }

    /**
     * 获取插入页码
     *
     * @return 页码（从1开始）
     */
    public Integer getPageNum() {
        return pageNum;
    }

    /**
     * 设置 插入页码
     *
     * @param pageNum 页码（从1开始）
     * @return this
     */
    public StreamCollect setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
        return this;
    }

    /**
     * 分析流式内容 转换为 虚拟页面
     *
     * @param pageLayout 页面布局信息
     * @return 虚拟页面集合
     */
    public List<VirtualPage> analyze(PageLayout pageLayout) {
        SegmentationEngine sgmEngine = new SegmentationEngine(pageLayout);
        StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(pageLayout);
        // 流式布局队列经过分段引擎，获取分段队列
        List<Segment> sgmQueue = sgmEngine.process(this.content);
        //段队列进入布局分析器，构造基于固定布局的虚拟页面。
        List<VirtualPage> virtualPageList = analyzer.analyze(sgmQueue);
        if (this.pageNum != null) {
            int start = this.pageNum;
            for (VirtualPage vPage : virtualPageList) {
                vPage.setPageNum(start);
                start++;
            }
        }
        return virtualPageList;
    }
}
