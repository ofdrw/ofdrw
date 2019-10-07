package org.ofdrw.core.basicStructure.doc.vpreferences.zoom;

import org.dom4j.Element;
import org.ofdrw.core.basicType.STBase;

/**
 * 文档的缩放率
 * <p>
 * 7.5 表 9 视图首选项
 *
 * @author 权观宇
 * @since 2019-10-07 09:52:29
 */
public class Zoom extends ZoomScale {
    public Zoom(Element proxy) {
        super(proxy);
    }

    public Zoom(double value) {
        super("Zoom");
        this.setValue(value);
    }

    /**
     * 设置  文档的缩放率
     *
     * @param value 文档的缩放率
     * @return this
     */
    public Zoom setValue(double value) {
        this.addText(STBase.fmt(value));
        return this;
    }

    /**
     * 获取 文档的缩放率
     *
     * @return 文档的缩放率
     */
    public Double getValue() {
        return Double.parseDouble(this.getText());
    }
}
