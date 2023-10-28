package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.layout.element.Img;
import org.ofdrw.layout.engine.ResManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片渲染对象
 * <p>
 * {@link org.ofdrw.layout.element.Img} 的渲染器
 *
 * @author 权观宇
 * @since 2020-03-22 13:17:52
 */
public class ImgRender {

    /**
     * 渲染图片对象
     * <p>
     * 由于图片对象有图片资源所以需要放入到文档容器中
     *
     * @param layer      图片将要放置的图层
     * @param resManager 资源管理器
     * @param e          图片对象
     * @param maxUnitID  最大元素ID提供器
     */
    public static void render(CT_PageBlock layer, ResManager resManager, Img e, AtomicInteger maxUnitID) {
        if (e == null) {
            return;
        }
        // 图片存储路径
        Path p = e.getSrc();
        if (p == null || Files.notExists(p)) {
            throw new IllegalArgumentException("图片对象(Img)路径非法");
        }
        // 加入图片资源
        ST_ID id = null;
        try {
            id = resManager.addImage(p);
        } catch (IOException ex) {
            throw new RenderException("渲染图片复制失败：" + ex.getMessage(), ex);
        }
        // 在公共资源中加入图片
        ImageObject imgObj = new ImageObject(maxUnitID.incrementAndGet());
        imgObj.setResourceID(id.ref());
        double x = e.getX() + e.getMarginLeft() + e.getBorderLeft() + e.getPaddingLeft();
        double y = e.getY() + e.getMarginTop() + e.getBorderTop() + e.getPaddingTop();
        imgObj.setBoundary(x, y, e.getWidth(), e.getHeight());
        imgObj.setCTM(new ST_Array(e.getWidth(), 0, 0, e.getHeight(), 0, 0));
        if (e.getOpacity() != null) {
            // 图元透明度
            imgObj.setAlpha((int) (e.getOpacity() * 255));
        }
        layer.addPageBlock(imgObj);
    }
}
