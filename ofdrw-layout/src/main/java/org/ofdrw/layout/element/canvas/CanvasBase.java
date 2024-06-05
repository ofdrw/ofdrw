package org.ofdrw.layout.element.canvas;

import org.ofdrw.layout.PageLayout;

/**
 * Canvas扩展基类，用于快速构建基于Canvas扩展的自定义元素。
 * <p>
 * 通过CanvasBase类，可以简化Canvas的创建过程，实现者需要实现 {@link Drawer} 接口，
 * 并在{@link Drawer#draw(DrawContext)} 实现自定义的绘制逻辑。
 * <p>
 * 一个最简单扩展示例可参考 {@link Line}。
 *
 * @author 权观宇
 * @since 2024-6-5 18:45:49
 */
public abstract class CanvasBase extends Canvas implements Drawer {

    public CanvasBase(Double width, Double height) {
        super(width, height);
        setDrawer(this);
    }

    public CanvasBase(double x, double y, double w, double h) {
        super(x, y, w, h);
        setDrawer(this);
    }

    public CanvasBase(PageLayout style) {
        super(style);
        setDrawer(this);
    }
}
