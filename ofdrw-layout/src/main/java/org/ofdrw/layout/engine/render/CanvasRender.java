package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.layout.element.canvas.Canvas;
import org.ofdrw.layout.element.canvas.DrawContext;
import org.ofdrw.layout.element.canvas.Drawer;
import org.ofdrw.layout.engine.ResManager;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Canvas 渲染器
 *
 * @author 权观宇
 * @since 2020-05-01 17:44:41
 */
public class CanvasRender {

    /**
     * 执行Canvas 渲染器
     *
     * @param layer      将要渲染到的图层
     * @param resManager 资源管理器
     * @param canvas     canvas对象
     * @param maxUnitID  最大ID
     * @throws RenderException 渲染发生错误
     */
    public static void render(CT_PageBlock layer,
                              ResManager resManager,
                              Canvas canvas,
                              AtomicInteger maxUnitID) throws RenderException {
        // 获取绘制器
        Drawer drawer = canvas.getDrawer();
        if (drawer == null) {
            return;
        }

        CT_PageBlock block = new CT_PageBlock();
        block.setObjID(maxUnitID.incrementAndGet());
        layer.addPageBlock(block);

        ST_Box boundary = new ST_Box(canvas.getX(), canvas.getY(), canvas.getWidth(), canvas.getHeight());
        // 构建上下文
        try (DrawContext ctx = new DrawContext(block, boundary, maxUnitID, resManager)) {
            // 执行绘制工作
            drawer.draw(ctx);
        } catch (IOException e) {
            throw new RenderException("Canvas绘制过程中发生异常", e);
        }
    }
}

