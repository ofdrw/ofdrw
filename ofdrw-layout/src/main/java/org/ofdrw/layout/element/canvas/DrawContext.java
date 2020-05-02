package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.layout.engine.ResManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 绘制器绘制上下文
 * <p>
 * 上下文中提供系列的绘制方法供绘制
 * <p>
 * 一个路径对象只允许出现一种描边和填充颜色
 * 重复设置，取最后一次设置的颜色。
 *
 * @author 权观宇
 * @since 2020-05-01 11:29:20
 */
public class DrawContext {

    /**
     * 用于容纳所绘制的所有图像的容器
     */
    private CT_PageBlock container;

    /**
     * 对象ID提供器
     */
    private AtomicInteger maxUnitID;

    /**
     * 资源管理器
     */
    private ResManager resManager;


    /**
     * 路径数据
     */
    private CT_Path pathData = null;

    /**
     * 描边RGB颜色
     */
    private int[] strokeColor;

    /**
     * 填充RGB颜色
     */
    private int[] fillColor;


    private DrawContext() {
    }

    /**
     * 创建绘制上下文
     *
     * @param container  绘制内容缩所放置容器
     * @param maxUnitID  自增的对象ID
     * @param resManager 资源管理器
     */
    public DrawContext(CT_PageBlock container,
                       AtomicInteger maxUnitID,
                       ResManager resManager) {
        this.container = container;
        this.maxUnitID = maxUnitID;
        this.resManager = resManager;
    }


    /**
     * 根据上下文属性创建一个Path对象
     *
     * @return 路径对象
     */
    private CT_Path newPathWithCtx() {
        CT_Path path = new CT_Path();
        // 设置描边颜色
        if (this.strokeColor != null) {
            path.setStroke(true)
                    .setStrokeColor(CT_Color.rgb(this.strokeColor.clone()));
        }
        // 设置填充颜色
        if (this.fillColor != null) {
            path.setFill(true)
                    .setFillColor(CT_Color.rgb(this.fillColor.clone()));
        }
        return path;
    }

    /**
     * 开启一段新的路径
     * <p>
     * 如果已经存在路径，那么将会关闭已经存在的路径
     *
     * @return this
     */
    public DrawContext beginPath() {
        // 如果已经存在路径，那么关闭路径
        if (this.pathData != null) {
            closePath();
        }
        this.pathData = newPathWithCtx();
        return this;
    }


    /**
     * 关闭路径
     * <p>
     * 如果路径存在描边或者填充，那么改路径将会被加入到图形容器中进行渲染
     * <p>
     * 路径关闭后将会清空上下文中的路径对象
     *
     * @return this
     */
    public DrawContext closePath() {
        if (this.pathData == null) {
            return this;
        }
        // 如果路径存在描边或者填充，那么改路径将会被加入到图形容器中进行渲染
        if (pathData.getFill() || pathData.getStroke()) {
            // 创建路径对象，放入图形容器中
            container.addPageBlock(pathData.toObj(new ST_ID(maxUnitID.incrementAndGet())));
        }
        this.pathData = null;
        return this;
    }


    /**
     * 读取当前描边颜色（只读）
     *
     * @return 描边颜色（只读）
     */
    public int[] getStrokeColor() {
        return strokeColor.clone();
    }

    /**
     * 设置描边颜色
     * <p>
     * 一条路径只有一种描边颜色，重复设置只取最后一次设置颜色
     *
     * @param strokeColor 描边的RGB颜色
     * @return this
     */
    public DrawContext setStrokeColor(int[] strokeColor) {
        if (strokeColor == null) {
            return this;
        }

        this.strokeColor = strokeColor;
        if (this.pathData != null) {
            this.pathData.setStroke(true)
                    .setStrokeColor(CT_Color.rgb(strokeColor));
        }
        return this;
    }

    /**
     * 设置描边颜色
     * <p>
     * 一条路径只有一种描边颜色，重复设置只取最后一次设置颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 蓝
     * @return this
     */
    public DrawContext setStrokeColor(int r, int g, int b) {
        return setStrokeColor(new int[]{r, g, b});
    }

    /**
     * 获取填充颜色（只读）
     *
     * @return 填充颜色（只读）
     */
    public int[] getFillColor() {
        return fillColor.clone();
    }

    /**
     * 设置填充颜色
     * <p>
     * 一条路径只有一种填充颜色，重复设置只取最后一次设置颜色
     *
     * @param fillColor 填充颜色
     * @return this
     */
    public DrawContext setFillColor(int[] fillColor) {
        if (fillColor == null) {
            return this;
        }

        this.fillColor = fillColor;
        if (this.pathData != null) {
            this.pathData.setFill(true)
                    .setFillColor(CT_Color.rgb(strokeColor));
        }
        return this;
    }

    /**
     * 设置填充颜色
     * <p>
     * 一条路径只有一种填充颜色，重复设置只取最后一次设置颜色
     *
     * @param r 红
     * @param g 绿
     * @param b 蓝
     * @return this
     */
    public DrawContext setFillColor(int r, int g, int b) {
        return setFillColor(new int[]{r, g, b});
    }
}
