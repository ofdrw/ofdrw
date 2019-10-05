package org.ofdrw.core.action;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 动作序列
 * <p>
 * 图 19 大纲节点结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:32:31
 */
public class Actions extends OFDElement {
    public Actions(Element proxy) {
        super(proxy);
    }

    public Actions() {
        super("Actions");
    }

    // TODO 2019-10-5 11:32:48 Action
}
