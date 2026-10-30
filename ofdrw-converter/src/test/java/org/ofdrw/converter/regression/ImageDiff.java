package org.ofdrw.converter.regression;

import java.awt.image.BufferedImage;

/**
 * 两张位图的像素比较结果
 * <p>
 * 比较在 RGB 三通道上进行：两图宽高不一致时先按“左上角对齐、空白补齐”的方式对齐，
 * 补齐区域计入差异，从而避免尺寸不一致被静默忽略。
 * <p>
 * 指标定义：
 * <ul>
 *     <li>差异像素比例（diffRatio）：任一通道差值大于 {@link #DIFF_PIXEL_THRESHOLD} 的像素数 / 对齐后总像素数；</li>
 *     <li>平均绝对误差（MAE）：所有像素所有通道差值绝对值之和 /（总像素数 × 3），取值 0~1，对应阈值写作 n/255。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ImageDiff {

    /**
     * 判定“差异像素”的单通道阈值（0~255）
     * <p>
     * 取 16 用于滤除抗锯齿、栅格化取整带来的抖动，只统计肉眼可辨的差异。
     */
    public static final int DIFF_PIXEL_THRESHOLD = 16;

    /**
     * 基准图宽度（像素）
     */
    private final int width;

    /**
     * 基准图高度（像素）
     */
    private final int height;

    /**
     * 任一通道差值超过 {@link #DIFF_PIXEL_THRESHOLD} 的像素数（个）
     */
    private final int diffPixels;

    /**
     * 对齐后的总像素数（个）
     */
    private final int totalPixels;

    /**
     * 全部通道差值绝对值之和（0~255 通道值累计）
     */
    private final long absoluteErrorSum;

    /**
     * @param width            对齐后的图宽（像素）
     * @param height           对齐后的图高（像素）
     * @param diffPixels       差异像素数（个）
     * @param absoluteErrorSum 通道差值绝对值之和
     */
    private ImageDiff(int width, int height, int diffPixels, long absoluteErrorSum) {
        this.width = width;
        this.height = height;
        this.diffPixels = diffPixels;
        this.totalPixels = width * height;
        this.absoluteErrorSum = absoluteErrorSum;
    }

    /**
     * 比较两张位图
     * <p>
     * 任一参数为空时直接抛出 {@link IllegalArgumentException}。
     * 两图宽高不一致时按左上角对齐，超出部分按白色页面底色补齐，从而计入差异。
     *
     * @param expected 基准图（通常为基准 PDF 渲染结果）
     * @param actual   实际图（通常为转换得到的 PDF 渲染结果）
     * @return 比较结果
     */
    public static ImageDiff compare(BufferedImage expected, BufferedImage actual) {
        if (expected == null || actual == null) {
            throw new IllegalArgumentException("待比较位图为空");
        }
        // 两图宽高不一致时按左上角对齐、多余区域按白色页面底色补齐，补齐区域会计入差异
        int width = Math.max(expected.getWidth(), actual.getWidth());
        int height = Math.max(expected.getHeight(), actual.getHeight());
        BufferedImage left = alignToBlank(expected, width, height);
        BufferedImage right = alignToBlank(actual, width, height);
        int[] leftPixels = left.getRGB(0, 0, width, height, null, 0, width);
        int[] rightPixels = right.getRGB(0, 0, width, height, null, 0, width);
        int diffPixels = 0;
        long sum = 0;
        for (int i = 0; i < leftPixels.length; i++) {
            int lp = leftPixels[i];
            int rp = rightPixels[i];
            int dr = Math.abs(((lp >> 16) & 0xFF) - ((rp >> 16) & 0xFF));
            int dg = Math.abs(((lp >> 8) & 0xFF) - ((rp >> 8) & 0xFF));
            int db = Math.abs((lp & 0xFF) - (rp & 0xFF));
            sum += dr + dg + db;
            if (dr > DIFF_PIXEL_THRESHOLD || dg > DIFF_PIXEL_THRESHOLD || db > DIFF_PIXEL_THRESHOLD) {
                diffPixels++;
            }
        }
        return new ImageDiff(width, height, diffPixels, sum);
    }

    /**
     * 把位图绘制到指定宽高的白色画布上（左上角对齐）
     * <p>
     * 尺寸一致时直接返回原图，避免无谓的像素拷贝；尺寸不一致时先创建 {@code width × height} 的 RGB 图并用白色填充，
     * 再把原图绘制到左上角，超出部分保持白色页面底色。
     *
     * @param source 原位图
     * @param width  画布宽（像素）
     * @param height 画布高（像素）
     * @return 与目标尺寸一致的位图
     */
    private static BufferedImage alignToBlank(BufferedImage source, int width, int height) {
        if (source.getWidth() == width && source.getHeight() == height) {
            return source;
        }
        BufferedImage canvas = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = canvas.createGraphics();
        try {
            g.setColor(java.awt.Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return canvas;
    }

    /**
     * @return 对齐后的图宽（像素）
     */
    public int getWidth() {
        return width;
    }

    /**
     * @return 对齐后的图高（像素）
     */
    public int getHeight() {
        return height;
    }

    /**
     * @return 差异像素数（个）
     */
    public int getDiffPixels() {
        return diffPixels;
    }

    /**
     * @return 对齐后的总像素数（个）
     */
    public int getTotalPixels() {
        return totalPixels;
    }

    /**
     * 差异像素比例，取值 0~1
     *
     * @return 差异像素数 / 总像素数
     */
    public double getDiffRatio() {
        return totalPixels == 0 ? 0d : (double) diffPixels / totalPixels;
    }

    /**
     * 平均绝对误差（MAE），取值 0~1
     * <p>
     * 定义为“各通道差值绝对值之和 /（像素数 × 3）/ 255”，与设计文档决策 5 的阈值
     * {@code MAE ≤ 2/255}（即 {@code <= 2d/255d}）同一口径，比较时不需要再做单位换算。
     *
     * @return 按 255 归一化后的平均绝对误差
     */
    public double getMeanAbsoluteError() {
        long samples = (long) totalPixels * 3;
        return samples == 0 ? 0d : (double) absoluteErrorSum / samples / 255d;
    }

    /**
     * @return 便于日志输出的单行描述
     */
    @Override
    public String toString() {
        return String.format("diffPixels=%d/%d (%.4f%%), MAE=%.2f/255",
                diffPixels, totalPixels, getDiffRatio() * 100d, getMeanAbsoluteError() * 255d);
    }
}
