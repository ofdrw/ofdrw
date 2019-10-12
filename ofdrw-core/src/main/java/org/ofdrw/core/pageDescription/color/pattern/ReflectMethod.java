package org.ofdrw.core.pageDescription.color.pattern;

/**
 * 翻转绘制效果
 * <p>
 * 8.3.4 渐变 图 28
 *
 * @author 权观宇
 * @since 2019-10-12 08:38:42
 */
public enum ReflectMethod {
    /**
     * 普通重复
     * <p>
     * 具体效果见 图 28  Normal
     */
    Normal,
    /**
     * 竖轴对称翻转
     * <p>
     * 具体效果见 图 28  Column
     */
    Column,
    /**
     * 横轴对称翻转
     * <p>
     * 具体效果见 图 28  Row
     */
    Row,
    /**
     * 十字轴对称翻转
     * <p>
     * 具体效果见 图 28  Row And Column
     */
    RowAndColumn;

    /**
     * 获取 翻转绘制效果 实例
     *
     * @param method 效果名称
     * @return 翻转绘制效果
     */
    public static ReflectMethod getInstance(String method) {
        method = (method == null) ? "" : method.trim();
        switch (method) {
            case "":
            case "Normal":
                return Normal;
            case "Column":
                return Column;
            case "Row":
                return Row;
            case "RowAndColumn":
                return RowAndColumn;
            default:
                throw new IllegalArgumentException("未知的翻转绘制效果：" + method);
        }
    }
}
