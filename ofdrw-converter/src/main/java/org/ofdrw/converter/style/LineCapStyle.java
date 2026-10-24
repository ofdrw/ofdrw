package org.ofdrw.converter.style;

/**
 * 线端点样式（对应 OFD 的 Cap 属性）。
 */
public enum LineCapStyle {

    /**
     * 平头端点
     */
    BUTT(java.awt.BasicStroke.CAP_BUTT),

    /**
     * 圆头端点
     */
    ROUND(java.awt.BasicStroke.CAP_ROUND),

    /**
     * 方头端点
     */
    SQUARE(java.awt.BasicStroke.CAP_SQUARE);

    /**
     * 对应的 AWT 线端点常量
     */
    private final int awtValue;

    /**
     * 创建线端点样式。
     *
     * @param awtValue 对应的 AWT 线端点常量，见 {@link java.awt.BasicStroke}
     */
    LineCapStyle(int awtValue) {
        this.awtValue = awtValue;
    }

    /**
     * 获取对应的 AWT 线端点常量。
     *
     * @return AWT 线端点常量，见 {@link java.awt.BasicStroke}
     */
    public int toAwtValue() {
        return awtValue;
    }
}
