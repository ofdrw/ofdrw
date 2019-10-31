package org.ofdrw.core.pageDescription.color.color;

import org.dom4j.Element;
import org.ofdrw.core.pageDescription.color.color.axialShd.CT_AxialShd;


/**
 * 颜色族
 * <p>
 * 用于标识属于颜色的一种，颜色可以是基本颜色、底纹和渐变
 * <p>
 * 8.3.2 图 25 颜色结构
 *
 * @author 权观宇
 * @since 2019-10-11 09:38:10
 */
public interface ColorClusterType extends Element {


    /**
     * 解析元素并获取对应的 颜色族子类实例
     *
     * @param element 实例
     * @return 子类实例，基本颜色、底纹和渐变
     * @throws IllegalArgumentException 未知的元素类型不是 ColorClusterType子类
     */
    static ColorClusterType getInstance(Element element) {
        String qName = element.getQualifiedName();
        ColorClusterType res = null;
        switch (qName) {
            case "ofd:Pattern":
                // TODO 2019-10-11 21:40:30 Pattern
                break;
            case "ofd:AxialShd":
                res = new CT_AxialShd(element);
                break;
            case "ofd:RadialShd":
                // TODO 2019-10-11 21:40:57 RadialShd
                break;
            case "ofd:GouraudShd":
                // TODO 2019-10-11 21:41:11 GouraudShd
                break;
            case "ofd:LaGouraudShd":
                // TODO 2019-10-11 21:41:22 LaGouraudShd
                break;
            default:
                throw new IllegalArgumentException("未知的元素类型不是 颜色子类：" + qName);
        }
        return res;
    }
}
