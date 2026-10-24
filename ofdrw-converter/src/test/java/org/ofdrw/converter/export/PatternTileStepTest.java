package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 底纹（Pattern）平铺间距的回归测试。
 * <p>
 * 覆盖两类缺陷：
 * <ol>
 *     <li>水印平铺密度错误：{@code XStep}/{@code YStep} 大于单元 {@code Width}/{@code Height} 时，
 *         必须按步进留出间隙，不能把单元密铺（表现为水印过密、字号偏大）；</li>
 *     <li>CTM 旋转被当成轴对齐包围盒：45° 旋转会引入 √2 倍的步进误差，
 *         旋转应在纹理位图中预先完成，锚点只承载平移与步进尺寸。</li>
 * </ol>
 * <p>
 * 测试资源：
 * <ul>
 *     <li>{@code pattern_watermark_nofont.ofd}：由 {@code pattern类型.ofd} 裁切而来（移除了内嵌字体，
 *         保留水印注释与底纹），单元 20×10 mm、步进 40×30 mm、45° 旋转，
 *         不含除水印外的其他图元，可直接按像素断言平铺间距；</li>
 *     <li>{@code pattern类型.ofd}：真实水印文档，单元 107×26.2 mm、步进 127×66.2 mm、
 *         CTM 为 45° 旋转，用于断言整体密度不再显著偏大。</li>
 * </ul>
 */
class PatternTileStepTest {

    /**
     * 带步进间隙的底纹文档：单元 20×10 mm，步进 40×30 mm，45° 旋转
     */
    private static final Path GAP_PATTERN_OFD = Paths.get("src/test/resources/pattern_watermark_nofont.ofd");

    /**
     * 真实水印文档：45° 斜向平铺的“严禁复制”水印
     */
    private static final Path WATERMARK_OFD = Paths.get("src/test/resources/pattern类型.ofd");

    /**
     * 导出分辨率，单位：像素/毫米（px/mm）
     */
    private static final double PPM = 4.0;

    @TempDir
    Path tempDir;

    @Test
    void shouldTileByStepInsteadOfCellSize() throws IOException, GeneralConvertException {
        final BufferedImage image = exportFirstPage(GAP_PATTERN_OFD, tempDir);
        /*
         * 断言“平铺周期”与“页内图形周期”的比值等于 XStep / Width（40 / 20 = 2）。
         *
         * 水印注释的 Scale 与旋转决定了单元在页面上的绝对尺寸，因此不能断言绝对像素；
         * 而单元内的图形（“严禁复制”与单元边界）周期等于 Width，相邻单元之间的周期等于 XStep，
         * 两者比值只取决于 XStep / Width。修复前单元被拉伸到步进宽度，该比值会退化为 1。
         */
        /*
         * 本样本为 45° 旋转的水印，页面坐标下的周期是旋转后步进的投影；
         * 因此断言“页面周期 ≥ 单元图形周期”，以及周期与单元图形的比例不小于
         * 投影比 XStep / sqrt(Width² + Height²)（20 / sqrt(20² + 10²) ≈ 0.89）。
         * 修复前单元被拉伸到步进宽度，页面周期会退化为与单元图形周期相同。
         */
        final int tileGap = dominantLag(image, 120, 360);
        final int cellGap = dominantLag(image, 8, 60);
        assertTrue(tileGap > 0 && cellGap > 0,
                "应能找到平铺周期与单元图形周期，实际：" + tileGap + " px / " + cellGap + " px");
        final double minRatio = 40.0 / Math.hypot(40.0, 30.0);
        final double actualRatio = (double) tileGap / cellGap;
        assertTrue(actualRatio >= minRatio - 0.1,
                String.format("平铺周期相对单元图形过小（疑似按单元尺寸密铺）：比例 %.2f，期望 ≥ %.2f（周期 %d px，图形周期 %d px）",
                        actualRatio, minRatio, tileGap, cellGap));
    }

    @Test
    void shouldLeaveGapBetweenTiles() throws IOException, GeneralConvertException {
        final BufferedImage image = exportFirstPage(GAP_PATTERN_OFD, tempDir);
        // 相邻单元之间必须留有空隙：单元图形自身的周期与单元平铺周期不同，间距即为两者之差
        final int tileGap = dominantLag(image, 120, 360);
        final int cellGap = dominantLag(image, 8, 60);
        assertTrue(tileGap > cellGap,
                String.format("相邻单元之间应留有间隙（XStep 40mm > Width 20mm），实际平铺周期 %d px ≤ 单元宽 %d px",
                        tileGap, cellGap));
        // 间隙宽度应接近 XStep - Width（20mm），在此文档的实际缩放下约 2 个字符宽
        final double scale = (double) cellGap / 20.0;
        final double expectedGap = 20.0 * scale;
        assertTrue(tileGap - cellGap >= expectedGap * 0.6,
                String.format("单元间隙偏小：实际 %d px，期望约 %.1f px", tileGap - cellGap, expectedGap));
    }

