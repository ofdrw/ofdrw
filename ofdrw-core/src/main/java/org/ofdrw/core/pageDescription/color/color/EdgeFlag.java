package org.ofdrw.core.pageDescription.color.color;

/**
 * 三角单元切换的方向标志
 * <p>
 * 8.1.4.4 表 31 附录A.13
 *
 * @author 权观宇
 * @since 2019-11-09 12:33:09
 */
public enum EdgeFlag {
    _0(0),
    _1(1),
    _2(2);

    private int flag;

    EdgeFlag(int flag) {
        this.flag = flag;
    }

    public static EdgeFlag getInstance(String flagStr) {
        if (flagStr == null || flagStr.trim().length() == 0) {
            flagStr = "";
        }
        switch (flagStr) {
            case "":
            case "0":
                return _0;
            case "1":
                return _1;
            case "2":
                return _2;
            default:
                throw new IllegalArgumentException("未知的三角单元切换的方向标志：" + flagStr);
        }
    }

    @Override
    public String toString() {
        return Integer.toString(flag);
    }
}
