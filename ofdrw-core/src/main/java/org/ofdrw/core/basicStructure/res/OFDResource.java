package org.ofdrw.core.basicStructure.res;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.res.resources.*;

/**
 * 资源文件抽象类型
 * <p>
 * 用于代指：绘制参数、颜色空间、字形、图像、音视频等资源的都为资源类型。
 *
 * @author 权观宇
 * @since 2019-10-11 06:07:07
 */
public interface OFDResource extends Element {

    /**
     * 解析元素并获取对应资源类型子类实例
     *
     * @param element 实例
     * @return 子类实例
     * @throws IllegalArgumentException 未知的元素类型不是 OFDResource子类
     */
    static OFDResource getInstance(Element element) {
        String qName = element.getQualifiedName();
        OFDResource res = null;
        switch (qName) {
            case "ofd:ColorSpaces":
            case "ColorSpaces":
                res = new ColorSpaces(element);
                break;
            case "ofd:DrawParams":
            case "DrawParams":
                res = new DrawParams(element);
                break;
            case "ofd:Fonts":
            case "Fonts":
                res = new Fonts(element);
                break;
            case "ofd:MultiMedias":
            case "MultiMedias":
                res = new MultiMedias(element);
                break;
            case "ofd:CompositeGraphicUnits":
            case "CompositeGraphicUnits":
                res = new CompositeGraphicUnits(element);
                break;
            default:
                if (qName.toLowerCase().contains("draw")) {
                    res = new DrawParams(element);
                } else if (qName.toLowerCase().contains("font")) {
                    res = new Fonts(element);
                } else if (qName.toLowerCase().contains("color")) {
                    res = new ColorSpaces(element);
                } else {
                    throw new IllegalArgumentException("不是 Res的子类，未知元素类型：" + qName);
                }
        }
        return res;
    }
}
