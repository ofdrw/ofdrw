package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.engine.ResManager;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * 元素绘制器
 * <p>
 * 用于实现OFDRW元素到OFD图元的转换，并处理OFD虚拟容器以及资源管理。
 * <p>
 * 绘制器的选择由 {@link org.ofdrw.layout.engine.VPageParseEngine} 实现，您需要向 VPageParseEngine 通过名称注册绘制器。
 *
 * @author 权观宇
 * @since 2024-5-27 19:22:48
 */
@FunctionalInterface
public interface Processor {

    /**
     * 处理OFDRW元素转换为OFD元素
     *
     * @param pageLoc    页面路径
     * @param layer      图片将要放置的图层
     * @param resManager 资源管理器
     * @param e          OFDRW元素
     * @param maxUnitID  最大元素ID提供器
     * @throws RenderException 渲染发生错误
     */
    public void render(ST_Loc pageLoc, CT_PageBlock layer, ResManager resManager, Div e, AtomicInteger maxUnitID) throws RenderException;
}
