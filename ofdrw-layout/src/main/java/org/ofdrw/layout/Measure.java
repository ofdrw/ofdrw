package org.ofdrw.layout;

/**
 * 大小测量接口，用于提供元素的大小尺寸
 *
 * @author 权观宇
 * @since 2020-02-28 04:02:47
 */
public interface Measure {
    /**
     * 根据限定重置尺寸，并返还尺寸样式
     *
     * @param widthLimit 宽度限制
     * @return 元素尺寸
     */
    Rectangle reSize(Double widthLimit);
}
