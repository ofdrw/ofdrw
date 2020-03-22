package org.ofdrw.layout.engine.render;

import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.layout.element.Img;
import org.ofdrw.pkg.dir.DocDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 图片渲染对象
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
     * @param layer     图片将要放置的图层
     * @param docDir    图片容器
     * @param e         图片对象
     * @param maxUnitID 最大元素ID提供器
     */
    public static void render(CT_Layer layer, DocDir docDir, Img e, AtomicInteger maxUnitID) {
        if (e == null) {
            return;
        }
        // 图片存储路径
        Path p = e.getSrc();
        if (p == null || Files.notExists(p)) {
            throw new IllegalArgumentException("图片对象(Img)路径非法");
        }
        // 初始化公共资源
        Res publicRes = docDir.getPublicRes();
        if (publicRes == null) {
            publicRes = new Res();
            docDir.setPublicRes(publicRes);
        }
        // 将图片加入到文档的公共资源中
        docDir.addResource(p);
        ImageObject imgObj = new ImageObject(maxUnitID.incrementAndGet());
        // TODO 调整图片元素属性

        layer.addPageBlock(imgObj);
    }
}
