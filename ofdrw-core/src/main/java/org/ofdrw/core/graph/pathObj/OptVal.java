package org.ofdrw.core.graph.pathObj;

import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.graph.tight.method.*;

import java.util.Arrays;
import java.util.List;

/**
 * 操作符和操作数
 *
 * @author 权观宇
 * @since 2021-04-24 18:19:55
 */
public class OptVal implements Cloneable {
    // 操作符
    public String opt;
    /**
     * 操作数
     */
    public double[] values = new double[0];

    /**
     * @param opt 操作符
     */
    public OptVal(String opt) {
        this.opt = opt;
    }

    /**
     * @param opt    操作符
     * @param values 操作数序列
     */
    public OptVal(String opt, double[] values) {
        this.opt = opt;
        if (values != null) {
            this.values = values;
        }
    }

    /**
     * @param opt          操作符
     * @param valueStrList 操作数字符序列
     */
    public OptVal(String opt, List<String> valueStrList) {
        this.opt = opt;
        if (valueStrList != null) {
            values = new double[valueStrList.size()];
            for (int i = 0; i < valueStrList.size(); i++) {
                try {
                    values[i] = Double.parseDouble(valueStrList.get(i));
                } catch (NumberFormatException e) {
                    values[i] = 0;
                }
            }
        }
    }

    public String getOpt() {
        return opt;
    }

    public void setOpt(String opt) {
        this.opt = opt;
    }

    public double[] getValues() {
        return values;
    }

    public void setValues(double[] values) {
        this.values = values;
    }

    /**
     * 在数组长度不足时填充数组
     * <p>
     * 不足时，填充的0到新的数组中
     *
     * @param arr 数组
     * @param num 期待的长度
     * @return 新数组或元素的复制
     */
    public static double[] filling(double[] arr, int num) {
        if (arr == null) {
            arr = new double[num];
        }
        if (arr.length < num) {
            return Arrays.copyOf(arr, num);
        }
        return arr;
    }

    /**
     * 转换为非紧缩标识
     * <p>
     * 如果无法识别操作符，那么返还null
     *
     * @return 费紧缩对象或null
     */
    public Command toCmd() {
        switch (opt) {
            case "S":
            case "M": {
                double[] arr = filling(values, 2);
                return new Move(arr[0], arr[1]);
            }
            case "L": {
                double[] arr = filling(values, 2);
                return new Line(arr[0], arr[1]);
            }
            case "Q": {
                final double[] arr = filling(values, 4);
                return new QuadraticBezier(arr[0], arr[1], arr[2], arr[3]);
            }
            case "B": {
                final double[] arr = filling(values, 6);
                return new CubicBezier(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
            }
            case "A": {
                final double[] arr = filling(values, 7);
                return new Arc(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5], arr[6]);
            }
            case "C": {
                return new Close();
            }
            default:
                return null;
        }
    }

    /**
     * 获取期待数量的参数值
     * <p>
     * 参数不足时补0
     *
     * @return 参数值
     */
    public double[] expectValues() {
        switch (opt) {
            case "S":
            case "M":
            case "L": {
                return filling(values, 2);
            }
            case "Q": {
                return filling(values, 4);
            }
            case "B": {
                return filling(values, 6);
            }
            case "A": {
                return filling(values, 7);
            }
            case "C": {
                return new double[0];
            }
            default:
                return new double[0];
        }
    }

    @Override
    public OptVal clone() {
        return new OptVal(this.opt, this.values.clone());
    }

    @Override
    public String toString() {
        switch (opt) {
            case "S":
            case "M":
            case "L": {
                double[] arr = filling(values, 2);
                return String.format("%s %s %s", opt, STBase.fmt(arr[0]), STBase.fmt(arr[1]));
            }
            case "Q": {
                final double[] arr = filling(values, 4);
                return String.format("Q %s %s %s %s",
                        STBase.fmt(arr[0]), STBase.fmt(arr[1]),
                        STBase.fmt(arr[2]), STBase.fmt(arr[3]));
            }
            case "B": {
                final double[] arr = filling(values, 6);
                return String.format("B %s %s %s %s %s %s",
                        STBase.fmt(arr[0]), STBase.fmt(arr[1]),
                        STBase.fmt(arr[2]), STBase.fmt(arr[3]),
                        STBase.fmt(arr[4]), STBase.fmt(arr[5]));
            }
            case "A": {
                final double[] arr = filling(values, 7);
                return String.format("A %s %s %s %s %s %s %s",
                        STBase.fmt(arr[0]), STBase.fmt(arr[1]),
                        STBase.fmt(arr[2]),
                        STBase.fmt(arr[3]),
                        STBase.fmt(arr[4]),
                        STBase.fmt(arr[5]), STBase.fmt(arr[6]));
            }
            case "C": {
                return "C";
            }
            default:
                return "";
        }
    }
}
