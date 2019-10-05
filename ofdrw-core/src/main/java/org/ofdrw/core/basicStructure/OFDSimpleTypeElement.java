package org.ofdrw.core.basicStructure;

import java.io.Serializable;

/**
 * 简单类型元素对象，用于承载 Text
 *
 * @author 权观宇
 * @since 2019-10-01 03:22:42
 */
public class OFDSimpleTypeElement extends OFDElement {

    /**
     * 创建一个带有文本元素
     *
     * @param name 元素名称
     * @param obj  元素值对象（可toString 序列化为字符串）
     */
    public OFDSimpleTypeElement(String name, Serializable obj) {
        super(name);
        this.setText(obj.toString());
    }

    @Override
    public String getQualifiedName() {
        return this.proxy.getQualifiedName();
    }
}
