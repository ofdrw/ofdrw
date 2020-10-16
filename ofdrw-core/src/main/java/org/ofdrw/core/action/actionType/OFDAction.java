package org.ofdrw.core.action.actionType;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.action.actionType.actionMovie.Movie;

/**
 * 动作类型
 * <p>
 * 表 51 动作类型属性
 *
 * @author 权观宇
 * @since 2019-10-05 08:31:15
 */
public interface OFDAction extends Element {


    /**
     * 获取动作类型实例
     *
     * @param element 元素
     * @return 实例
     * @throws IllegalArgumentException 未知的动作类型
     */
    static OFDAction getInstance(Element element) {
        String qName = element.getQualifiedName();
        switch (qName) {
            case "ofd:Goto":
                return new Goto(element);
            case "ofd:URI":
                return new URI(element);
            case "ofd:GotoA":
                return new GotoA(element);
            case "ofd:Sound":
                return new Sound(element);
            case "ofd:Movies":
                return new Movie(element);
            default:
                throw new IllegalArgumentException("未知的动作类型：" + qName);
        }
    }
}
