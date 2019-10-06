package org.ofdrw.core.action.actionType.actionGoto;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 用于描述Goto的目的地
 *
 * @author 权观宇
 * @since 2019-10-05 09:05:15
 */
public class GotoTarget extends OFDElement {
    public GotoTarget(Element proxy) {
        super(proxy);
    }

    public GotoTarget(String name) {
        super(name);
    }
}
