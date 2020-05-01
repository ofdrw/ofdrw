package org.ofdrw.layout.element.canvas;

import java.io.IOException;

/**
 * Canvas 内容绘制器
 * <p>
 * 用于绘制Canvas中的实际内容
 *
 * @author 权观宇
 * @since 2020-05-01 11:22:05
 */
@FunctionalInterface
public interface Drawer {

    /**
     * 绘制
     *
     * @param ctx 绘制上下文
     * @throws IOException 图片读取过程中IO异常
     */
    void draw(DrawContext ctx) throws IOException;
}
