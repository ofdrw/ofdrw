package org.ofdrw.converter.style;

/**
 * 线条连接样式（对应 OFD 的 Join 属性）。
 */
public enum LineJoinStyle {

    /**
     * 尖角连接
     */
    MITER(java.awt.BasicStroke.JOIN_MITER),

    /**
     * 圆角连接
     */
    ROUND(java.awt.BasicStroke.JOIN_ROUND),

    /**
     * 斜切连接
     */
    BEVEL(java.awt.BasicStroke.JOIN_BEVEL);

    /**
     * 对应的 AWT 线连接常量
     */
    private final int awtValue;

    /**
     * 创建线条连接样式。
     *
     * @param awtValue 对应的 AWT 线连接常量，见 {@link java.awt.BasicStroke}
     */
    LineJoinStyle(int awtValue) {
        this.awtValue = awtValue;
    }

    /**
     * 获取对应的 AWT 线连接常量。
     *
     * @return AWT 线连接常量，见 {@link java.awt.BasicStroke}
     */
    public int toAwtValue() {
        return awtValue;
    }
}
