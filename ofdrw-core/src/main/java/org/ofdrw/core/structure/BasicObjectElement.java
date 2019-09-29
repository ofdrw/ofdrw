package org.ofdrw.core.structure;

import org.dom4j.tree.DefaultElement;
import org.ofdrw.core.Const;

/**
 * 元素节点应使用命名空间标识符，元素属性不使用命名空间标识符。
 * ————《GB/T 33190-2016》 7.1 命名空间
 *
 * @author 权观宇
 * @since 2019-09-28 12:05:52
 */
public class BasicObjectElement extends DefaultElement {

    public BasicObjectElement(String name) {
        //  qName = "odf:" + name
        super(Const.OFD_Q + name);
    }

    /**
     * 创建一个带有文本元素
     * @param name 元素名称
     * @param obj 元素值对象（可toString 序列化为字符串）
     */
    public BasicObjectElement(String name, Object obj) {
        this(name);
        this.setText(obj.toString());
    }
}
