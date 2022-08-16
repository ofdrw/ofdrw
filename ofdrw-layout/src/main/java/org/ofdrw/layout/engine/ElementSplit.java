package org.ofdrw.layout.engine;

import org.ofdrw.layout.element.Div;

/**
 * 元素分割
 *
 * @author 权观宇
 * @since 2020-03-08 01:37:35
 */
public interface ElementSplit {
    /**
     * 根据给定的高度切分元素
     * <p>
     * 截断元素前必须确定元素的宽度和高度，否则将会抛出异常
     * <p>
     * 元素的分割只作用于竖直方向上，水平方向不做分割每次只会截断1次。
     * <p>
     * 截断的元素在截断出均无margin、border、padding
     * <p>
     * 截断后的内容比截断高度高的多
     *
     * @param sHeight   切分高度
     * @return 根据给定空间分割之后的新元素
     */
    Div[] split(double sHeight);
}
