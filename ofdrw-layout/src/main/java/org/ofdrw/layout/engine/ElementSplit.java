package org.ofdrw.layout.engine;

import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.Div;

/**
 * 元素分割
 *
 * @author 权观宇
 * @since 2020-03-08 01:37:35
 */
public interface ElementSplit {
    /**
     * 根据给定的空间分割元素
     *
     * @param area 空间大小
     * @return 根据给定空间分割之后的元素
     */
    Div[] split(Rectangle area);
}
