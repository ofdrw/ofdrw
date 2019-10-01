package org.ofdrw.core.basicType;

import org.ofdrw.core.structure.SimpleTypeElement;

/**
 * 简单类型基类，用于提供便捷的方法实例化元素
 * @author 权观宇
 * @since 2019-10-01 03:25:28
 */
public class STBase {

    /**
     * 使用简单类型创建一个指定名称的元素
     * @param name 指定名称
     * @return 简单类型元素，<code><名称>ST_ 类型的toString</名称></code>
     */
    public SimpleTypeElement getElement(String name){
        return new SimpleTypeElement(name, this.toString());
    }
}
