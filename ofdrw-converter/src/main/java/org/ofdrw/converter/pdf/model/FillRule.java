package org.ofdrw.converter.pdf.model;

/**
 * 填充规则
 * <p>
 * 对应 PDF 的 {@code f}（非零环绕）与 {@code f*}（奇偶），OFD 中对应 PathObject 的 {@code Rule} 属性。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public enum FillRule {
    /**
     * 非零环绕规则，OFD 的 {@code NonZero}
     */
    NON_ZERO,
    /**
     * 奇偶规则，OFD 的 {@code Even-Odd}
     */
    EVEN_ODD;

    /**
     * 由 OFD 的 Rule 属性文本解析填充规则
     *
     * @param rule OFD Rule 属性文本，取值 {@code NonZero} 或 {@code Even-Odd}，可为空
     * @return 填充规则，为空或无法识别时返回 OFD 默认值 {@link #NON_ZERO}
     */
    public static FillRule of(String rule) {
        if (rule == null) {
            return NON_ZERO;
        }
        String normalized = rule.trim().replace("-", "").replace("_", "");
        if ("EvenOdd".equalsIgnoreCase(normalized)) {
            return EVEN_ODD;
        }
        return NON_ZERO;
    }
}
