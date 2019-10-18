package org.ofdrw.core.basicType;

import java.io.Serializable;
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
     * 获取一个单位矩阵变换参数
     *
     * @return 单位CTM举证
     */
    public static ST_Array unitCTM() {
        return new ST_Array(new String[]{
                "1", "0", // 0
                "0", "1", // 0
                "0", "0"  // 1
        });
    }

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

    public ST_Array(Serializable... arr) {

        if (arr == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        array = new ArrayList<>(arr.length);

        for (Serializable item : arr) {
            if (item instanceof String || item == null) {
                String str = (String) item;
                if (item == null || str.trim().length() == 0) {
                    throw new IllegalArgumentException("数组元素为空");
                } else {
                    item = str;
                }
            }
            array.add(item.toString());
        }
    }


    public ST_Array add(String item) {
        this.array.add(item);
        return this;
    }

    public List<String> getArray() {
        return array;
    }

    /**
     * 获取浮点数组
     *
     * @return 浮点数组
     */
    public Double[] toDouble() {
        return this.array.stream()
                .map(Double::parseDouble)
                .toArray(Double[]::new);
    }

    /**
     * 获取整形数组
     *
     * @return 整形数组
     */
    public Integer[] toInt() {
        return this.array.stream()
                .map(Integer::parseInt)
                .toArray(Integer[]::new);
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
