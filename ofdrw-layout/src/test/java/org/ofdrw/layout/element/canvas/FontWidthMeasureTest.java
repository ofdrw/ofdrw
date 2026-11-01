package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.font.Font;
import org.ofdrw.font.FontName;
import org.ofdrw.layout.element.TxtGlyph;

import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 字符宽度测量测试
 * <p>
 * 用于验证：字体绑定字体文件时，测量结果应当取自字体文件中的真实字形宽度，
 * 并与 AWT 渲染该字体文件得到的结果一致，而不是采用预设的字符宽度映射表。
 *
 * @author 权观宇
 * @since 2025-11-01
 */
class FontWidthMeasureTest {

    /**
     * 测试用字体文件：Noto Serif CJK SC，其可打印 ASCII 宽度正是映射表的打表来源
     */
    private static final Path NOTO_FONT = Paths.get("src", "test", "resources", "NotoSerifCJKsc-Regular.otf");

    /**
     * 测量字号，单位：毫米
     */
    private static final double FONT_SIZE = 5d;

    /**
     * 字符宽度允许的误差，单位：毫米
     * <p>
     * AWT 以 float 精度计算字宽，与 hmtx 表中整数单位换算出的结果之间存在亚微米级差异，
     * 实测最大约 1.3e-4 mm（字符 'W'、'%'、'M'）。
     */
    private static final double TOLERANCE_MM = 1e-3;

