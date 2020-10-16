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
public class ST_Array extends STBase implements Cloneable{

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
        /*
         * 1 0 0
         * 0 1 0
         * 0 0 1
         */
        return new ST_Array(
                "1", "0", // 0
                "0", "1", // 0
                "0", "0"  // 1
        );
    }

    /**
     * 矩阵相乘
     *
     * @param array 矩阵数组
     * @return 相乘后的结果矩阵
     */
    public ST_Array mtxMul(ST_Array array) {
        if (this.array.size() != 6 || array.size() != 6) {
            throw new IllegalArgumentException("矩阵乘法数组规模必须 6元素(a b c d e f)");
        }
        double[][] a = this.toMtx();
        double[][] b = array.toMtx();

        double[][] res = new double[3][3];

        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < 3; i++) {
                for (int j = 0; j < 3; j++) {
                    res[i][j] += a[i][k] * b[k][j];
                }
            }
        }

        return new ST_Array(
                fmt(res[0][0]), fmt(res[0][1]),
                fmt(res[1][0]), fmt(res[1][1]),
                fmt(res[2][0]), fmt(res[2][1])
        );
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
                    continue;
                } else {
                    item = str;
                }
            } else if (item instanceof Double) {
                item = STBase.fmt((Double) item);
            } else if (item instanceof Number) {
                item = item.toString();
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

    /**
     * 反序列化为矩阵数组
     *
     * @return 矩阵
     */
    public double[][] toMtx() {
        if (size() != 6) {
            throw new IllegalArgumentException("矩阵数组必须有 9个元素");
        }

        double[][] mtx = new double[3][3];
        mtx[0][0] = Double.parseDouble(array.get(0));
        mtx[0][1] = Double.parseDouble(array.get(1));
        mtx[0][2] = 0;

        mtx[1][0] = Double.parseDouble(array.get(2));
        mtx[1][1] = Double.parseDouble(array.get(3));
        mtx[1][2] = 0;

        mtx[2][0] = Double.parseDouble(array.get(4));
        mtx[2][1] = Double.parseDouble(array.get(5));
        mtx[2][2] = 1;

        return mtx;
    }

    public void printMtx(){
        double[][] m = toMtx();
        for (int i = 0; i < m.length; i++) {
            for (int j = 0; j < m[i].length; j++) {
                System.out.print(fmt(m[i][j]) + "\t");
            }
            System.out.println();
        }
    }

    @Override
    public ST_Array clone(){
        ST_Array res = new ST_Array();
        for (String item : this.array) {
            res.add(item);
        }
        return res;
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
