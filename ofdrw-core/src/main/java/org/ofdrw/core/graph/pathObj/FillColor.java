package org.ofdrw.core.graph.pathObj;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.pageDescription.color.color.CT_AxialShd;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

/**
 * 填充颜色
 * <p>
 * 9.1 表 35
 *
 * @author 权观宇
 * @since 2019-10-27 03:23:49
 */
public class FillColor extends CT_Color {
    public FillColor(Element proxy) {
        super(proxy);
    }

    public FillColor() {
        super("FillColor");
    }

}