    /**
     * 可打印 ASCII 字符
     */
    private static String printableAscii() {
        StringBuilder sb = new StringBuilder();
        for (char c = 32; c <= 126; c++) {
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 在指定字号下测量字符串的宽度（单位：毫米），直接通过 AWT 渲染被测字体文件
     *
     * @param awtFont AWT 字体对象
     * @param text    待测文本
     * @return 文本宽度，单位：毫米
     */
    private static double awtWidth(java.awt.Font awtFont, String text) {
        FontRenderContext frc = new FontRenderContext(new AffineTransform(), true, true);
        double width = 0;
        for (int i = 0; i < text.length(); i++) {
            width += awtFont.getStringBounds(String.valueOf(text.charAt(i)), frc).getWidth();
        }
        return width;
    }

    /**
     * 加载测试字体的 AWT 对象
     *
     * @return 指定字号的 AWT 字体
     * @throws Exception 字体加载异常
     */
    private static java.awt.Font loadAwtFont() throws Exception {
        Assertions.assertTrue(NOTO_FONT.toFile().exists(), "测试字体文件不存在：" + NOTO_FONT.toAbsolutePath());
        return java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, NOTO_FONT.toFile())
                .deriveFont((float) FONT_SIZE);
    }

    /**
     * 字体绑定字体文件后，应当能够读取真实字形宽度
     *
     * @throws Exception 字体加载异常
     */
    @Test
    void hasRealGlyphWidth() throws Exception {
        Font borderless = new Font("Noto Serif CJK SC", "Noto Serif CJK SC");
        Assertions.assertFalse(borderless.hasRealGlyphWidth(), "未绑定字体文件的字体不应报告可读取真实字形宽度");

        Font withFile = new Font("Noto Serif CJK SC", "Noto Serif CJK SC", NOTO_FONT);
        Assertions.assertTrue(withFile.hasRealGlyphWidth(), "绑定了字体文件的字体应当可以读取真实字形宽度");
    }

    /**
     * 真实字形宽度应当与 AWT 渲染同一字体文件的结果一致
     * <p>
     * 这份报告覆盖全部可打印 ASCII 字符。
     *
     * @throws Exception 字体加载异常
     */
    @Test
    void realGlyphWidthMatchesAwt() throws Exception {
        Font font = new Font("Noto Serif CJK SC", "Noto Serif CJK SC", NOTO_FONT);
        FontSetting fs = new FontSetting(FONT_SIZE, font);
        java.awt.Font awtFont = loadAwtFont();

        StringBuilder report = new StringBuilder();
        report.append(String.format("%-4s %-12s %-12s %-10s%n", "字符", "真实字形宽度", "AWT测量宽度", "差值(mm)"));

        double maxDiff = 0;
        char maxDiffChar = ' ';
        for (char c : printableAscii().toCharArray()) {
            double real = fs.charWidth(c);
            double awt = awtWidth(awtFont, String.valueOf(c));
            double diff = Math.abs(real - awt);
            if (diff > maxDiff) {
                maxDiff = diff;
                maxDiffChar = c;
            }
            if (diff > TOLERANCE_MM) {
                report.append(String.format("'%c'  %.6f    %.6f    %.6f%n", c, real, awt, diff));
            }
        }
        System.out.println(">> 真实字形宽度与 AWT 渲染对比（字号 " + FONT_SIZE + "mm），差异超过 "
                + TOLERANCE_MM + "mm 的字符如下：");
        System.out.println(report);

        Assertions.assertTrue(maxDiff <= TOLERANCE_MM,
                String.format("字符 '%c' 的真实字形宽度与 AWT 测量结果差异 %.6f mm 超过容差 %.6f mm",
                        maxDiffChar, maxDiff, TOLERANCE_MM));
    }

    /**
     * 真实字形宽度应当优于预设的字符宽度映射表
     * <p>
     * 映射表由 canvas 在 1px 字号下打表得到，存在取整误差；真实字形宽度取自字体文件 hmtx 表，没有该误差。
     *
     * @throws Exception 字体加载异常
     */
    @Test
    void realGlyphWidthBeatsWidthMap() throws Exception {
        // 绑定字体文件但仍然携带映射表，验证字体文件优先
        Font font = new Font("Noto Serif CJK SC", "Noto Serif CJK SC", NOTO_FONT,
                FontName.NOTO_PRINTABLE_ASCII_WIDTH_MAP);
        FontSetting fs = new FontSetting(FONT_SIZE, font);
        java.awt.Font awtFont = loadAwtFont();

        double maxDiffReal = 0;
        double maxDiffMap = 0;
        char maxDiffRealChar = ' ';
        char maxDiffMapChar = ' ';
        for (char c : printableAscii().toCharArray()) {
            double awt = awtWidth(awtFont, String.valueOf(c));
            double diffReal = Math.abs(fs.charWidth(c) - awt);
            double diffMap = Math.abs(font.getCharWidthScale(c) * FONT_SIZE - awt);
            if (diffReal > maxDiffReal) {
                maxDiffReal = diffReal;
                maxDiffRealChar = c;
            }
            if (diffMap > maxDiffMap) {
                maxDiffMap = diffMap;
                maxDiffMapChar = c;
            }
        }

        System.out.printf(">> 真实字形宽度 最大偏差 %.6f mm (字符 '%c')%n", maxDiffReal, maxDiffRealChar);
        System.out.printf(">> 映射表宽度   最大偏差 %.6f mm (字符 '%c')%n", maxDiffMap, maxDiffMapChar);

        Assertions.assertTrue(maxDiffReal <= TOLERANCE_MM,
                String.format("字符 '%c' 的真实字形宽度偏差 %.6f mm 超过容差", maxDiffRealChar, maxDiffReal));
        Assertions.assertTrue(maxDiffReal < maxDiffMap,
                String.format("真实字形宽度偏差 %.6f mm 未优于映射表偏差 %.6f mm", maxDiffReal, maxDiffMap));
    }

    /**
     * 排版测量（TxtGlyph）与绘制测量（FontSetting）在绑定字体文件时必须给出一致的宽度
     *
     * @throws Exception 字体加载异常
     */
    @Test
    void glyphAndFontSettingAgree() throws Exception {
        Font font = new Font("Noto Serif CJK SC", "Noto Serif CJK SC", NOTO_FONT,
                FontName.NOTO_PRINTABLE_ASCII_WIDTH_MAP);
        FontSetting fs = new FontSetting(FONT_SIZE, font);
        fs.setLetterSpacing(0.2);

        StringBuilder report = new StringBuilder();
        double maxDiff = 0;
        char maxDiffChar = ' ';
        for (char c : printableAscii().toCharArray()) {
            TxtGlyph glyph = new TxtGlyph(c, fs);
            double viaGlyph = glyph.getW();
            // TxtGlyph.getW 含字间距，FontSetting.charWidth 不含
            double viaSetting = fs.charWidth(c) + fs.getLetterSpacing();
            double diff = Math.abs(viaGlyph - viaSetting);
            if (diff > maxDiff) {
                maxDiff = diff;
                maxDiffChar = c;
            }
            if (diff > TOLERANCE_MM) {
                report.append(String.format("'%c' TxtGlyph=%.6f FontSetting=%.6f 差=%.6f%n",
                        c, viaGlyph, viaSetting, diff));
            }
        }
        System.out.println(">> TxtGlyph 与 FontSetting 宽度一致性检查，差异字符如下：");
        System.out.println(report);
        Assertions.assertTrue(maxDiff <= TOLERANCE_MM,
                String.format("字符 '%c' 的两条测量路径差异 %.6f mm", maxDiffChar, maxDiff));
    }

    /**
     * 未绑定字体文件的字体应当保持原有行为，仍然使用映射表
     * <p>
     * 该用例用于确认本次改动没有破坏 Times New Roman 等依赖映射表的既有场景。
     */
    @Test
    void widthMapStillUsedWhenNoFontFile() {
        Font times = FontName.TimesNewRoman.font();
        Assertions.assertTrue(times.hasWidthMath(), "Times New Roman 应当携带映射表");
        Assertions.assertFalse(times.hasRealGlyphWidth(), "未绑定字体文件时不应读取真实字形宽度");

        FontSetting fs = new FontSetting(FONT_SIZE, times);
        // 映射表中 '1' 的比例为 0.46326171875，乘以字号即为宽度
        double expected = 0.46326171875 * FONT_SIZE;
        Assertions.assertEquals(expected, fs.charWidth('1'), 1e-9, "未绑定字体文件时应当继续使用映射表");
    }

    /**
     * 字体文件缺少某个字符的字形时，应当回退到原有估算方式
     * <p>
     * 子集字体常见的场景：字体只包含部分字形，缺失的字符在 cmap 中查不到（gid 为 0），
     * 此时不应采用该字体的 .notdef 字形宽度，以保持既有排版行为不变。
     */
    @Test
    void missingGlyphFallsBackToOldEstimate() {
        // 该字体仅包含 - H W d e l o r 等少量字形，不包含汉字与 'O'
        Path subsetFont = Paths.get("src", "test", "resources", "simhei-cut1.ttf");
        Assertions.assertTrue(subsetFont.toFile().exists(), "测试字体文件不存在：" + subsetFont.toAbsolutePath());
        Font font = new Font("宋体", "宋体", subsetFont);

        Assertions.assertTrue(font.hasRealGlyphWidth(), "子集字体绑定了字体文件，应具备读取真实字形宽度的能力");
        // 缺失的字符返回 -1，由调用方回退
        Assertions.assertEquals(-1d, font.getRealCharWidthScale('你'), 1e-9, "缺失字形应返回 -1");
        Assertions.assertEquals(-1d, font.getRealCharWidthScale('O'), 1e-9, "缺失字形应返回 -1");
        // 存在的字符可以读到真实宽度：该字体中 'W' 的 advance 为 0.5 em
        Assertions.assertEquals(0.5d, font.getRealCharWidthScale('W'), 1e-9, "'W' 应读取到真实字形宽度");

        FontSetting fs = new FontSetting(FONT_SIZE, font);
        // 缺失字形时回退到原有测量路径：该字体未携带映射表，因此走 AWT 测量，
        // AWT 对缺失字符使用 .notdef 字形，其宽度为 1 em
        Assertions.assertEquals(FONT_SIZE, fs.charWidth('你'), 1e-9, "缺失字形应回退到 AWT 测量");
        Assertions.assertEquals(FONT_SIZE, fs.charWidth('O'), 1e-9, "缺失字形应回退到 AWT 测量");
        // 'W' 在字体中存在，按真实字形宽度 0.5 em 计算
        Assertions.assertEquals(FONT_SIZE * 0.5, fs.charWidth('W'), 1e-9, "'W' 应使用真实字形宽度");
    }
}
