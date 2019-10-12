package org.ofdrw.core.pageDescription.color.pattern;

/**
 * 底纹单元起始绘制位置
 * <p>
 * 8.3.4 表 28 RelativeTo
 *
 * @author 权观宇
 * @since 2019-10-12 08:49:39
 */
public enum RelativeTo {
    /**
     * 相对于页面坐标的原点
     */
    Page,
    /**
     * 相对于对象坐标系的原点
     */
    Object;

    /**
     * 获取 底纹单元起始绘制位置
     *
     * @param to 绘制位置字符串
     * @return 底纹单元起始绘制位置
     */
    public static RelativeTo getInstance(String to) {
        to = (to == null) ? "" : to.trim();
        switch (to) {
            case "":
            case "Object":
                return Object;
            case "Page":
                return Page;
            default:
                throw new IllegalArgumentException("未知的底纹单元起始绘制位置：" + to);
        }

    }
}
