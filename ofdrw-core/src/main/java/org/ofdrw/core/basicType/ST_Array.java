package org.ofdrw.core.basicType;

import org.ofdrw.core.structure.SimpleTypeElement;

import javax.xml.bind.Element;
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
    private List<Object> array = new ArrayList<Object>();


    public ST_Array(String[] arr) {
        array = new ArrayList<Object>(arr.length);
        for (String item : arr) {
            if (item == null || item.length() == 0) {
                throw new IllegalArgumentException("数组元素为空");
            }
            array.add(item);
        }
    }


    public ST_Array add(Object item) {
        this.array.add(item);
        return this;
    }

    public List<Object> getArray() {
        return array;
    }

    public ST_Array setArray(List<Object> array) {
        this.array = array;
        return this;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Object item : array) {
            if (sb.length() > 1) {
                sb.append(" ");
            }
            sb.append(item);
        }
        return sb.toString();
    }
}
