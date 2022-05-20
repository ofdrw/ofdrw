package org.ofdrw.layout.edit;

import org.ofdrw.core.basicStructure.pageObj.Content;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
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
     * 创建增量虚拟页面
     *
     * @param pageObj 需要增量的页面对象
     */
    public AdditionVPage(Page pageObj) {
        if (pageObj == null) {
            throw new IllegalArgumentException("页面对象（pageObj）不能为空");
        }
        this.pageObj = pageObj;
    }

    private AdditionVPage(){}

    public Page getPageObj() {
        return pageObj;
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
}
