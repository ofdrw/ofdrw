package org.ofdrw.layout.element;

/**
 * 数组参数工具
 *
 * @author 权观宇
 * @since 2020-02-03 01:09:54
 */
public final class ArrayParamTool {

    /**
     * 四个参数分析工具
     * <p>
     * 当传入
     * - 空时，为{0,0,0,0}
     * - 1个元素，{arr[0], arr[0], arr[0], arr[0]}
     * - 2个元素，{arr[0], arr[1], arr[0], arr[1]}
     * - 3个元素，{arr[0], arr[1], arr[2], 0}
     * - 3个以上，{arr[0], arr[1], arr[2], arr[3]}
     *
     * @param arr 可变参数
     * @return 4个参数
     */
    public static Double[] arr4p(Double... arr) {
        if (arr == null || arr.length == 0) {
            return new Double[]{0d, 0d, 0d, 0d};
        }
        switch (arr.length) {
            case 1:
                return new Double[]{arr[0], arr[0], arr[0], arr[0]};
            case 2:
                return new Double[]{arr[0], arr[1], arr[0], arr[1]};
            case 3:
                return new Double[]{arr[0], arr[1], arr[2], 0d};
            default:
                return new Double[]{arr[0], arr[1], arr[2], arr[3]};
        }
    }
}
