package org.ofdrw.converter.pdf.model;

import java.util.Arrays;

/**
 * 中间模型颜色：与渲染器无关的颜色值
 * <p>
 * 通道值统一归一化到 0~1，通道数量由 {@link ColorSpace} 决定：
 * <ul>
 *     <li>{@link ColorSpace#GRAY}：1 个通道</li>
 *     <li>{@link ColorSpace#RGB}：3 个通道</li>
 *     <li>{@link ColorSpace#CMYK}：4 个通道</li>
 * </ul>
 * 颜色本身携带的 Alpha 按设计 3.4 节换算为 0~1 的不透明度，缺省视为 1.0（不透明）。
 * <p>
 * 本类为不可变对象，颜色通道数组在构造与访问时均做防御性拷贝。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfColor {

    /**
     * 黑色（RGB）
     */
    public static final PdfColor BLACK = rgb(0, 0, 0);

    /**
     * 白色（RGB）
     */
    public static final PdfColor WHITE = rgb(1, 1, 1);

    /**
     * 颜色空间
     */
    private final ColorSpace colorSpace;

    /**
     * 各通道值，范围 0~1
     */
    private final float[] channels;

    /**
     * 不透明度，范围 0~1，1 表示完全不透明
     */
    private final float opacity;

    /**
     * 构造颜色
     *
     * @param colorSpace 颜色空间，不能为空
     * @param opacity    不透明度，范围 0~1（超出范围按边界截断）
     * @param channels   通道值，范围 0~1，数量必须与颜色空间一致
     */
    public PdfColor(ColorSpace colorSpace, float opacity, float... channels) {
        if (colorSpace == null) {
            throw new IllegalArgumentException("颜色空间不能为空");
        }
        if (channels == null || channels.length != colorSpace.getComponentCount()) {
            throw new IllegalArgumentException(colorSpace + " 需要 " + colorSpace.getComponentCount() + " 个通道");
        }
        this.colorSpace = colorSpace;
        this.channels = new float[channels.length];
        for (int i = 0; i < channels.length; i++) {
            this.channels[i] = clamp(channels[i]);
        }
        this.opacity = clamp(opacity);
    }

    /**
     * 创建 GRAY 颜色
     *
     * @param gray 灰度值，范围 0（黑）~1（白）
     * @return 颜色
     */
    public static PdfColor gray(float gray) {
        return new PdfColor(ColorSpace.GRAY, 1f, gray);
    }

    /**
     * 创建 RGB 颜色
     *
     * @param r 红色分量，范围 0~1
     * @param g 绿色分量，范围 0~1
     * @param b 蓝色分量，范围 0~1
     * @return 颜色
     */
    public static PdfColor rgb(float r, float g, float b) {
        return new PdfColor(ColorSpace.RGB, 1f, r, g, b);
    }

    /**
     * 创建 CMYK 颜色
     * <p>
     * 注意：设计 3.4 节要求 CMYK 必须按原颜色空间输出，不能转换为 RGB。
     *
     * @param c 青色分量，范围 0~1
     * @param m 品红分量，范围 0~1
     * @param y 黄色分量，范围 0~1
     * @param k 黑色分量，范围 0~1
     * @return 颜色
     */
    public static PdfColor cmyk(float c, float m, float y, float k) {
        return new PdfColor(ColorSpace.CMYK, 1f, c, m, y, k);
    }

    /**
     * 复制颜色并替换不透明度
     *
     * @param newOpacity 不透明度，范围 0~1
     * @return 新的颜色对象
     */
    public PdfColor withOpacity(float newOpacity) {
        return new PdfColor(colorSpace, newOpacity, channels);
    }

    /**
     * 获取颜色空间
     *
     * @return 颜色空间
     */
    public ColorSpace getColorSpace() {
        return colorSpace;
    }

    /**
     * 获取通道数量
     *
     * @return 通道数量
     */
    public int getComponentCount() {
        return channels.length;
    }

    /**
     * 获取指定通道值
     *
     * @param index 通道下标，从 0 起
     * @return 通道值，范围 0~1
     */
    public float getChannel(int index) {
        return channels[index];
    }

    /**
     * 获取所有通道值
     *
     * @return 通道值副本，范围 0~1
     */
    public float[] getChannels() {
        return channels.clone();
    }

    /**
     * 获取不透明度
     *
     * @return 不透明度，范围 0~1，1 表示完全不透明
     */
    public float getOpacity() {
        return opacity;
    }

    /**
     * 将数值截断到 0~1 区间，非有限值按 0 处理
     *
     * @param value 待截断数值
     * @return 截断后的数值
     */
    private static float clamp(float value) {
        if (Float.isNaN(value) || Float.isInfinite(value)) {
            return 0f;
        }
        if (value < 0f) {
            return 0f;
        }
        return Math.min(value, 1f);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PdfColor)) {
            return false;
        }
        PdfColor other = (PdfColor) o;
        return colorSpace == other.colorSpace
                && Float.compare(opacity, other.opacity) == 0
                && Arrays.equals(channels, other.channels);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * colorSpace.hashCode() + Float.hashCode(opacity)) + Arrays.hashCode(channels);
    }

    @Override
    public String toString() {
        return colorSpace + Arrays.toString(channels) + " alpha=" + opacity;
    }

    /**
     * 中间模型支持的颜色空间
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public enum ColorSpace {
        /**
         * 灰度
         */
        GRAY(1),
        /**
         * RGB
         */
        RGB(3),
        /**
         * CMYK
         */
        CMYK(4);

        /**
         * 通道数量
         */
        private final int componentCount;

        ColorSpace(int componentCount) {
            this.componentCount = componentCount;
        }

        /**
         * 获取通道数量
         *
         * @return 通道数量
         */
        public int getComponentCount() {
            return componentCount;
        }
    }
}
