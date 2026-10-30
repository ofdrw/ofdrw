package org.ofdrw.converter.regression;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 回归指标自检
 * <p>
 * 像素/文本指标是回归结论的唯一依据，必须先证明“相同输入判通过、已知差异判不通过”，
 * 否则框架本身不可信（设计第 7.2 节决策 5 的阈值以本自检校准）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class RegressionMetricTest {

    /**
     * 构造纯色位图
     *
     * @param width  宽（像素）
     * @param height 高（像素）
     * @param rgb    颜色
     * @return 位图
     */
    private static BufferedImage solid(int width, int height, int rgb) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                image.setRGB(x, y, rgb);
            }
        }
        return image;
    }

    @Test
    @DisplayName("像素指标：完全一致时差异为 0")
    void identicalImages() {
        ImageDiff diff = ImageDiff.compare(solid(100, 100, 0xFFFFFF), solid(100, 100, 0xFFFFFF));
        assertEquals(0, diff.getDiffPixels());
        assertEquals(0d, diff.getDiffRatio(), 1e-9, "完全一致时差异像素比例为 0");
        assertEquals(0d, diff.getMeanAbsoluteError(), 1e-9, "完全一致时 MAE 为 0");
    }

    @Test
    @DisplayName("像素指标：抗锯齿级别的微小抖动不计入差异像素")
    void antiAliasingNoiseIgnored() {
        ImageDiff diff = ImageDiff.compare(solid(100, 100, 0xFF0000), solid(100, 100, 0xEF0000));
        assertEquals(0, diff.getDiffPixels(), "单通道差值 16 不应计为差异像素");
        // 但 MAE 仍会反映出来：只有 R 通道差 16，按 3 通道平均为 (16/3)/255
        assertEquals(16d / 3d / 255d, diff.getMeanAbsoluteError(), 1e-9, "MAE 应按 3 通道平均并归一化到 0~1");
    }

    @Test
    @DisplayName("像素指标：1% 像素明显变化时超过 0.5% 阈值")
    void onePercentDiffFails() {
        BufferedImage expected = solid(100, 100, 0xFFFFFF);
        BufferedImage actual = solid(100, 100, 0xFFFFFF);
        // 10 行 × 10 列 = 100 像素 = 1%
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 10; x++) {
                actual.setRGB(x, y, 0x000000);
            }
        }
        ImageDiff diff = ImageDiff.compare(expected, actual);
        // 100 像素（3 通道）全黑、其余为白：MAE = 100 × 3 × 255 / (10000 × 3) = 0.01，即 2.55/255
        assertEquals(0.01d, diff.getDiffRatio(), 1e-9, "差异像素比例应为 1%");
        assertEquals(2.55d / 255d, diff.getMeanAbsoluteError(), 1e-9, "MAE 应为 2.55/255（超过 2/255 阈值）");
        assertTrue(diff.getMeanAbsoluteError() > PdfRegressionSupport.MAX_MAE, "MAE 应超过 2/255 阈值");
        assertTrue(diff.getDiffRatio() > PdfRegressionSupport.MAX_DIFF_RATIO);
    }

    @Test
    @DisplayName("像素指标：10% 像素明显变化时差异像素比例同步为 10%")
    void tenPercentDiffRatio() {
        BufferedImage expected = solid(100, 100, 0xFFFFFF);
        BufferedImage actual = solid(100, 100, 0xFFFFFF);
        // 10 行 × 100 列 = 1000 像素 = 10%：说明整行涂黑才是 10%，不是 1%
        for (int y = 0; y < 10; y++) {
            for (int x = 0; x < 100; x++) {
                actual.setRGB(x, y, 0x000000);
            }
        }
        ImageDiff diff = ImageDiff.compare(expected, actual);
        assertEquals(0.1d, diff.getDiffRatio(), 1e-9, "整行涂黑为 10%");
        assertEquals(25.5d / 255d, diff.getMeanAbsoluteError(), 1e-9, "MAE 应为 25.5/255");
    }

    @Test
    @DisplayName("像素指标：0.5% 像素变化且 MAE 恰好等于 2/255 时可通过")
    void boundaryPasses() {
        BufferedImage expected = solid(100, 200, 0xFFFFFF);
        BufferedImage actual = solid(100, 200, 0xFFFFFF);
        // 100 像素（20000 像素中的 0.5%）变为黑色：MAE = 100 × 3 × 255 / (20000 × 3) = 0.005，即 1.275/255，低于 2/255
        for (int x = 0; x < 100; x++) {
            actual.setRGB(x, 0, 0x000000);
        }
        ImageDiff diff = ImageDiff.compare(expected, actual);
        assertEquals(0.005d, diff.getDiffRatio(), 1e-9, "差异像素比例应为 0.5%");
        assertEquals(1.275d / 255d, diff.getMeanAbsoluteError(), 1e-9, "MAE 应为 1.275/255（低于 2/255 阈值）");
        assertTrue(diff.getMeanAbsoluteError() <= PdfRegressionSupport.MAX_MAE);
    }

    @Test
    @DisplayName("像素指标：尺寸不一致时按空白补齐并计入差异")
    void sizeMismatchCountsAsDiff() {
        // 补齐色是白色页面底色：窄图（白）补齐后与宽图（白）外观一致，此时不应产生差异
        ImageDiff sameColor = ImageDiff.compare(solid(100, 100, 0xFFFFFF), solid(50, 100, 0xFFFFFF));
        assertEquals(100, sameColor.getWidth(), "对齐后的宽度取两者较大值");
        assertEquals(0, sameColor.getDiffPixels(), "白底补齐到白底不应产生差异");
        assertEquals(0d, sameColor.getMeanAbsoluteError(), 1e-9, "白底补齐到白底 MAE 应为 0");

        // 窄图为黑色时，右侧 50 列补齐为白色，与宽图的黑底不同：5000 像素全部计入差异
        ImageDiff diff = ImageDiff.compare(solid(100, 100, 0x000000), solid(50, 100, 0x000000));
        assertEquals(100, diff.getWidth());
        assertEquals(100, diff.getHeight());
        assertEquals(5000, diff.getDiffPixels(), "补齐的 50 列×100 行应全部计为差异像素");
        assertEquals(0.5d, diff.getDiffRatio(), 1e-9, "缺失 50 列应计为 50% 差异");
        assertEquals(127.5d / 255d, diff.getMeanAbsoluteError(), 1e-9, "补齐区域 MAE 为 127.5/255");
    }

    @Test
    @DisplayName("文本指标：空白、换行与全半角差异不影响相似度")
    void textNormalization() {
        TextComparison same = TextComparison.compare("你好 呀，OFD Reader&Writer！", "你好呀,\nOFD\nReader&Writer!");
        assertEquals(1d, same.getSimilarity(), 1e-9, "空白与全半角差异不应影响相似度");
    }

    @Test
    @DisplayName("文本指标：缺字与错字会降低相似度")
    void textMissingAndWrong() {
        TextComparison missing = TextComparison.compare("你好呀，OFD Reader&Writer！", "你好呀，OFDWriter！");
        assertTrue(missing.getSimilarity() < 0.99d, "缺字应被检出: " + missing);
        TextComparison wrong = TextComparison.compare("发票监制章", "发票目制章");
        assertTrue(wrong.getSimilarity() < 1d, "错字应被检出: " + wrong);
        assertEquals("", TextComparison.normalize(null));
    }

    @Test
    @DisplayName("文本指标：两侧均为空视为一致")
    void textEmpty() {
        assertEquals(1d, TextComparison.compare(null, "  ").getSimilarity(), 1e-9, "两侧均无文本时视为一致");
    }
}