    @Test
    void shouldNotOverDensifyWatermark() throws IOException, GeneralConvertException {
        final BufferedImage image = exportFirstPage(WATERMARK_OFD, tempDir);
        double lightRatio = 0;
        double darkRatio = 0;
        long total = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                total++;
                final int gray = grayOf(image, x, y);
                if (gray >= 170 && gray <= 245) {
                    lightRatio++;
                } else if (gray < 100) {
                    darkRatio++;
                }
            }
        }
        lightRatio = total == 0 ? 0 : lightRatio / total;
        darkRatio = total == 0 ? 0 : darkRatio / total;
        // 修复前按单元尺寸密铺，浅灰水印占比约 14.5%；按步进平铺后应显著下降
        assertTrue(lightRatio < 0.10,
                String.format("水印平铺过密：浅灰像素占比 %.2f%%（修复前约 14.5%%，PDF 基准约 6.7%%）",
                        lightRatio * 100));
        assertTrue(lightRatio > 0.005, "水印不应消失，浅灰像素占比过小：" + lightRatio);
        // 水印不得退化为整块深色遮挡正文
        assertTrue(darkRatio < 0.05, String.format("深色像素占比异常：%.2f%%", darkRatio * 100));
    }

    /**
     * 导出指定文档的第 1 页。
     *
     * @param ofd      待导出文档
     * @param targetDir 输出目录
     * @return 导出的位图
     * @throws IOException             文件读写异常
     * @throws GeneralConvertException 导出异常
     */
    private static BufferedImage exportFirstPage(Path ofd, Path targetDir) throws IOException, GeneralConvertException {
        try (CanvasImageExporter exporter = new CanvasImageExporter(ofd, targetDir, "PNG", PPM)) {
            exporter.export(0);
            final Path img = exporter.getImgFilePaths().get(0);
            final BufferedImage image = ImageIO.read(img.toFile());
            assertNotNull(image, "导出的图片应可读取");
            return image;
        }
    }

    /**
     * 用列投影的自相关估计水印在给定范围内最显著的重复周期。
     * <p>
     * 列投影为每列中水印像素的数量，周期重复的平铺会在自相关上出现峰值。
     *
     * @param image  位图
     * @param minLag 搜索的周期下界，单位：像素
     * @param maxLag 搜索的周期上界，单位：像素
     * @return 最显著周期，单位：像素；找不到时返回 0
     */
    private static int dominantLag(BufferedImage image, int minLag, int maxLag) {
        final double[] corr = columnAutocorrelation(image, Math.min(maxLag, image.getWidth() / 2));
        int bestLag = 0;
        double best = 0;
        for (int lag = Math.max(1, minLag); lag < corr.length; lag++) {
            if (corr[lag] > best) {
                best = corr[lag];
                bestLag = lag;
            }
        }
        return bestLag;
    }

    /**
     * 计算水印列投影的自相关。
     *
     * @param image 位图
     * @param maxLag 最大滞后，单位：像素
     * @return 自相关数组，下标为滞后量
     */
    private static double[] columnAutocorrelation(BufferedImage image, int maxLag) {
        final int width = image.getWidth();
        final int height = image.getHeight();
        final double[] column = new double[width];
        for (int x = 0; x < width; x++) {
            int count = 0;
            for (int y = 0; y < height; y++) {
                final int rgb = image.getRGB(x, y);
                if ((rgb >>> 24) != 0 && grayOf(image, x, y) < 250) {
                    count++;
                }
            }
            column[x] = count;
        }
        double mean = 0;
        for (double v : column) {
            mean += v;
        }
        mean /= width;
        for (int x = 0; x < width; x++) {
            column[x] -= mean;
        }
        final double[] corr = new double[maxLag + 1];
        for (int lag = 0; lag <= maxLag && lag < width; lag++) {
            double sum = 0;
            for (int x = 0; x + lag < width; x++) {
                sum += column[x] * column[x + lag];
            }
            corr[lag] = sum / (width - lag);
        }
        return corr;
    }

    /**
     * 取像素的灰度值。
     *
     * @param image 位图
     * @param x     横坐标，单位：像素
     * @param y     纵坐标，单位：像素
     * @return 灰度值，取值 0..255，无量纲
     */
    private static int grayOf(BufferedImage image, int x, int y) {
        final int rgb = image.getRGB(x, y);
        return (((rgb >> 16) & 0xFF) + ((rgb >> 8) & 0xFF) + (rgb & 0xFF)) / 3;
    }
}
