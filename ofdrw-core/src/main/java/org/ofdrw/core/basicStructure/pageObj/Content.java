package org.ofdrw.core.basicStructure.pageObj;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_Layer;
import org.ofdrw.core.basicType.ST_ID;

import java.util.ArrayList;
import java.util.List;

/**
 * 页面内容描述，该节点不存在是，表示空白页面
 * <p>
 * 7.7 页面对象 表 12
 *
 * @author 权观宇
 * @since 2019-10-10 09:55:20
 */
public class Content extends OFDElement {
    public Content(Element proxy) {
        super(proxy);
    }

    public Content() {
        super("Content");
    }

    // TODO 2019-10-10 21:57:22 测试

    /**
     * 【必选】
     * 增加 层节点
     * <p>
     * 一页可以包含一个或多个层
     * <p>
     * 注意：每个加入的层节点必须设置 ID属性。
     *
     * @param layer 层节点
     * @return this
     * @throws IllegalArgumentException 加入的图层对象（CT_Layer）没有设置ID属性
     */
    public Content addLayer(CT_Layer layer) {
        ST_ID id = layer.getObjID();
        if (id == null) {
            throw new IllegalArgumentException("加入的图层对象（CT_Layer）没有设置ID属性");
        }
        this.add(layer);
        return this;
    }

    /**
     * 【必选】
     * 获取 层节点列表
     * <p>
     * 一页可以包含一个或多个层
     * <p>
     * 注意：每个加入的层节点必须设置 ID属性。
     *
     * @return 层节点
     */
    public List<CT_Layer> getLayers() {
        List<Element> elements = this.getOFDElements("Layer");
        List<CT_Layer> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new CT_Layer(item)));
        return res;
    }
}
