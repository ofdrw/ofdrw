package org.ofdrw.layout.element;

/**
 * Div在行内的表现形式
 *
 * @author 权观宇
 * @since 2023-11-22 18:37:13
 */
public enum Display {

    /**
     * 块级元素
     * <p>
     * 该元素生成一个块级元素盒，在正常的流中，该元素之前和之后产生换行。
     */
    block,
    /**
     * 内联块级元素
     * <p>
     * 它在正常的流中不产生换行，除非它的前一个元素是块级元素盒。
     */
    inlineBlock,
}
