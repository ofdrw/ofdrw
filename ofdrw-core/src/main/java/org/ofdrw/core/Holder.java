package org.ofdrw.core;

import java.io.Serializable;

/**
 * 外壳对象，用于解决 lambda语法作用域导致的赋值问题。
 *
 * @author 权观宇
 * @since 2022-03-04 22:07:53
 */
public final class Holder<T> implements Serializable {

    /**
     * 持有对象
     */
    public T value;

    public Holder() {
    }

    /**
     * 构造外壳对象
     *
     * @param value 待编辑的值
     */
    public Holder(T value) {
        this.value = value;
    }
}
