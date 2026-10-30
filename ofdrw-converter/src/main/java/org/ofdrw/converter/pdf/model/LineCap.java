package org.ofdrw.converter.pdf.model;

/**
 * 线端样式，对应 PDF 的 {@code J} 操作符
 *
 * @author 权观宇
 * @since 2.5.1
 */
public enum LineCap {
    /**
     * 平头，OFD {@code Butt}
     */
    BUTT,
    /**
     * 圆头，OFD {@code Round}
     */
    ROUND,
    /**
     * 方头，OFD {@code Square}
     */
    SQUARE;

    /**
     * 由 OFD LineCap 属性解析
     *
     * @param name OFD 属性文本，可为空
     * @return 线端样式，为空或无法识别时返回 {@link #BUTT}
     */
    public static LineCap of(String name) {
        if (name == null) {
            return BUTT;
        }
        for (LineCap cap : values()) {
            if (cap.name().equalsIgnoreCase(name.trim())) {
                return cap;
            }
        }
        return BUTT;
    }
}
