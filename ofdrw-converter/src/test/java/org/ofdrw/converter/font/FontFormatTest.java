package org.ofdrw.converter.font;

import org.apache.fontbox.ttf.GlyphDescription;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字体格式识别固化测试。
 * <p>
 * 固化字体解析修复的成果：
 * <ol>
 *     <li>识别首4字节为 {@code 0x74727565}（"true"）的 TrueType sfnt 字体；</li>
 *     <li>识别裸 CFF 数据（{@code 0x01000402} / {@code 0x01000403}）；</li>
 *     <li>字形索引越界时返回 null 而不是抛出空指针异常。</li>
 * </ol>
 * 全部输入均来自本仓库 {@code src/test/resources}，测试运行期不访问任何外部样本目录。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class FontFormatTest {

    /**
     * 标准 TrueType sfnt 字体（首4字节 {@code 0x00010000}，无 cmap 表）
     */
    private static final String FONT_SFNT = "src/test/resources/font_10.ttf";

    /**
     * 裸 CFF 字体（首4字节 {@code 0x01000402}）
     * <p>
     * 来源：本地样本 {@code ofd_pdf_match/signout.ofd} 内嵌字体 {@code Doc_0/Res/font_3_3.ttf}，
     * 体积 684 字节，PostScript 名 {@code UCTIWJ+MinionPro-Regular}；仅包含字形轮廓数据，不含文本内容。
     */
    private static final String FONT_BARE_CFF_0402 = "src/test/resources/font_bare_cff_0402.cff";

    /**
     * "true" 版本标记（sfnt 的另一种合法版本号写法）
     */
    private static final byte[] TRUE_VERSION = new byte[]{0x74, 0x72, 0x75, 0x65};

    /**
     * 裸 CFF 头部的版本标记 {@code 0x01000402}
     */
    private static final byte[] BARE_CFF_VERSION_0402 = new byte[]{0x01, 0x00, 0x04, 0x02};

    /**
     * 裸 CFF 头部的版本标记 {@code 0x01000403}
     */
    private static final byte[] BARE_CFF_VERSION_0403 = new byte[]{0x01, 0x00, 0x04, 0x03};

    /**
     * 读取测试资源字体字节
     *
     * @param path 相对工作目录的字体文件路径
     * @return 字体全部字节
     * @throws IOException 读取失败
     */
    private static byte[] fontBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    /**
     * 构造把首4字节改写为新版本标记的字体数据
     *
     * @param original 原字体字节
     * @param version  新的首4字节
     * @return 改写后的字体字节
     */
    private static byte[] withVersion(byte[] original, byte[] version) {
        byte[] copy = original.clone();
        System.arraycopy(version, 0, copy, 0, version.length);
        return copy;
    }

    /**
     * 比较两个字体解析结果的字形与变换矩阵
     *
     * @param expected 期望的字体
     * @param actual   实际的字体
     * @throws IOException 解析失败
     */
    private static void assertSameParsing(TrueTypeFont expected, TrueTypeFont actual) throws IOException {
        assertEquals(expected.getFontMatrix(), actual.getFontMatrix(), "字体变换矩阵应一致");
        GlyphDescription expectedGlyph = expected.getGlyph(469).getDescription();
        GlyphDescription actualGlyph = actual.getGlyph(469).getDescription();
        assertEquals(expectedGlyph.getContourCount(), actualGlyph.getContourCount(), "字形轮廓数应一致");
        assertEquals(expectedGlyph.getPointCount(), actualGlyph.getPointCount(), "字形绘制点数应一致");
        assertFalse(actualGlyph.isComposite(), "字形应为简单轮廓");
    }

    /**
     * "true" 版本标记（0x74727565）应按 TrueType sfnt 解析，结果与标准版本号一致
     *
     * @throws IOException 解析失败
     */
    @Test
    @DisplayName("字体格式：\"true\" 版本标记按 sfnt 解析")
    void trueVersionTagIsParsedAsSfnt() throws IOException {
        final byte[] original = fontBytes(FONT_SFNT);
        // 前置条件：原文件首4字节为 0x00010000
        assertEquals(0x00, original[0]);
        assertEquals(0x01, original[1]);
        assertEquals(0x00, original[2]);
        assertEquals(0x00, original[3]);

        TrueTypeFont standard = new TrueTypeFont().parse(original);
        TrueTypeFont trueTag = new TrueTypeFont().parse(withVersion(original, TRUE_VERSION));

        // 改写版本号后仍能完整解析
        assertNotNull(trueTag);
        assertEquals(standard.psName, trueTag.psName);
        assertEquals(standard.fontFamily, trueTag.fontFamily);
        assertSameParsing(standard, trueTag);

        // gid=469 为 2 段轮廓、16 个绘制点
        assertEquals(2, trueTag.getGlyph(469).getDescription().getContourCount());
        assertEquals(16, trueTag.getGlyph(469).getDescription().getPointCount());
        assertEquals(standard.getFontMatrix(), trueTag.getFontMatrix());
    }

    /**
     * 裸 CFF（0x01000402）应跳过 sfnt 表目录直接按 CFF 解析
     *
     * @throws IOException 解析失败
     */
    @Test
    @DisplayName("字体格式：裸 CFF(0x01000402) 按 CFF 解析")
    void bareCff0402IsParsed() throws IOException {
        final byte[] cffData = fontBytes(FONT_BARE_CFF_0402);
        assertTrue(cffData.length > 4);
        // 前置条件：测试资源确为 0x01000402 开头
        for (int i = 0; i < BARE_CFF_VERSION_0402.length; i++) {
            assertEquals(BARE_CFF_VERSION_0402[i], cffData[i], "测试字体首4字节应为 0x01000402");
        }

        TrueTypeFont font = new TrueTypeFont().parse(cffData);
        // CFF 的 PostScript 名来自 CFF Name INDEX
        assertEquals("UCTIWJ+MinionPro-Regular", font.psName);
        // 裸 CFF 无 head 表，单位数按 1000 处理，矩阵为 0.001 缩放
        List<Number> matrix = font.getFontMatrix();
        assertEquals(6, matrix.size());
        assertEquals(0.001f, matrix.get(0).floatValue(), 1e-9);
        assertEquals(0.001f, matrix.get(3).floatValue(), 1e-9);

        // 字形数量来自 CharStrings INDEX（3 个字形），越界返回 null
        assertNotNull(font.getGlyph(0));
        assertNotNull(font.getGlyph(2));
        assertNull(font.getGlyph(3));

        // 首个字形能转出非空绘制路径
        GeneralPath path = font.getPath(0);
        assertNotNull(path);
        assertTrue(path.getBounds2D().getWidth() > 0);
        assertTrue(path.getBounds2D().getHeight() > 0);
    }

    /**
     * 裸 CFF 头部次版本号 0x0403 同样按 CFF 解析（复用同一份 CFF 数据改写头部）
     *
     * @throws IOException 解析失败
     */
    @Test
    @DisplayName("字体格式：裸 CFF(0x01000403) 按 CFF 解析")
    void bareCff0403IsParsed() throws IOException {
        // 仓库内没有 0x01000403 的样本，这里改写首4字节头部，CFF 结构不变
        final byte[] cffData = withVersion(fontBytes(FONT_BARE_CFF_0402), BARE_CFF_VERSION_0403);
        for (int i = 0; i < BARE_CFF_VERSION_0403.length; i++) {
            assertEquals(BARE_CFF_VERSION_0403[i], cffData[i]);
        }

        TrueTypeFont font = new TrueTypeFont().parse(cffData);
        assertEquals("UCTIWJ+MinionPro-Regular", font.psName);
        assertEquals(0.001f, font.getFontMatrix().get(0).floatValue(), 1e-9);
        assertNotNull(font.getGlyph(0));
        assertNull(font.getGlyph(3));
    }

    /**
     * 字形索引越界时应返回 null，且不抛出空指针异常
     *
     * @throws IOException 解析失败
     */
    @Test
    @DisplayName("字形索引越界：返回 null 且不抛 NPE")
    void outOfRangeGidReturnsNull() throws IOException {
        TrueTypeFont sfnt = new TrueTypeFont().parse(Paths.get(FONT_SFNT));
        assertNull(sfnt.getPath(-1));
        assertNull(sfnt.getGlyph(-1));
        assertNull(sfnt.getPath(1000000));
        assertNull(sfnt.getGlyph(1000000));
        // 边界内的字形仍可正常获取
        assertNotNull(sfnt.getGlyph(469));
        assertNotNull(sfnt.getPath(469));

        TrueTypeFont cff = new TrueTypeFont().parse(Paths.get(FONT_BARE_CFF_0402));
        assertNull(cff.getPath(-1));
        assertNull(cff.getGlyph(-1));
        assertNull(cff.getPath(1000000));
        assertNull(cff.getGlyph(1000000));
    }
}
