package org.ofdrw.converter.pdf.model;

/**
 * 线连接样式，对应 PDF 的 {@code j} 操作符
 *
 * @author 权观宇
 * @since 2.5.1
 */
public enum LineJoin {
    /**
     * 尖角，OFD {@code Miter}
     */
    MITER,
    /**
     * 圆角，OFD {@code Round}
     */
    ROUND,
    /**
     * 斜切，OFD {@code Bevel}
     */
    BEVEL;

    /**
     * 由 OFD Join 属性解析
     *
     * @param name OFD 属性文本，可为空
     * @return 线连接样式，为空或无法识别时返回 {@link #MITER}
     */
    public static LineJoin of(String name) {
        if (name == null) {
            return MITER;
        }
        for (LineJoin join : values()) {
            if (join.name().equalsIgnoreCase(name.trim())) {
                return join;
            }
        }
        return MITER;
    }
}
