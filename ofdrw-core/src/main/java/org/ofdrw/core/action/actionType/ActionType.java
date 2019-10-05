package org.ofdrw.core.action.actionType;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 动作类型
 * <p>
 * 表 51 动作类型属性
 *
 * @author 权观宇
 * @since 2019-10-05 08:31:15
 */
public abstract class ActionType extends OFDElement {

    public ActionType(Element proxy) {
        super(proxy);
    }

    public ActionType(String name) {
        super(name);
    }
}
