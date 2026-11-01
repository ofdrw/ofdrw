package org.ofdrw.converter.pdf.font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.model.GlyphRun;
import org.ofdrw.converter.pdf.model.PdfColor;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 字体引用测试：整字体嵌入、字形编码、ToUnicode 登记与资源名分配
 * <p>
 * 样本使用本模块测试资源 {@code src/test/resources/font_13132_0_edit.ttf}
 * （unitsPerEm=256，hhea ascender=220、descender=-37，'A'→gid 102，'/'→gid 402，字形总数 1384）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class TrueTypePdfFontTest {

    /**
     * 测试字体路径
     */
    private static final String FONT = "src/test/resources/font_13132_0_edit.ttf";

    /**
     * 由测试资源创建字体引用
     *
     * @param resourceName 资源名
     * @return 字体引用
     * @throws IOException 读取失败
     */
    private static TrueTypePdfFont font(String resourceName) throws IOException {
        return TrueTypePdfFont.create(resourceName, java.nio.file.Files.readAllBytes(Paths.get(FONT)), 0, false);
    }

    @Test
    @DisplayName("整字体嵌入：嵌入数据与字体文件逐字节一致（决策 16，不子集）")
    void embeddedWholeFont() throws IOException {
        TrueTypePdfFont font = font("F1");
        assertFalse(font.isSubstituted());
        assertEquals("F1", font.getResourceName());
        assertEquals("sysfST", font.getFamilyName());
        byte[] original = java.nio.file.Files.readAllBytes(Paths.get(FONT));
        assertArrayEquals(original, font.getEmbedded().getData());
        assertNotNull(font.getDescriptor());
        assertNotNull(font.getMetrics());
        assertNotNull(font.getGlyphIndexProvider());
        // FontFile2（TrueType 轮廓）
        assertEquals("FontFile2", font.getDescriptor().getFontFileKey());
        assertEquals("sysfST--GB1-0", font.getBaseFontName());
        assertFalse(font.getDescriptor().isRenamingRequired());
    }

    @Test
    @DisplayName("字形编码：字形总数 > 255 时按双字节 CID，超范围不截断")
    void encodeGlyph() throws IOException {
        TrueTypePdfFont font = font("F1");
        // 样本字体有 1384 个字形 → 双字节
        assertEquals(2, font.getCodeByteLength());
        assertEquals(469, font.encodeGlyph(469));
        assertEquals(1000, font.encodeGlyph(1000));
    }

    @Test
    @DisplayName("Identity-H 编码：CID 双字节，字形编号不做 255 截断")
    void encodeGlyphIdentityH() {
        TrueTypePdfFont font = TrueTypePdfFont.withoutFontFile("F2", "Fallback", true);
        // Identity-H 下文本字符串恒为 2 字节 CID，与字形总数无关
        assertEquals(2, font.getCodeByteLength());
        assertEquals(3, font.encodeGlyph(3));
        // 双字节编码可承载 0~65535，超出 255 的字形按原值编码（不得截断为 0）
        assertEquals(300, font.encodeGlyph(300));
        assertTrue(font.isSubstituted());
        assertNull(font.getDescriptor());
        assertNull(font.getEmbedded());
        assertEquals("Fallback", font.getBaseFontName());
    }

    @Test
    @DisplayName("字形解析：显式 GID 优先，其次 cmap，最后回退 0 并记录替换")
    void resolveGlyphId() throws IOException {
        TrueTypePdfFont font = font("F1");
        List<String> warnings = new ArrayList<>();
        // 显式 GID 优先（与 cmap 结果不同）
        assertEquals(469, font.resolveGlyphId(469, new int[]{'A'}, warnings));
        assertTrue(warnings.isEmpty());
        // cmap：'A' → 102
        assertEquals(102, font.resolveGlyphId(-1, new int[]{'A'}, warnings));
        assertTrue(warnings.isEmpty());
        // 无法映射：回退字形 0，并记录替换（决策 7：warning 日志）。
        // 该字体有 cmap，因此说明文本为“字符不在字体中 → 回退字形 0”
        assertEquals(0, font.resolveGlyphId(-1, new int[]{0x10FFFF}, warnings));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("回退字形 0"), "实际：" + warnings.get(0));
        assertTrue(font.hasUnicodeCmap(), "样本字体含 cmap");
    }

    @Test
    @DisplayName("无 cmap 字体：按字符取字形回退 0，并给出“需要 CGTransform 字形索引”的降级原因")
    void resolveWithoutCmap() throws IOException {
        // 构造去掉 cmap 表的字体字节：OFD 内嵌字体常见的裁剪形态
        byte[] data = stripCmap(java.nio.file.Files.readAllBytes(Paths.get(FONT)));
        TrueTypePdfFont font = TrueTypePdfFont.create("F1", data, 0, false);
        assertFalse(font.hasUnicodeCmap(), "裁剪字体不应再有 cmap");
        List<String> warnings = new ArrayList<>();
        assertEquals(0, font.resolveGlyphId(-1, new int[]{'A'}, warnings));
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("没有 cmap 表"), "实际：" + warnings.get(0));
        // 显式 GID 仍可用（CGTransform 场景）
        assertEquals(469, font.resolveGlyphId(469, new int[]{'A'}, new ArrayList<>()));
    }

    /**
     * 去掉 sfnt 中的 cmap 表（重建表目录，保持其他表偏移不变）
     *
     * @param data 字体字节
     * @return 去掉 cmap 后的字体字节
     */
    private static byte[] stripCmap(byte[] data) {
        int numTables = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        int kept = 0;
        byte[] out = data.clone();
        for (int i = 0; i < numTables; i++) {
            int src = 12 + i * 16;
            String tag = new String(data, src, 4, java.nio.charset.StandardCharsets.ISO_8859_1);
            if ("cmap".equals(tag)) {
                continue;
            }
            int dst = 12 + kept * 16;
            System.arraycopy(data, src, out, dst, 16);
            kept++;
        }
        out[4] = (byte) ((kept >>> 8) & 0xFF);
        out[5] = (byte) (kept & 0xFF);
        return out;
    }

    @Test
    @DisplayName("ToUnicode：登记字形映射后生成标准 CMap")
    void toUnicode() throws IOException {
        TrueTypePdfFont font = font("F1");
        assertNull(font.toUnicodeCMap(), "未登记映射时不应输出 ToUnicode");
        font.registerUnicode(102, new int[]{'A'});
        font.registerUnicode(103, new int[]{'B'});
        font.registerUnicode(2000, new int[]{0x4E2D});
        String cmap = font.toUnicodeCMap();
        assertNotNull(cmap);
        assertTrue(cmap.contains("/CIDInit /ProcSet findresource begin"));
        assertTrue(cmap.contains("<0000> <FFFF>"), "双字节 CID 的 codespacerange");
        // 102、103 连续映射压缩为 bfrange，三元组不附带宽度（PDF 1.7 9.10.3，宽度由 /W 决定）
        assertTrue(cmap.contains("<0066> <0067> <0041>\n"), "gid 102~103 → 'A'、'B'");
        assertFalse(cmap.contains("<0066> <0067> <0041> 500"), "bfrange 不应附带宽度");
        assertTrue(cmap.contains("<07D0> <4E2D>"), "gid 2000 → 中");
        // 空码位不登记
        font.registerUnicode(3000, new int[0]);
        assertFalse(font.toUnicodeCMap().contains("<0BB8>"));
    }

    @Test
    @DisplayName("标称步进：按字宽表换算，字号 10pt 下 '/' 为 5pt")
    void nominalAdvance() throws IOException {
        TrueTypePdfFont font = font("F1");
        // 样本字体全部字形的 advanceWidth 为 128 或 256（unitsPerEm=256），标称步进取首个有效字形
        double advance = font.nominalAdvancePt(10);
        assertTrue(advance > 0);
        assertTrue(advance <= 10.0001, "标称步进不应超过 1em：" + advance);
        assertEquals(advance, font.advancePt(org.ofdrw.converter.pdf.CoordSystem.ptToMm(advance), 10), 1e-9);
    }

    @Test
    @DisplayName("资源登记表：资源名递增、字体按对象复用、跨页累积")
    void registry() throws IOException {
        /*
         * 契约：资源名全局唯一、字体表跨页累积（不随 newPage() 清空）。
         * 原因：同一字体在多页出现时是同一个 TrueTypePdfFont 实例，其“字形 → Unicode”映射随页面推进不断追加，
         * 保存前需要据此统一刷新 ToUnicode；因此 newPage() 不清空字体表，资源名也继续递增。
         */
        TrueTypePdfFont.Registry registry = new TrueTypePdfFont.Registry();
        assertEquals("F1", registry.allocateResourceName());
        assertEquals("F2", registry.allocateResourceName());
        TrueTypePdfFont font = font("F1");
        assertTrue(registry.getFonts().isEmpty());
        registry.register(font);
        registry.resolved("font.ttf", font);
        assertEquals(1, registry.getFonts().size());
        assertEquals(font, registry.lookup("font.ttf"));
        assertNull(registry.lookup("other.ttf"));
        // 重复登记同一资源名不重复添加
        registry.register(font);
        assertEquals(1, registry.getFonts().size());
        registry.newPage();
        // 换页不清空字体表：跨页累积是设计行为（全文档统一刷新 ToUnicode），资源名继续递增
        assertEquals(1, registry.getFonts().size(), "newPage 后字体表应保留（跨页累积）");
        assertEquals("F3", registry.allocateResourceName(), "资源名全局唯一、跨页继续递增");
        assertThrows(IllegalArgumentException.class, () -> registry.register(null));
    }

    @Test
    @DisplayName("字形条目：GlyphRun 契约（GID + Unicode + 文本空间位移）")
    void glyphRunContract() {
        GlyphRun run = new GlyphRun(102, "A", 0, 0, 5);
        assertEquals(102, run.getGlyphId());
        assertTrue(run.hasGlyphId());
        assertEquals("A", run.getUnicode());
        assertEquals(5, run.getAdvancePt(), 1e-9);
        GlyphRun unknown = new GlyphRun(-1, null, 1, 2, 0);
        assertFalse(unknown.hasGlyphId());
        assertEquals("", unknown.getUnicode());
        assertEquals(PdfColor.BLACK, PdfColor.BLACK.withOpacity(1f));
    }
}
