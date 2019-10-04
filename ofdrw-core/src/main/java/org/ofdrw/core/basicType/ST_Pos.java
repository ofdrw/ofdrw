package org.ofdrw.core.basicType;

/**
 * 点坐标，以格分割，前者为 x值，后者为 y值，可以是整数或浮点数
 * <p>
 * 示例：
 * <code>0 0</code>
 * <p>
 * ————《GB/T 33190-2016》 表 2 基本数据类型
 */
public class ST_Pos extends STBase {

    /**
     * X坐标
     * <p>
     * 从左 到 右
     */
    private Double x = 0d;
    /**
     * y坐标
     * <p>
     * 从上 到 下
     */
    private Double y = 0d;

    public ST_Pos(double x, double y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 获取 ST_Pos 实例如果参数非法则返还null
     *
     * @param arrStr 数字字符串
     * @return 实例 或 null
     */
    public static ST_Pos getInstance(String arrStr) {
        if (arrStr == null || arrStr.trim().length() == 0) {
            return null;
        }
        String[] values = arrStr.trim().split(" ");

        if (values.length != 2) {
            return null;
        }
        return new ST_Pos(Double.parseDouble(values[0]), Double.parseDouble(values[1]));
    }

    public Double getX() {
        return x;
    }

    public ST_Pos setX(Double x) {
        this.x = x;
        return this;
    }

    public Double getY() {
        return y;
    }

    public ST_Pos setY(Double y) {
        this.y = y;
        return this;
    }

    @Override
    public String toString() {
        return (fmt(x) + " " + fmt(y));
    }
}
