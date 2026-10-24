package org.ofdrw.converter.renderer;

import java.awt.Composite;
import java.awt.CompositeContext;
import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * 基于 {@link BlendMode} 的自定义 {@link Composite}，用于补上 AWT 缺失的混合模式能力。
 * <p>
 * 限制：
 * <ul>
 *     <li>仅支持 {@link java.awt.image.BufferedImage#TYPE_INT_RGB} 与 {@link java.awt.image.BufferedImage#TYPE_INT_ARGB} 目标位图，
 *     其它目标（如外部 Graphics2D、IndexColorModel）退化为“普通覆盖”；</li>
 *     <li>仅应在离屏渲染后端启用：{@code SVGGraphics2D}、{@code PdfGraphics2D} 等外部上下文不支持自定义 Composite；</li>
 *     <li>颜色按整数 RGB 通道逐像素计算，不做色彩管理。</li>
 * </ul>
 * 计算方式与 PDF 标准混合模式一致：先按混合函数得到 BlendedColor，再按源 alpha 在 BlendedColor 与目标色之间插值，
 * 即 {@code result = (1 - alpha) * dst + alpha * blend(src, dst)}。
 */
public final class Graphics2DBlendComposite implements Composite {

    /**
     * 混合模式
     */
    private final BlendMode mode;

    /**
     * 构造混合合成对象。
     *
     * @param mode 混合模式，不能为空
     */
    public Graphics2DBlendComposite(BlendMode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("混合模式不能为空");
        }
        this.mode = mode;
    }

    /**
     * 获取混合模式。
     *
     * @return 混合模式
     */
    public BlendMode getMode() {
        return mode;
    }

    /**
     * 创建绘制上下文。
     *
     * @param srcColorModel  源颜色模型
     * @param dstColorModel  目标颜色模型
     * @param hints          绘制提示
     * @return 合成上下文
     */
    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
        return new BlendContext(mode);
    }

    /**
     * 把源图片按本混合模式合成到目标图片上（逐像素写入目标图片）。
     * <p>
     * 该方法不经过 AWT 绘制管线，直接以前景/背景位图的栅格数据进行混合，
     * 因此不受 AWT 平台管线（shadow buffer、临时画布）对自定义 {@link Composite} 的影响。
     * 两张图片左上角对齐，尺寸以较小者为准。
     *
     * @param target 目标图片，必须为 {@link BufferedImage#TYPE_INT_ARGB} 或 {@link BufferedImage#TYPE_INT_RGB}
     * @param source 源图片，必须为 {@link BufferedImage#TYPE_INT_ARGB} 或 {@link BufferedImage#TYPE_INT_RGB}
     * @throws IllegalArgumentException 图片为空或像素格式不受支持
     */
    public void blend(BufferedImage target, BufferedImage source) {
        if (target == null || source == null) {
            throw new IllegalArgumentException("目标图片与源图片均不能为空");
        }
        int[] targetPixels = pixelArray(target);
        int[] sourcePixels = pixelArray(source);
        int width = Math.min(target.getWidth(), source.getWidth());
        int height = Math.min(target.getHeight(), source.getHeight());
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int s = sourcePixels[y * source.getWidth() + x];
                int sa = (s >>> 24) & 0xFF;
                if (sa == 0) {
                    continue;
                }
                int index = y * target.getWidth() + x;
                int d = targetPixels[index];
                int blended = blendPixel(mode, s, d);
                if (sa < 255) {
                    // 半透明源：在混合结果与目标色之间按 alpha 插值
                    int r = channel(blended, 16, d, sa);
                    int g = channel(blended, 8, d, sa);
                    int b = channel(blended, 0, d, sa);
                    int a = Math.max((d >>> 24) & 0xFF, sa);
                    blended = (a << 24) | (r << 16) | (g << 8) | b;
                }
                targetPixels[index] = blended;
            }
        }
    }

    /**
     * 获取可直接读写的像素数组。
     *
     * @param image 图片，必须为 {@link BufferedImage#TYPE_INT_ARGB} 或 {@link BufferedImage#TYPE_INT_RGB}
     * @return 像素数组，按行主序排列
     * @throws IllegalArgumentException 像素格式不受支持
     */
    private static int[] pixelArray(BufferedImage image) {
        if (!(image.getRaster().getDataBuffer() instanceof DataBufferInt)) {
            throw new IllegalArgumentException("仅支持 TYPE_INT_ARGB 与 TYPE_INT_RGB 位图，实际为：" + image.getType());
        }
        return ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
    }

    @Override
    public String toString() {
        return "Graphics2DBlendComposite{" + mode + "}";
    }

    /**
     * 混合合成上下文：逐像素执行混合函数。
     */
    private static final class BlendContext implements CompositeContext {

        /**
         * 混合模式
         */
        private final BlendMode mode;

        /**
         * 构造混合上下文。
         *
         * @param mode 混合模式
         */
        BlendContext(BlendMode mode) {
            this.mode = mode;
        }

        /**
         * 释放上下文资源；本实现不持有额外资源。
         */
        @Override
        public void dispose() {
        }

        /**
         * 把源像素按混合模式合成到目标像素上。
         *
         * @param src 源栅格
         * @param dst 目标栅格（可写副本）
         * @param hints 绘制提示
         */
        @Override
        public void compose(Raster src, Raster dstIn, WritableRaster dst) {
            if (!(dst.getDataBuffer() instanceof DataBufferInt)) {
                // 非整数像素栅格退化为普通覆盖
                dst.setRect(src);
                return;
            }
            int[] dstPixels = ((DataBufferInt) dst.getDataBuffer()).getData();
            int[] inPixels = dstIn.getDataBuffer() instanceof DataBufferInt
                    ? ((DataBufferInt) dstIn.getDataBuffer()).getData() : dstPixels;
            int width = Math.min(src.getWidth(), dst.getWidth());
            int height = Math.min(src.getHeight(), dst.getHeight());
            int x0 = Math.max(src.getMinX(), dst.getMinX());
            int y0 = Math.max(src.getMinY(), dst.getMinY());
            int[] source = new int[4];
            for (int y = y0; y < y0 + height; y++) {
                for (int x = x0; x < x0 + width; x++) {
                    src.getPixel(x, y, source);
                    int dstIndex = pixelIndex(dst, x, y);
                    int inIndex = pixelIndex(dstIn, x, y);
                    if (dstIndex < 0 || dstIndex >= dstPixels.length || inIndex < 0 || inIndex >= inPixels.length) {
                        continue;
                    }
                    int d = inPixels[inIndex];
                    int sa = source[3];
                    if (sa == 0) {
                        continue;
                    }
                    int s = (sa << 24) | (source[0] << 16) | (source[1] << 8) | source[2];
                    int blended = blendPixel(mode, s, d);
                    if (sa < 255) {
                        // 半透明源：在混合结果与目标色之间按 alpha 插值
                        int r = channel(blended, 16, d, sa);
                        int g = channel(blended, 8, d, sa);
                        int b = channel(blended, 0, d, sa);
                        int a = Math.max((d >>> 24) & 0xFF, sa);
                        blended = (a << 24) | (r << 16) | (g << 8) | b;
                    }
                    dstPixels[dstIndex] = blended;
                }
            }
        }

        /**
         * 计算像素在数据缓冲区中的索引。
         *
         * @param raster 栅格
         * @param x      绝对 X 坐标，单位：像素
         * @param y      绝对 Y 坐标，单位：像素
         * @return 数据缓冲区索引
         */
        private static int pixelIndex(Raster raster, int x, int y) {
            int localX = x - raster.getSampleModelTranslateX();
            int localY = y - raster.getSampleModelTranslateY();
            return raster.getDataBuffer().getOffset() + localY * raster.getWidth() + localX;
        }

        /**
         * 计算像素在数据缓冲区中的索引。
         *
         * @param raster 栅格
         * @param x      绝对 X 坐标，单位：像素
         * @param y      绝对 Y 坐标，单位：像素
         * @return 数据缓冲区索引
         */

        /**
         * 按 alpha 插值单个通道（上下文内部使用）。
         *
         * @param blended  混合后的像素
         * @param shift    通道位移，16 表示红、8 表示绿、0 表示蓝
         * @param target   目标像素
         * @param alpha    源 alpha，取值 0–255
         * @return 插值后的通道值，取值 0–255
         */
        private static int channel(int blended, int shift, int target, int alpha) {
            int source = (blended >>> shift) & 0xFF;
            int destination = (target >>> shift) & 0xFF;
            return destination + (source - destination) * alpha / 255;
        }

    }

    /**
     * 按 alpha 插值单个通道。
     *
     * @param blended 混合后的像素
     * @param shift   通道位移，16 表示红、8 表示绿、0 表示蓝
     * @param target  目标像素
     * @param alpha   源 alpha，取值 0–255
     * @return 插值后的通道值，取值 0–255
     */
    private static int channel(int blended, int shift, int target, int alpha) {
        int source = (blended >>> shift) & 0xFF;
        int destination = (target >>> shift) & 0xFF;
        return destination + (source - destination) * alpha / 255;
    }

    /**
     * 对单个像素执行混合函数。
     *
     * @param mode 混合模式
     * @param src  源像素 ARGB
     * @param dst  目标像素 ARGB
     * @return 混合后的不透明像素 ARGB
     */
    private static int blendPixel(BlendMode mode, int src, int dst) {
        int sr = (src >>> 16) & 0xFF;
        int sg = (src >>> 8) & 0xFF;
        int sb = src & 0xFF;
        int dr = (dst >>> 16) & 0xFF;
        int dg = (dst >>> 8) & 0xFF;
        int db = dst & 0xFF;
        int r;
        int g;
        int b;
        switch (mode) {
            case MULTIPLY:
                r = sr * dr / 255;
                g = sg * dg / 255;
                b = sb * db / 255;
                break;
            case SCREEN:
                r = 255 - (255 - sr) * (255 - dr) / 255;
                g = 255 - (255 - sg) * (255 - dg) / 255;
                b = 255 - (255 - sb) * (255 - db) / 255;
                break;
            case OVERLAY:
                r = overlay(sr, dr);
                g = overlay(sg, dg);
                b = overlay(sb, db);
                break;
            case DARKEN:
                r = Math.min(sr, dr);
                g = Math.min(sg, dg);
                b = Math.min(sb, db);
                break;
            case LIGHTEN:
                r = Math.max(sr, dr);
                g = Math.max(sg, dg);
                b = Math.max(sb, db);
                break;
            case DIFFERENCE:
                r = Math.abs(sr - dr);
                g = Math.abs(sg - dg);
                b = Math.abs(sb - db);
                break;
            case EXCLUSION:
                r = sr + dr - 2 * sr * dr / 255;
                g = sg + dg - 2 * sg * dg / 255;
                b = sb + db - 2 * sb * db / 255;
                break;
            case COLOR_DODGE:
                r = colorDodge(sr, dr);
                g = colorDodge(sg, dg);
                b = colorDodge(sb, db);
                break;
            case COLOR_BURN:
                r = colorBurn(sr, dr);
                g = colorBurn(sg, dg);
                b = colorBurn(sb, db);
                break;
            case HARD_LIGHT:
                r = overlay(dr, sr);
                g = overlay(dg, sg);
                b = overlay(db, sb);
                break;
            case SOFT_LIGHT:
                r = softLight(sr, dr);
                g = softLight(sg, dg);
                b = softLight(sb, db);
                break;
            case NORMAL:
            default:
                r = sr;
                g = sg;
                b = sb;
                break;
        }
        return (0xFF << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b);
    }

    /**
     * 叠加（Overlay）混合函数：每一通道按目标通道高低选择正片叠底或滤色。
     *
     * @param source 源通道值，取值 0–255
     * @param target 目标通道值，取值 0–255
     * @return 混合结果，取值 0–255
     */
    private static int overlay(int source, int target) {
        return target < 128
                ? 2 * source * target / 255
                : 255 - 2 * (255 - source) * (255 - target) / 255;
    }

    /**
     * 颜色减淡（Color Dodge）混合函数。
     *
     * @param source 源通道值，取值 0–255
     * @param target 目标通道值，取值 0–255
     * @return 混合结果，取值 0–255
     */
    private static int colorDodge(int source, int target) {
        if (source == 0) {
            return 0;
        }
        if (target >= 255) {
            return 255;
        }
        return Math.min(255, target * 255 / (255 - source));
    }

    /**
     * 颜色加深（Color Burn）混合函数。
     *
     * @param source 源通道值，取值 0–255
     * @param target 目标通道值，取值 0–255
     * @return 混合结果，取值 0–255
     */
    private static int colorBurn(int source, int target) {
        if (source >= 255) {
            return 255;
        }
        if (target <= 0) {
            return 0;
        }
        return 255 - Math.min(255, (255 - target) * 255 / source);
    }

    /**
     * 柔光（Soft Light）混合函数。
     *
     * @param source 源通道值，取值 0–255
     * @param target 目标通道值，取值 0–255
     * @return 混合结果，取值 0–255
     */
    private static int softLight(int source, int target) {
        double s = source / 255d;
        double d = target / 255d;
        double result = d + (2 * s - 1) * (d - d * d);
        return (int) Math.round(result * 255);
    }

    /**
     * 把通道值截断到 0–255。
     *
     * @param value 通道值
     * @return 截断后的通道值
     */
    private static int clamp(int value) {
        return Math.max(0, Math.min(255, value));
    }
}
