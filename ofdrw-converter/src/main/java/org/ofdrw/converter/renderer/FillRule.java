package org.ofdrw.converter.renderer;

/**
 * 路径填充规则。
 * <p>
 * 与 AWT {@link java.awt.geom.Path2D} 的填充规则一一对应。
 */
public enum FillRule {

    /**
     * 非零环绕数规则
     */
    NON_ZERO,

    /**
     * 奇偶规则
     */
    EVEN_ODD
}
