package org.ofdrw.converter;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.core.basicType.ST_Array;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字符步进兜底测试（DeltaX 缺失时按字体字宽估算）
 * <p>
 * 通过 AWTMaker 的字符步进估算方法，验证 DeltaX 缺失时字符位置单调递增且间距合理。
 *
 * @author 权观宇
 * @since 2024-10-24
 */
class AWTMakerAdvanceWidthTest {

    /**
     * 测试用字体，汉字字宽为 256，unitsPerEm 为 256
     */
    private static final String FONT_PATH = "src/test/resources/font_13132_0_edit.ttf";

    /**
     * 字号，单位为毫米（mm）
     */
    private static final double FONT_SIZE = 3.0;

    /**
     * 测试用文字内容，全为全角汉字，每个字符占 1 个 em
     */
    private static final String CONTENT = "中文字体";

    @Test
    void shouldFallbackToAdvanceWidthWhenDeltaXMissing() throws Exception {
        TrueTypeFont font = new TrueTypeFont().parse(Paths.get(FONT_PATH));
        char[] chars = CONTENT.toCharArray();

        List<Double> xPositions = placeChars(font, chars, null);

        double step = expectedStep(font);
        assertEquals(CONTENT.length(), xPositions.size());
        assertEquals(0.0, xPositions.get(0), 1e-9);
        for (int i = 1; i < xPositions.size(); i++) {
            assertTrue(xPositions.get(i) > xPositions.get(i - 1), "字符位置应单调递增: " + xPositions);
            assertEquals(step, xPositions.get(i) - xPositions.get(i - 1), 1e-9);
        }
    }

    @Test
    void shouldFallbackToAdvanceWidthWhenDeltaXTooShort() throws Exception {
        TrueTypeFont font = new TrueTypeFont().parse(Paths.get(FONT_PATH));
        char[] chars = CONTENT.toCharArray();

        // DeltaX 只有一个数值，与字符数不匹配，全部字符按字宽估算步进
        List<Double> xPositions = placeChars(font, chars, new ST_Array(1.5));

        double step = expectedStep(font);
        assertEquals(CONTENT.length(), xPositions.size());
        for (int i = 1; i < xPositions.size(); i++) {
            assertTrue(xPositions.get(i) > xPositions.get(i - 1), "字符位置应单调递增: " + xPositions);
            assertEquals(step, xPositions.get(i) - xPositions.get(i - 1), 1e-9);
        }
    }

    @Test
    void shouldEstimateReasonableStepFromFontMetrics() throws Exception {
        TrueTypeFont font = new TrueTypeFont().parse(Paths.get(FONT_PATH));
        // 该字体汉字字宽 256、unitsPerEm 256，渲染后每个汉字占 1 个 em（3mm）
        assertEquals(256, font.getAdvanceWidth(2));
        assertEquals(FONT_SIZE, expectedStep(font), 1e-9);

        List<Double> xPositions = placeChars(font, CONTENT.toCharArray(), null);
        double step = xPositions.get(1) - xPositions.get(0);
        // 间距合理：不小于半个全角字符，也不超过两个全角字符
        assertTrue(step >= FONT_SIZE / 2 && step <= FONT_SIZE * 2, "步进应在合理范围内: " + step);
    }

    /**
     * 按 AWTMaker.writeText 的规则，依次计算每个字符的 X 位置
     * <p>
     * DeltaX 数量与字符数匹配时使用 DeltaX，缺失或长度不足时使用 AWTMaker 的步进估算。
     *
     * @param font   绘制使用的字体
     * @param chars  字符序列
     * @param deltaX 字符偏移量，null 表示缺失
     * @return 各字符的 X 位置，单位为毫米（mm）
     * @throws Exception 字体解析异常
     */
    private static List<Double> placeChars(TrueTypeFont font, char[] chars, ST_Array deltaX) throws Exception {
        List<Double> delta = AWTMaker.parseDelta(deltaX);
        List<Number> fontMatrix = font.getFontMatrix();
        List<Double> xPositions = new ArrayList<>(chars.length);
        double x = 0;
        for (int i = 0; i < chars.length; i++) {
            if (i > 0) {
                if (delta.size() >= chars.length) {
                    x += delta.get(i - 1);
                } else {
                    int gid = font.getUnicodeCmapLookup().getGlyphId(chars[i - 1]);
                    x += AWTMaker.estimateAdvanceWidth(font, gid, FONT_SIZE, fontMatrix);
                }
            }
            xPositions.add(x);
        }
        return xPositions;
    }

    /**
     * 计算期望的字符步进
     *
     * @param font 字体
     * @return 步进量，单位为毫米（mm）
     */
    private static double expectedStep(TrueTypeFont font) {
        double scaleX = Math.abs(font.getFontMatrix().get(0).doubleValue());
        return font.getAdvanceWidth(2) * scaleX * FONT_SIZE;
    }
}
