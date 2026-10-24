package org.ofdrw.converter.renderer;

/**
 * 绘制混合模式。
 * <p>
 * 取值与 PDF 标准混合模式一致，用于图片与印章的合成。
 */
public enum BlendMode {

    /**
     * 正常覆盖（源覆盖目标）
     */
    NORMAL,

    /**
     * 正片叠底
     */
    MULTIPLY,

    /**
     * 滤色
     */
    SCREEN,

    /**
     * 叠加
     */
    OVERLAY,

    /**
     * 变暗
     */
    DARKEN,

    /**
     * 变亮
     */
    LIGHTEN,

    /**
     * 颜色减淡
     */
    COLOR_DODGE,

    /**
     * 颜色加深
     */
    COLOR_BURN,

    /**
     * 强光
     */
    HARD_LIGHT,

    /**
     * 柔光
     */
    SOFT_LIGHT,

    /**
     * 差值
     */
    DIFFERENCE,

    /**
     * 排除
     */
    EXCLUSION
}
