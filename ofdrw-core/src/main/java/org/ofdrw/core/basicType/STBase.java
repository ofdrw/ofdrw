package org.ofdrw.core.basicType;

import org.ofdrw.core.OFDSimpleTypeElement;

import java.io.Serializable;

/**
 * 简单类型基类，用于提供便捷的方法实例化元素
 *
 * @author 权观宇
 * @since 2019-10-01 03:25:28
 */
public abstract class STBase implements Serializable {


    /**
     * 使用简单类型创建一个指定名称的元素
     *
     * @param name 指定名称
     * @return 简单类型元素
     */
    public OFDSimpleTypeElement getElement(String name) {
        return new OFDSimpleTypeElement(name, this.toString());
    }

    /**
     * 如果浮点数为整数，则省略小数
     * @param d 浮点数
     * @return 没有小数点的整数字符串
     */
    public static String fmt(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else {
            return String.format("%s", d);
        }
    }
}
