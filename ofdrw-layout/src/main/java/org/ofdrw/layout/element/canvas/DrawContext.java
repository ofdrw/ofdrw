package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.layout.engine.ResManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 绘制器绘制上下文
 * <p>
 * 上下文中提供系列的绘制方法供绘制
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
    private AbbreviatedData pathData = null;

    /**
     * RGB 描边颜色
     */
    private int[] strokeColor;


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
     * 开启一段新的路径
     *
     * 如果已经存在路径，那么将会关闭已经存在的路径
     *
     * @return this
     */
    public DrawContext beginPath(){
        // 如果已经存在路径，那么关闭路径
        if (this.pathData != null) {
            closePath();
        }
        // TODO 2020-5-1 21:47:09

        return this;
    }


    /**
     * 关闭路径 绘制图形
     *
     * @return this
     */
    public DrawContext closePath(){
        // TODO 2020-5-1 21:47:00

        return this;
    }

}
