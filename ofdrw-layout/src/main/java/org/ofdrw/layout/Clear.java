package org.ofdrw.layout;

/**
 * 对段的占用情况
 *  @author 权观宇
  * @since 2020-02-03 01:01:38
 */
public enum Clear {
    /**
     * 共享: 两侧都允许出现元素。
     * <p>
     * 默认值
     */
    none,
    /**
     * 左侧不允许出现元素
     */
    left,
    /**
     * 右侧不允许出现元素
     */
    right,
    /**
     * 两侧不允许出现元素
     */
    both;
}
