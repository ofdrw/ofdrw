package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.Rectangle;
import org.ofdrw.layout.element.Div;

/**
 * 画板
 * <p>
 * 用于更加自由的向页面中加入内容
 * <p>
 * 绘制行为详见渲染器：{@link org.ofdrw.layout.engine.render.CanvasRender}
 *
 * @author 权观宇
 * @since 2020-05-01 11:04:46
 */
public class Canvas extends Div<Canvas> {

    /**
     * 绘制器
     */
    private Drawer drawer;

    /**
     * 优先使用的页面块
     * <p>
     * Canvas 生成的所有图元都将存储与该区块中
     * <p>
     * 默认为：null，表示不指定，每次绘制都会创建新的页面块。
     */
    private CT_PageBlock preferBlock;

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
        this.preferBlock = null;
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

    /**
     * 在指定位置 创建Canvas对象
     *
     * @param x 画布左上角的x坐标
     * @param y 画布左上角的y坐标
     * @param w 画布的宽度
     * @param h 画布的高度
     */
    public Canvas(double x, double y, double w, double h) {
        super(x, y, w, h);
    }

    /**
     * 创建Canvas对象
     *
     * @param style 页面样式属性
     */
    public Canvas(PageLayout style) {
        this(style.getWidth(), style.getHeight());
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

    /**
     * 获取 优先使用的页面块
     * <p>
     * Canvas 生成的所有图元都将存储与该区块中，通常在渲染完成后可以获得非空的页面块。
     *
     * @return 优先使用的页面块，null。
     */
    public CT_PageBlock getPreferBlock() {
        return preferBlock;
    }

    /**
     * 设置 优先使用的页面块
     * <p>
     * Canvas 生成的所有图元都将存储与该区块中
     * <p>
     * 注意：该方法具有一定危险性，若您不清楚该方法的作用，请勿使用。
     *
     * @param preferBlock 页面块
     */
    public void setPreferBlock(CT_PageBlock preferBlock) {
        this.preferBlock = preferBlock;
    }

    /**
     * 获取元素类型
     * <p>
     * 关联绘制器：{@link org.ofdrw.layout.engine.render.CanvasRender}
     *
     * @return Canvas
     */
    @Override
    public String elementType() {
        return "Canvas";
    }
}
