package org.ofdrw.core.action.actionType.actionGoto;

import org.dom4j.Element;

/**
 * 用于描述Goto的目的地
 *
 * @author 权观宇
 * @since 2019-10-05 09:05:15
 */
public interface OFDGotoTarget extends Element {

    /**
     * 获取Goto目的地描述实例
     * @param element 元素
     * @return 实例
     * @throws IllegalArgumentException 未知Goto的目标类型
     */
    static OFDGotoTarget getInstance(Element element) {
        String qName = element.getQualifiedName();
        switch (qName) {
            case "ofd:Dest":
                return new CT_Dest(element);
            case "ofd:Bookmark":
                return new Bookmark(element);
            default:
                throw new IllegalArgumentException("未知Goto的目标类型：" + qName);
        }
    }
}
