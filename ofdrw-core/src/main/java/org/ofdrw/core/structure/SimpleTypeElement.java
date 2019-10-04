package org.ofdrw.core.structure;

/**
 * 简单类型元素对象，用于承载 Text
 *
 * @author 权观宇
 * @since 2019-10-01 03:22:42
 */
public class SimpleTypeElement extends OFDElement {

    /**
     * 创建一个带有文本元素
     *
     * @param name 元素名称
     * @param obj  元素值对象（可toString 序列化为字符串）
     */
    public SimpleTypeElement(String name, Object obj) {
        super(name);
        this.setText(obj.toString());
    }

    @Override
    public String getQualifiedName() {
        return this.proxy.getQualifiedName();
    }
}
