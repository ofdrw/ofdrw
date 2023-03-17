package org.ofdrw.core.basicType;

import org.ofdrw.core.OFDSimpleTypeElement;

import java.io.Serializable;
import java.util.function.Function;

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
     * <p>
     * 浮点数含有小数，那么对保留3位有效小数
     *
     * @param d 浮点数
     * @return 数字字符串
     */
    public static String fmt(double d) {
        if (d == (long) d) {
            return String.format("%d", (long) d);
        } else if (-1 < d && d < 1) {
            String str = String.format("%f", d);

            int len = str.length();
            for (int i = 0; i < len; i++) {
                char c = str.charAt(i);
                if (c - '0' <= 0) {
                    continue;
                }
                if (i + 3 <= len) {
                    return str.substring(0, i + 3);
                }
                return str.substring(0, len);
            }
            return str;
        }  else {
            return String.format("%.3f", d);
        }
    }

    /**
     * 字符串转换Double
     *
     * @param str 字符串
     * @return Double 或 0
     */
    public static Double toDouble(String str) {
        Double res = num(str, Double::parseDouble);
        return res == null ? 0 : res;
    }

    /**
     * 字符串转换 Integer
     *
     * @param str 字符串
     * @return Double 或 0
     */
    public static Integer toInt(String str) {
        Integer res = num(str, Integer::parseInt);
        return res == null ? 0 : res;
    }

    /**
     * 字符串转换函数
     *
     * @param str    字符串
     * @param mapper 转换函数
     * @param <R>    转换返回值类型
     * @return 数字
     */
    private static <R extends Number> R num(String str, Function<String, R> mapper) {
        R res = null;
        try {
            res = mapper.apply(str);
        } catch (Exception e) {
            // 尽最大可能解析
            StringBuilder sb = new StringBuilder();
            for (char c : str.toCharArray()) {
                if (c == '-' || c == '+' || c == '.') {
                    sb.append(c);
                    continue;
                }
                if ('0' <= c && c <= '9') {
                    sb.append(c);
                }
            }
            try {
                res = mapper.apply(sb.toString());
            } catch (Exception ignored) {
            }
        }
        return res;
    }
}
