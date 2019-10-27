package org.ofdrw.core.image;

import org.dom4j.Element;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

/**
 * 边框颜色
 * <p>
 * 有关边框颜色描述见 8.3.2 基本颜色
 * <p>
 * 默认为黑色
 *
 * @author 权观宇
 * @since 2019-10-27 03:36:42
 */
public class BorderColor extends CT_Color {
    public BorderColor(Element proxy) {
        super(proxy);
    }

    public BorderColor() {
        super("BorderColor");
    }
}
