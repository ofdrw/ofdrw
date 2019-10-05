package org.ofdrw.core.basicStructure.outlines;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 大纲按照树形结构进行组织
 * <p>
 * 图 18 大纲节点结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:12:39
 */
public class Outlines extends OFDElement {
    public Outlines(Element proxy) {
        super(proxy);
    }

    public Outlines() {
        super("Outlines");
    }

    // TODO 2019-10-5 11:12:51 OutlineElem
}
