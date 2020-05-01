package org.ofdrw.layout.element.canvas;

import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.Div;

/**
 * 画板
 * <p>
 * 用于更加自由的向页面中加入内容
 *
 * @author 权观宇
 * @since 2020-05-01 11:04:46
 */
public class Canvas extends Div {

    /**
     * 绘制器
     */
    private Drawer drawer;

    private Canvas() {
    }

    /**
     * 创建Canvas对象，并指定绘制器
     * <p>
     * Canvas的宽度和高度必须在创建时指定
     *
     * @param width  宽度（单位：毫米mm）
     * @param height 高度（单位：毫米mm）
     * @param drawer Canvas内容的绘制器
     */
    public Canvas(Double width, Double height, Drawer drawer) {
        super(width, height);
        this.drawer = drawer;
    }

    /**
     * 创建Canvas对象
     * <p>
     * Canvas的宽度和高度必须在创建时指定
     *
     * @param width  宽度（单位：毫米mm）
     * @param height 高度（单位：毫米mm）
     */
    public Canvas(Double width, Double height) {
        super(width, height);
    }


    public Drawer getDrawer() {
        return drawer;
    }


    /**
     * 在进入渲染器之前可以对Canvas的绘制进行重设
     *
     * @param drawer 新的绘制器
     * @return this
     */
    public Canvas setDrawer(Drawer drawer) {
        this.drawer = drawer;
        return this;
    }

    /**
     * Canvas 不接受宽度重设
     */
    @Override
    public Rectangle doPrepare(Double widthLimit) {
        double w = this.getWidth() + widthPlus();
        double h = this.getHeight() + heightPlus();
        return new Rectangle(w, h);
    }
}
