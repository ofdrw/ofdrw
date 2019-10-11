package org.ofdrw.core.basicType;

import java.util.ArrayList;
import java.util.List;

/**
 * 数组，以空格来分割元素。元素可以是除ST_Loc、
 * ST_Array外的数据类型，不可嵌套
 * <p>
 * 示例：
 * <code>1 2.0 5.0</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 *
 * @author 权观宇
 * @since 2019-09-28 10:40:37
 */
public class ST_Array extends STBase {

    /**
     * 元素收容
     */
    private List<String> array = new ArrayList<>();


    /**
     * 获取 ST_Array 实例如果参数非法则返还null
     *
     * @param arrStr 数字字符串
     * @return 实例 或 null
     */
    public static ST_Array getInstance(String arrStr) {
        if (arrStr == null || arrStr.trim().length() == 0) {
            return null;
        }
        return new ST_Array(arrStr.trim().split(" "));
    }

    public ST_Array(String[] arr) {
        array = new ArrayList<>(arr.length);
        for (String item : arr) {
            if (item == null || item.length() == 0) {
                throw new IllegalArgumentException("数组元素为空");
            }
            array.add(item);
        }
    }

    public ST_Array(double[] arr) {
        array = new ArrayList<>(arr.length);
        for (double item : arr) {
            array.add(STBase.fmt(item));
        }
    }


    public ST_Array add(String item) {
        this.array.add(item);
        return this;
    }

    public List<String> getArray() {
        return array;
    }


    public ST_Array setArray(List<String> array) {
        this.array = array;
        return this;
    }

    /**
     * 数组长度
     *
     * @return 数组长度
     */
    public int size() {
        return this.array.size();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String item : array) {
            sb.append(' ').append(item);
        }
        return sb.toString().trim();
    }
}
