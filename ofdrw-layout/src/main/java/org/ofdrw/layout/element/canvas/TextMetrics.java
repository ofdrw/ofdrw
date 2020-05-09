package org.ofdrw.layout.element.canvas;

/**
 * 测量的文字信息
 *
 * @author 权观宇
 * @since 2020-05-07 19:03:17
 */
public class TextMetrics {

    /**
     * 阅读方向
     */
    public int readDirection;

    /**
     * 文字宽度
     * <p>
     * 如果 readDirection == 0 || 180 为宽度
     * <p>
     * 如果 readDirection == 90 || 270 为高度
     */
    public Double width;
}
