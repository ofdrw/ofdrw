package org.ofdrw.core.basicStructure.res;

import org.dom4j.Element;

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
                // TODO 2019-10-11 18:10:13 ColorSpaces
                break;
            case "ofd:DrawParams":
                // TODO 2019-10-11 18:10:25 DrawParams
                break;
            case "ofd:Fonts":
                // TODO 2019-10-11 18:10:36 Fonts
                break;
            case "ofd:MultiMedias":
                // TODO 2019-10-11 18:10:55 MultiMedias
                break;
            case "ofd:CompositeGraphicUnits":
                // TODO 2019-10-11 18:11:52 CompositeGraphicUnit
                break;
            default:
                throw new IllegalArgumentException("不是 Res的子类，未知元素类型：" + qName);
        }
        return res;
    }
}
