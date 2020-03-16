package org.ofdrw.layout.element;

/**
 * 页面剩余空间填充
 * <p>
 * 该元素不会被布局分析器解析，只是作为一个命令标志，
 * 告诉分析器使剩余的空间为0，也就是构造一个特殊的段
 *
 * @author 权观宇
 * @since 2020-03-16 10:52:21
 */
public class PageAreaFiller extends Div {
    public PageAreaFiller() {
        setPlaceholder(true);
        setClear(Clear.both);
        setFloat(AFloat.center);
    }
}
