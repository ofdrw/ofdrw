package org.ofdrw.layout.edit;

import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.layout.VirtualPage;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 专用于向页面已有页面中添加内容的虚拟页面
 *
 * @author 权观宇
 * @since 2020-04-07 20:15:51
 */
public class AdditionVPage extends VirtualPage {

    /**
     * 反序列化后的页面对象
     */
    private Page pageObj;

    /**
     * 页面对象在容器中的绝对路径（以 "/" 开头）
     * <p>
     * 考虑到兼容性 该字段可能为空。
     * <p>
     * 该字段将于 区域占位块 AreaHolderBlock 定位。
     */
    private ST_Loc pageLoc;

    /**
     * 已经过时，仅为兼容行保留，使用 {@link #AdditionVPage(Page, ST_Loc)}
     *
     * @param pageObj 页面对象
     * @deprecated {@link #AdditionVPage(Page, ST_Loc)}
     */
    @Deprecated
    public AdditionVPage(Page pageObj) {
        this(pageObj, null);
    }

    /**
     * 创建一个向页面中添加内容的虚拟页面
     *
     * @param pageObj 页面对象
     * @param pageLoc 页面对象在容器中的绝对路径（以 "/" 开头）
     */
    public AdditionVPage(Page pageObj, ST_Loc pageLoc) {
        if (pageObj == null) {
            throw new IllegalArgumentException("页面对象（pageObj）不能为空");
        }
        this.pageObj = pageObj;
        this.pageLoc = pageLoc;
    }

    private AdditionVPage() {
    }



    /**
     * 获取处于上层的图层
     * <p>
     * 使用最上方的图层防止，增加的内容被覆盖
     * <p>
     * 如果含有多个相同图层那么选取位置靠后的图层
     * <p>
     * 如果页面中没有任何图层，那么创建一个图层
     *
     * @param maxUnitID 如果需要创建图层，那么给与图层对象ID
     * @return 处于页面最上层的图层对象
     */
    public CT_Layer obtainTopLayer(AtomicInteger maxUnitID) {
        CT_Layer topLayer = null;
        // 默认为最底层
        int currentOrder = Type.Background.order();
        Content content = pageObj.getContent();
        if (content == null) {
            // 页面那种没有任何图层的情况下，创建一个新的图层加入到页面
            topLayer = new CT_Layer();
            topLayer.setObjID(maxUnitID.incrementAndGet());
            content = new Content().addLayer(topLayer);
            pageObj.setContent(content);
            return topLayer;
        }
        List<CT_Layer> layers = content.getLayers();
        if (layers == null) {
            throw new RuntimeException("页面对象无法（Content）解析，图层对象不能为空");
        }

        for (CT_Layer layer : layers) {
            if (layer == null) {
                continue;
            }
            Type type = layer.getType();
            if (type.order() >= currentOrder) {
                topLayer = layer;
            }
        }
        return topLayer;
    }

    /**
     * 创建新的层
     *
     * @param maxUnitID 如果需要创建图层，那么给与图层对象ID
     * @return 新的图层
     */
    public CT_Layer newLayer(AtomicInteger maxUnitID) {
        CT_Layer res = null;
        Content content = pageObj.getContent();
        if (content == null) {
            content = new Content();
            pageObj.setContent(content);
            return res;
        }
        res = new CT_Layer();
        res.setObjID(maxUnitID.incrementAndGet());
        content.addLayer(res);
        return res;
    }

    /**
     * 获取页面对象
     * @return 页面对象
     */
    public Page getPageObj() {
        return pageObj;
    }

    /**
     * 获取 页面的绝对路径（注意可能为空）
     *
     * @return 页面绝对路径，可能为空。
     */
    public ST_Loc getPageLoc() {
        return pageLoc;
    }

    /**
     * 设置 页面的绝对路径
     *
     * @param pageLoc 页面绝对路径
     */
    public void setPageLoc(ST_Loc pageLoc) {
        this.pageLoc = pageLoc;
    }
}
