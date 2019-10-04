package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

/**
 * 文档根节点
 * Document.xml
 *
 * ————《GB/T 33190-2016》 图 5
 *
 * @author 权观宇
 * @since 2019-10-04 07:43:14
 */
public class Document extends OFDElement {
    public Document(Element proxy) {
        super(proxy);
    }

    public Document() {
        super("Document");
    }

    // TODO CommonData 2019-10-4 19:44:38
}
