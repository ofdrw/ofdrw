package org.ofdrw.core.pageDescription.drawParam;

/**
 * 线端点样式
 * <p>
 * 指定一条线的端点样式。
 * <p>
 * 线条端点样式取值与效果之间关系见表 24
 *
 * 默认值为 Butt
 *
 * @author 权观宇
 * @since 2019-10-11 07:34:59
 */
public enum LineCapType {
    Butt,
    Round,
    Square;

    /**
     * 根据类型字符串获取类型枚举
     * <p>
     * 默认值：Miter
     *
     * @param type 类型字符串
     * @return 枚举实例
     * @throws IllegalArgumentException 未知的线端点样式
     */
    public static LineCapType getInstance(String type) {
        type = type == null ? "" : type.trim();
        // switch (type) {
        //     case "":
        //     case "Butt":
        //         return Butt;
        //     case "Round":
        //         return Round;
        //     case "Square":
        //         return Square;
        //     default:
        //         throw new IllegalArgumentException("未知的线端点样式：" + type);
        // }
        if (type.equalsIgnoreCase("Butt")|| "".equals(type)){
            return Butt;
        } else if (type.equalsIgnoreCase("Round")) {
            return Round;
        } else if (type.equalsIgnoreCase("Square")) {
            return Square;
        } else {
            throw new IllegalArgumentException("未知的线端点样式：" + type);
        }
    }
}
