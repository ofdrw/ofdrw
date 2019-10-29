package org.ofdrw.core.compositeObj;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.pageObj.layer.block.CT_PageBlock;

/**
 * 内容的矢量描述
 * <p>
 * 13 复合对象 表 50
 *
 * @author 权观宇
 * @since 2019-10-27 05:20:54
 */
public class Content extends CT_PageBlock {
    public Content(Element proxy) {
        super(proxy);
    }

    public Content() {
        super("Content");
    }
}
