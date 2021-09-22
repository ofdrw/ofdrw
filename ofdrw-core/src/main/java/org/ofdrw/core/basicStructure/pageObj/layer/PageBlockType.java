package org.ofdrw.core.basicStructure.pageObj.layer;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.block.*;

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
        PageBlockType res = null;
        switch (qName) {
            case "ofd:TextObject":
            case "TextObject":
                res = new TextObject(element);
                break;
            case "ofd:PathObject":
            case "PathObject":
                res = new PathObject(element);
                break;
            case "ofd:ImageObject":
            case "ImageObject":
                res = new ImageObject(element);
                break;
            case "ofd:CompositeObject":
            case "CompositeObject":
                res = new CompositeObject(element);
                break;
            case "ofd:PageBlock":
            case "PageBlock":
                res = new CT_PageBlock(element);
                break;
            case "ofd:Layer":
            case "Layer":
                res = new CT_Layer(element);
                break;
            default:
                throw new IllegalArgumentException("不是 PageBlock 子类，未知元素类型：" + qName);
        }
        return res;
    }
}
