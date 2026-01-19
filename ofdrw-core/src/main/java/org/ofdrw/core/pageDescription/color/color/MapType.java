package org.ofdrw.core.pageDescription.color.color;

/**
 * 渐变绘制的方式
 * <p>
 * 8.3.4.2 轴向渐变 图 29、30 表 29
 *
 * @author 权观宇
 * @since 2019-10-31 06:57:29
 */
public enum MapType {
    /**
     * 默认值 Direct
     */
    Direct,
    Repeat,
    Reflect;

    public static MapType getInstance(String type) {
        type = (type == null) ? "" : type.trim();
        // switch (type){
        //     case "":
        //     case "Direct":
        //         return Direct;
        //     case "Repeat":
        //         return Repeat;
        //     case "Reflect":
        //         return Reflect;
        //     default:
        //         throw new IllegalArgumentException("位置渐变绘制的方式：" + type);
        // }
        if (type.equalsIgnoreCase("Direct") || type.equals("")) {
            return Direct;
        } else if (type.equalsIgnoreCase("Repeat")) {
            return Repeat;
        } else if (type.equalsIgnoreCase("Reflect")) {
            return Reflect;
        } else {
            throw new IllegalArgumentException("位置渐变绘制的方式：" + type);
        }
    }
}
