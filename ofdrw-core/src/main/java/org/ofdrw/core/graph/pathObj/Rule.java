package org.ofdrw.core.graph.pathObj;

/**
 * 图形的填充规则
 * <p>
 * 9.1 表 35 图形对象描述
 *
 * @author 权观宇
 * @since 2019-10-16 08:46:57
 */
public enum Rule {

    NonZero("NonZero"),
    Even_Odd("Even-Odd");

    private String name;

    Rule(String name) {
        this.name = name;
    }

    public static Rule getInstance(String type) {
        type = type == null ? "" : type.trim();
        // switch (type) {
        //     case "":
        //     case "NonZero":
        //         return NonZero;
        //     case "Even-Odd":
        //         return Even_Odd;
        //     default:
        //         throw new IllegalArgumentException("未知的图形的填充规则类型：" + type);
        // }
        if (type.equalsIgnoreCase("NonZero") || "".equals(type)) {
            return NonZero;
        } else if (type.equalsIgnoreCase("Even-Odd")) {
            return Even_Odd;
        } else {
            throw new IllegalArgumentException("未知的图形的填充规则类型：" + type);
        }
    }


    @Override
    public String toString() {
        return name;
    }
}
