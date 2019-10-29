package org.ofdrw.core.basicStructure.pageObj.layer;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;

/**
 * 用于表示页块类型的接口
 * <p>
 * 逻辑层面表示
 *
 * @author 权观宇
 * @since 2019-10-10 10:12:35
 */
public interface PageBlockType extends Element {

    /**
     * 解析元素并获取对应的PageBlock子类实例
     *
     * @param element 实例
     * @return 子类实例
     * @throws IllegalArgumentException 未知的元素类型不是 PageBlock子类
     */
    static PageBlockType getInstance(Element element) {
        String qName = element.getQualifiedName();
        CT_PageBlock res = null;
        switch (qName) {
            case "ofd:TextObject":
                // TODO 2019-10-10 20:22:35 TextObject
                break;
            case "ofd:PathObject":
                // TODO 2019-10-10 20:26:36 PathObject
                break;
            case "ofd:ImageObject":
                // TODO 2019-10-10 20:26:51 ImageObject
                break;
            case "ofd:CompositeObject":
                // TODO 2019-10-10 20:27:11 CompositeObject
                break;
            case "ofd:PageBlock":
                res = new CT_PageBlock(element);
                break;
            case "ofd:Layer":
                res = new CT_Layer(element);
                break;
            default:
                throw new IllegalArgumentException("不是 PageBlock 子类，未知元素类型：" + qName);
        }
        return res;
    }
}
