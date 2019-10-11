package org.ofdrw.core.graph.method;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 路径操作
 *
 * 表 37 图形对象描述方法，如：移动、划线等
 */
public abstract class Command extends OFDElement {
    public Command(Element proxy) {
        super(proxy);
    }

    public Command(String name) {
        super(name);
    }
}
