package org.ofdrw.layout.element;

/**
 * 定位方式
 *
 * @author 权观宇
 * @since 2020-02-28 01:40:26
 */
public enum Position {
    /**
     * 静态定位（由渲染器决定）
     * <p>
     * 默认值
     */
    Static,
    /**
     * 相对定位
     * <p>
     * 使用： left、right在段内定位
     */
    Relative,
    /**
     * 绝对定位
     * <p>
     * 使用：x、y 确定位置
     * 并且需要有确定的 Width 和 Height
     */
    Absolute,
}
