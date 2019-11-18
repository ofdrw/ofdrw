package org.ofdrw.core.annotation.pageannot;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 分页注释文件
 *
 * 15.2 图 81 表 61
 *
 * @author 权观宇
 * @since 2019-11-16 02:20:54
 */
public class PageAnnot extends OFDElement {
    public PageAnnot(Element proxy) {
        super(proxy);
    }

    public PageAnnot() {
        super("PageAnnot");
    }

    // TODO Annot 2019-11-16 14:21:24
}
