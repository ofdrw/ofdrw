package org.ofdrw.layout;

/**
 * 渲染准备
 * <p>
 * 在渲染前完成元素初步布局和测量以及缓存等工作
 *
 * @author 权观宇
 * @since 2020-02-28 04:02:47
 */
public interface RenderPrepare {
    /**
     * 执行渲染前的准备工作
     * <p>
     * 包括简单的内部布局，必要数据的缓存
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    Rectangle doPrepare(Double widthLimit);
}
