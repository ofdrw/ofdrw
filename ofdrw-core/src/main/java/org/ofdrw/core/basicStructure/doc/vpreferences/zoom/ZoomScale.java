package org.ofdrw.core.basicStructure.doc.vpreferences.zoom;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 缩放比例选择对象基类
 */
public class ZoomScale extends OFDElement  {
    public ZoomScale(Element proxy) {
        super(proxy);
    }

    public ZoomScale(String name) {
        super(name);
    }
}
