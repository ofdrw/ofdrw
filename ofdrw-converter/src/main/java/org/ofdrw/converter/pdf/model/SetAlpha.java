package org.ofdrw.converter.pdf.model;

/**
 * 透明度状态：对应 PDF ExtGState 的 {@code ca}（非描边）与 {@code CA}（描边）
 * <p>
 * 设计决策 2：OFD 图元 Alpha 缺省视为 <b>255（不透明）</b>，仅显式声明才生效；
 * 颜色自身 Alpha（0~255）与图元 Alpha 相乘后写入本指令。
 * <p>
 * 不透明度取值范围 0~1，1 表示完全不透明。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class SetAlpha extends AbstractCommand {

    /**
     * 描边不透明度
     */
    private final float strokeAlpha;

    /**
     * 非描边不透明度
     */
    private final float fillAlpha;

    /**
     * 构造透明度指令
     *
     * @param fillAlpha   非描边不透明度，范围 0~1
     * @param strokeAlpha 描边不透明度，范围 0~1
     */
    public SetAlpha(float fillAlpha, float strokeAlpha) {
        super(CoordSpace.PAGE);
        this.fillAlpha = clamp(fillAlpha);
        this.strokeAlpha = clamp(strokeAlpha);
    }

    /**
     * 同时设置填充与描边的不透明度
     *
     * @param alpha 不透明度，范围 0~1
     * @return 透明度指令
     */
    public static SetAlpha of(float alpha) {
        return new SetAlpha(alpha, alpha);
    }

    /**
     * 由 OFD 的 0~255 整型 Alpha 创建指令
     *
     * @param alpha0to255 OFD Alpha 值，范围 0~255；null 表示属性缺失，按不透明 255 处理
     * @return 透明度指令
     */
    public static SetAlpha fromOfd(Integer alpha0to255) {
        float value = alpha0to255 == null ? 1f : alpha0to255 / 255f;
        return new SetAlpha(value, value);
    }

    /**
     * @return 非描边不透明度，范围 0~1
     */
    public float getFillAlpha() {
        return fillAlpha;
    }

    /**
     * @return 描边不透明度，范围 0~1
     */
    public float getStrokeAlpha() {
        return strokeAlpha;
    }

    /**
     * 截断到 0~1
     *
     * @param value 输入值
     * @return 截断后的值
     */
    private static float clamp(float value) {
        if (Float.isNaN(value)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, value));
    }

    @Override
    public String toString() {
        return "SetAlpha(ca=" + fillAlpha + ", CA=" + strokeAlpha + ")";
    }
}
