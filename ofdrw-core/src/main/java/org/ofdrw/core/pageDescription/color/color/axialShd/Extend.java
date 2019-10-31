package org.ofdrw.core.pageDescription.color.color.axialShd;

/**
 * 轴线延长线方向是否继续绘制
 * <p>
 * 可选值为 0、1、2、3
 * <p>
 * 0：不向两侧继续绘制渐变
 * <p>
 * 1: 在结束点至起始点延长线方向绘制渐变
 * <p>
 * 2：在起始点至结束点延长线方向绘制渐变
 * <p>
 * 3：向两侧延长线方向绘制渐变
 * <p>
 * 默认值为 0
 *
 * 8.3.4.2 轴向渐变 图 29、30 表 29
 *
 * @author 权观宇
 * @since 2019-10-31 07:39:22
 */
public enum Extend {

    /**
     * 不向两侧继续绘制渐变
     *
     * 默认值
     */
    _0(0),
    /**
     * 在结束点至起始点延长线方向绘制渐变
     */
    _1(1),
    /**
     * 在起始点至结束点延长线方向绘制渐变
     */
    _2(2),
    /**
     * 向两侧延长线方向绘制渐变
     */
    _3(3);

    /**
     * 类型值
     */
    private int value;

    Extend(int value) {
        this.value = value;
    }

    public static Extend getInstance(String value) {
        value = (value == null)? "" : value.trim();
        switch (value) {
            case "":
            case "0":
                return _0;
            case "1":
                return _1;
            case "2":
                return _2;
            case "3":
                return _3;
            default:
                throw new IllegalArgumentException("未知的轴线延长线方向是否继续绘制类型：" + value);
        }
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
