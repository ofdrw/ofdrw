package org.ofdrw.core.pageDescription.drawParam;

/**
 * 线条连接样式
 * <p>
 * 指定了两个线的端点结合时采用的样式
 * <p>
 * 线条连接样式的取值和显示效果之间的关系见表 22
 *
 * @author 权观宇
 * @since 2019-10-11 07:29:38
 */
public enum LineJoinType {
    Miter,
    Round,
    Bevel;

    /**
     * 根据类型字符串获取类型枚举
     * <p>
     * 默认值：Miter
     *
     * @param type 类型字符串
     * @return 枚举实例
     * @throws IllegalArgumentException 未知线条连接样式
     */
    public static LineJoinType getInstance(String type) {
        type = type == null ? "" : type.trim();
        switch (type) {
            case "":
            case "Miter":
                return Miter;
            case "Round":
                return Round;
            case "Bevel":
                return Bevel;
            default:
                throw new IllegalArgumentException("未知线条连接样式：" + type);
        }
    }
}
