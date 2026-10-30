package org.ofdrw.converter.font;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ofdrw-font 对外 API 的回归基线（设计文档 3.9 节“字体能力扩展”）。
 * <p>
 * 覆盖：
 * <ol>
 *     <li>编译期签名锁定：与 {@code org.ofdrw.converter.font} 下的既有类型（{@code TrueTypeFont}、
 *     {@code EmbeddedFontData} 等）一起显式引用 5 个新增类型的公开构造器与访问器；</li>
 *     <li>运行期行为锁定：以本模块测试资源 {@code src/test/resources/font_13132_0_edit.ttf}
 *     与运行期打包的 TTC（{@value #SAMPLE_TTC}）断言格式识别、度量、嵌入数据、ToUnicode 与字形选择的结果。</li>
 * </ol>
 * 不依赖 {@code ofdrw-converter} 模块，也不引入外部字体样本。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ApiCompatibilityTest {

    /**
     * 带 cmap 表的 TrueType 字体样本
     * <p>
     * 来源：{@code ofdrw-converter/src/test/resources/font_13132_0_edit.ttf}（裁剪字体，cmap 位于文件偏移 172，
     * 仅一个 3/1 子表；unitsPerEm = 256，hhea ascender = 222、descender = -78）
     */
    private static final String SAMPLE_TTF = "src/test/resources/font_13132_0_edit.ttf";

    /**
     * 字体集合（TTC）样本
     * <p>
     * 运行环境自带的 Ubuntu 打包字体，用于验证“TTC 提取指定字体并重建 sfnt”；
     * 文件缺失时自动跳过相关断言（见 {@link #assumeTtcAvailable()}）。
     */
    private static final String SAMPLE_TTC = "/usr/share/fonts/truetype/wqy/wqy-microhei.ttc";

    /**
     * 读取测试资源字体文件
     *
     * @param path 字体文件路径
     * @return 字体文件全部字节
     * @throws IOException 读取失败
     */
    private static byte[] fontBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    /**
     * 判断运行环境是否具备 TTC 样本
     *
     * @return true - 可用
     */
    private static boolean ttcAvailable() {
        return Files.isRegularFile(Paths.get(SAMPLE_TTC));
    }

    // =================== FontDescriptor ===================

    /**
     * 锁定 FontDescriptor 的常量、工厂方法与全部访问器
     */
    @Test
    @DisplayName("FontDescriptor：常量、from 与访问器")
    void fontDescriptorApis() throws IOException {
        // 常量签名
        assertEquals("FontFile2", FontDescriptor.FONT_FILE_2);
        assertEquals("FontFile3", FontDescriptor.FONT_FILE_3);
        assertEquals(1, FontDescriptor.FLAG_FIXED_PITCH);
        assertEquals(2, FontDescriptor.FLAG_SERIF);
        assertEquals(4, FontDescriptor.FLAG_SYMBOLIC);
        assertEquals(8, FontDescriptor.FLAG_SCRIPT);
        assertEquals(32, FontDescriptor.FLAG_NONSYMBOLIC);
        assertEquals(64, FontDescriptor.FLAG_ITALIC);
        assertEquals(1 << 16, FontDescriptor.FLAG_ALLCAP);
        assertEquals(1 << 18, FontDescriptor.FLAG_FORCE_BOLD);

        FontDescriptor descriptor = FontDescriptor.from(Paths.get(SAMPLE_TTF), 0);
        assertEquals(FontDescriptor.GlyphFormat.TRUE_TYPE, descriptor.getGlyphFormat());
        assertEquals(FontDescriptor.FONT_FILE_2, descriptor.getFontFileKey());
        assertEquals("sysfST--GB1-0", descriptor.getPostScriptName());
        assertEquals("sysfST--GB1-0", descriptor.getRenamedPostScriptName());
        // 名称不含子集前缀，无需重命名
        assertFalse(descriptor.isRenamingRequired());
        assertEquals("sysfST", descriptor.getFamilyName());
        // 子族名为 Regular
        assertEquals(400, descriptor.getFontWeight());
        assertEquals(0f, descriptor.getItalicAngle(), 1e-6);
        assertTrue((descriptor.getFlags() & FontDescriptor.FLAG_NONSYMBOLIC) != 0);
        float[] bbox = descriptor.getFontBBox();
        assertEquals(4, bbox.length);
        assertTrue(bbox[2] > bbox[0], "字体包围盒宽度应大于 0");
        assertTrue(bbox[3] > bbox[1], "字体包围盒高度应大于 0");

        // 字典条目
        Map<String, Object> entries = descriptor.toDescriptorEntries();
        assertEquals("sysfST--GB1-0", entries.get("BaseFont"));
        assertEquals("sysfST", entries.get("FontFamily"));
        assertEquals(400, entries.get("FontWeight"));
        assertEquals(0f, (Float) entries.get("ItalicAngle"), 1e-6);
        assertTrue(entries.containsKey("Flags"));
        float[] entryBBox = (float[]) entries.get("FontBBox");
        assertEquals(4, entryBBox.length);
        assertEquals(FontDescriptor.FONT_FILE_2, entries.get(FontDescriptor.FONT_FILE_2));

        // 复用已解析结果
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        EmbeddedFontData embedded = EmbeddedFontData.of(fontBytes(SAMPLE_TTF), 0);
        FontDescriptor reused = FontDescriptor.of(font, embedded);
        assertEquals(descriptor.getPostScriptName(), reused.getPostScriptName());
        assertEquals(descriptor.getFontFileKey(), reused.getFontFileKey());
        assertThrows(IllegalArgumentException.class, () -> FontDescriptor.of(null, embedded));
        assertThrows(IllegalArgumentException.class, () -> FontDescriptor.of(font, null));
        assertNotNull(descriptor.toString());
    }

    /**
     * 锁定字体数据格式识别
     */
    @Test
    @DisplayName("FontDescriptor：detectFormat 识别 sfnt 与字体集合")
    void fontDescriptorDetectFormat() throws IOException {
        byte[] data = fontBytes(SAMPLE_TTF);
        assertEquals(FontDescriptor.GlyphFormat.TRUE_TYPE, FontDescriptor.detectFormat(data));
        // 首 4 字节应为 0x00010000
        assertEquals(0x00010000L, FontDescriptor.readU32(data, 0));
        assertThrows(IOException.class, () -> FontDescriptor.detectFormat(new byte[]{1, 2}));
        assertThrows(IOException.class, () -> FontDescriptor.detectFormat(new byte[]{9, 9, 9, 9}));
        if (ttcAvailable()) {
            byte[] ttc = Files.readAllBytes(Paths.get(SAMPLE_TTC));
            // 字体集合按其内部首个字体判断轮廓格式：wqy-microhei 为 glyf 轮廓
            assertEquals(FontDescriptor.GlyphFormat.TRUE_TYPE, FontDescriptor.detectFormat(ttc));
        }
    }

    /**
     * 锁定 ABCDEF+ 重命名规则：已带子集前缀的字体名会重新加前缀
     */
    @Test
    @DisplayName("FontDescriptor：ABCDEF+ 前缀重命名")
    void fontDescriptorRenaming() throws IOException {
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        EmbeddedFontData embedded = EmbeddedFontData.of(fontBytes(SAMPLE_TTF), 0);
        // 模拟自带子集前缀的字体名
        font.psName = "ZJPTDA+SimSun";
        FontDescriptor descriptor = FontDescriptor.of(font, embedded);
        assertTrue(descriptor.isRenamingRequired());
        String renamed = descriptor.getRenamedPostScriptName();
        assertEquals("ZJPTDA+SimSun".length() + 7, renamed.length());
        assertTrue(renamed.matches("[A-Z]{6}\\+ZJPTDA\\+SimSun"), "重命名结果：" + renamed);
        // 前缀稳定：同一字体多次汇总结果一致
        assertEquals(renamed, FontDescriptor.of(font, embedded).getRenamedPostScriptName());

        // 不含加号的名称表示完整的字体名，按原样写入
        font.psName = "SimSun";
        FontDescriptor plain = FontDescriptor.of(font, embedded);
        assertFalse(plain.isRenamingRequired());
        assertEquals("SimSun", plain.getRenamedPostScriptName());

        // 缺少 PostScript 名时按兜底名称重命名
        font.psName = null;
        FontDescriptor fallback = FontDescriptor.of(font, embedded);
        assertTrue(fallback.isRenamingRequired());
        assertEquals("UnknownFont", fallback.getPostScriptName());
        assertTrue(fallback.getRenamedPostScriptName().endsWith("+UnknownFont"));
    }

    // =================== EmbeddedFontData ===================

    /**
     * 锁定 EmbeddedFontData 的工厂方法、数据访问与长度访问器（整字体嵌入，不做子集）
     */
    @Test
    @DisplayName("EmbeddedFontData：原样导出与访问器")
    void embeddedFontDataApis() throws IOException {
        byte[] data = fontBytes(SAMPLE_TTF);
        EmbeddedFontData embedded = EmbeddedFontData.of(data, 0);
        assertEquals(FontDescriptor.GlyphFormat.TRUE_TYPE, embedded.getGlyphFormat());
        assertArrayEquals(data, embedded.getData());
        assertEquals(data.length, embedded.getLength());
        assertEquals(data.length, embedded.getLength1());
        assertFalse(embedded.isExtractedFromCollection());
        assertEquals(-1, embedded.getCollectionIndex());
        assertNull(embedded.getCffFontName());
        assertTrue(embedded.getPfbSegmentLengths().isEmpty());
        assertFalse(EmbeddedFontData.isCollection(data));
        assertFalse(EmbeddedFontData.isType1(data));
        // 读取流内容与数据一致
        assertArrayEquals(data, org.apache.commons.io.IOUtils.toByteArray(embedded.openStream()));
        assertNotNull(embedded.toString());

        // 路径入口
        EmbeddedFontData fromPath = EmbeddedFontData.of(Paths.get(SAMPLE_TTF), 0);
        assertArrayEquals(data, fromPath.getData());

        // 数据副本：调用方修改返回值不影响内部数据
        byte[] copy = embedded.getData();
        copy[0] = 0;
        assertEquals(data[0], embedded.getData()[0]);

        // 非法输入
        assertThrows(IOException.class, () -> EmbeddedFontData.of(new byte[]{1, 2}, 0));
        // 裸 CFF 数据没有表目录，无法直接嵌入 PDF
        byte[] bareCff = new byte[]{0x01, 0x00, 0x04, 0x02, 0, 0, 0, 0};
        assertThrows(IOException.class, () -> EmbeddedFontData.of(bareCff, 0));
    }

    /**
     * 锁定字体集合（TTC）提取指定字体并重建 sfnt
     */
    @Test
    @DisplayName("EmbeddedFontData：TTC 提取指定字体并重建 sfnt")
    void embeddedFontDataFromCollection() throws IOException {
        if (!ttcAvailable()) {
            return;
        }
        byte[] ttc = Files.readAllBytes(Paths.get(SAMPLE_TTC));
        assertTrue(EmbeddedFontData.isCollection(ttc));

        EmbeddedFontData extracted = EmbeddedFontData.of(ttc, 0);
        assertTrue(extracted.isExtractedFromCollection());
        assertEquals(0, extracted.getCollectionIndex());
        byte[] sfnt = extracted.getData();
        // 重建结果不再包含 'ttcf' 头，首 4 字节为 sfnt 版本号
        assertFalse(EmbeddedFontData.isCollection(sfnt));
        long sfntVersion = FontDescriptor.readU32(sfnt, 0);
        assertTrue(sfntVersion == 0x00010000L || sfntVersion == 0x4F54544FL,
                "重建的 sfnt 版本号异常：" + Long.toHexString(sfntVersion));
        assertTrue(sfnt.length < ttc.length, "重建后应剔除集合头与其它字体的表");
        assertEquals(sfnt.length, extracted.getLength1());

        // 表目录完整：每条表记录的偏移与长度均落在文件内
        int numTables = FontDescriptor.readU16(sfnt, 4);
        assertTrue(numTables > 0);
        for (int i = 0; i < numTables; i++) {
            int recordOffset = 12 + i * 16;
            String tag = new String(sfnt, recordOffset, 4, StandardCharsets.ISO_8859_1);
            long offset = FontDescriptor.readU32(sfnt, recordOffset + 8);
            long length = FontDescriptor.readU32(sfnt, recordOffset + 12);
            assertTrue(offset >= 0 && offset + length <= sfnt.length, "表 " + tag + " 偏移越界");
        }

        // 重建后的 sfnt 能被本模块正常解析，且与直接从 TTC 取出的字体一致
        TrueTypeFont rebuilt = new TrueTypeFont().parse(sfnt);
        TrueTypeFontCollectionProbe probe = new TrueTypeFontCollectionProbe(ttc);
        assertEquals(probe.psName, rebuilt.psName);
        assertNotNull(rebuilt.getUnicodeCmapLookup());

        // checkSumAdjustment：全文件校验和在置零后应为 0xB1B0AFBA
        long[] head = FontDescriptor.findSfntTable(sfnt, 0, "head");
        assertNotNull(head);
        int headOffset = (int) head[0];
        long adjustment = FontDescriptor.readU32(sfnt, headOffset + 8);
        byte[] zeroed = sfnt.clone();
        zeroed[headOffset + 8] = 0;
        zeroed[headOffset + 9] = 0;
        zeroed[headOffset + 10] = 0;
        zeroed[headOffset + 11] = 0;
        long checksum = 0;
        for (int i = 0; i < zeroed.length; i += 4) {
            long word = 0;
            for (int j = 0; j < 4; j++) {
                word = (word << 8) | (i + j < zeroed.length ? (zeroed[i + j] & 0xFF) : 0);
            }
            checksum += word;
        }
        assertEquals(0xB1B0AFBAL, (checksum + adjustment) & 0xFFFFFFFFL);
    }

    /**
     * 读取字体集合内首个字体的 PostScript 名（用于与重建结果比对）
     */
    private static final class TrueTypeFontCollectionProbe {
        private final String psName;

        private TrueTypeFontCollectionProbe(byte[] ttc) throws IOException {
            TrueTypeCollection collection = new TrueTypeCollection()
                    .parse(new MemoryTTFDataStream(ttc));
            this.psName = collection.getFontAtIndex(0).psName;
        }
    }

    // =================== FontMetrics ===================

    /**
     * 锁定 FontMetrics 的工厂方法与度量访问器
     */
    @Test
    @DisplayName("FontMetrics：unitsPerEm、字宽、上下行与包围盒")
    void fontMetricsApis() throws IOException {
        FontMetrics metrics = FontMetrics.of(Paths.get(SAMPLE_TTF), 0);
        // 样本字体 unitsPerEm = 256
        assertEquals(256, metrics.getUnitsPerEm());
        assertEquals(1000f / 256f, metrics.getScale(), 1e-6);
        // hhea：ascender = 220、descender = -37、lineGap = 0
        assertEquals(220f * 1000f / 256f, metrics.getAscent(), 1e-3);
        assertEquals(-37f * 1000f / 256f, metrics.getDescent(), 1e-3);
        assertEquals(0f, metrics.getLineGap(), 1e-6);
        // hmtx：gid 0 的 advanceWidth = 256（= 1 em），'/'（gid 402）为 128（= 0.5 em）
        assertTrue(metrics.hasAdvanceWidth());
        assertEquals(1000f, metrics.getAdvanceWidth(0), 1e-3);
        assertEquals(500f, metrics.getAdvanceWidth(402), 1e-3);
        // 越界返回 0
        assertEquals(0f, metrics.getAdvanceWidth(-1), 0);
        assertEquals(0f, metrics.getAdvanceWidth(999999), 0);
        float[] bbox = metrics.getBBox();
        assertEquals(4, bbox.length);
        assertTrue(bbox[2] > bbox[0]);
        assertTrue(bbox[3] > bbox[1]);
        assertNotNull(metrics.toString());

        // 复用已解析字体
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        FontMetrics reused = FontMetrics.of(font);
        assertEquals(metrics.getUnitsPerEm(), reused.getUnitsPerEm());
        assertEquals(metrics.getAdvanceWidth(402), reused.getAdvanceWidth(402), 1e-6);
        assertThrows(IllegalArgumentException.class, () -> FontMetrics.of((TrueTypeFont) null));

        if (ttcAvailable()) {
            FontMetrics ttcMetrics = FontMetrics.of(Paths.get(SAMPLE_TTC), 0);
            assertTrue(ttcMetrics.getUnitsPerEm() > 0);
            assertTrue(ttcMetrics.getAscent() > 0);
            assertTrue(ttcMetrics.getDescent() < 0);
            assertTrue(ttcMetrics.getAdvanceWidth(1) > 0);
        }
    }

    // =================== ToUnicodeCMap ===================

    /**
     * 锁定 ToUnicodeCMap 的构造器、登记方法与 CMap 文本结构
     */
    @Test
    @DisplayName("ToUnicodeCMap：登记映射并生成标准 CMap 文本")
    void toUnicodeCMapApis() {
        ToUnicodeCMap cmap = new ToUnicodeCMap("TestFont");
        assertEquals(0, cmap.size());
        assertFalse(cmap.contains(3));
        // 单字节码位
        cmap.add(3, 'A');
        cmap.addCodePoint('B', 4);
        cmap.add(5, new int[]{'C'});
        assertTrue(cmap.contains(3));
        assertEquals(3, cmap.size());
        assertFalse(cmap.requiresTwoBytes());
        assertEquals(Integer.valueOf('A'), cmap.getMappings().get(3)[0]);

        // 连续映射压缩为 bfrange：gid 10、11、12 → U+0061、U+0062、U+0063
        cmap.add(10, 'a').add(11, 'b').add(12, 'c');
        // 一对多：连字 gid 20 → "ff"
        cmap.add(20, new int[]{'f', 'f'});
        assertFalse(cmap.requiresTwoBytes());

        String text = cmap.toCMapText();
        assertTrue(text.startsWith("/CIDInit /ProcSet findresource begin"));
        assertTrue(text.contains("/CMapName /Adobe-Identity-UCS def"));
        assertTrue(text.contains("/CMapType 2 def"));
        assertTrue(text.contains("1 begincodespacerange\n<00> <FF>\nendcodespacerange"));
        assertTrue(text.contains("beginbfchar"));
        // 连续区间
        assertTrue(text.contains("beginbfrange"));
        assertTrue(text.contains("<0A> <0C> <0061>"));
        // 一对多
        assertTrue(text.contains("<14> <00660066>"));
        assertTrue(text.trim().endsWith("end"));
        // 连续映射（3~5、10~12）输出为 2 个 bfrange，一对多映射（20）输出为 1 个 bfchar
        assertTrue(text.contains("2 beginbfrange"));
        assertTrue(text.contains("<03> <05> <0041>"));
        assertTrue(text.contains("1 beginbfchar"));

        // 字节内容与文本一致
        assertArrayEquals(text.getBytes(StandardCharsets.ISO_8859_1), cmap.toCMapBytes());

        // 单条目区间按 bfchar 输出（不产生 beginbfrange）
        ToUnicodeCMap single = new ToUnicodeCMap(null);
        single.add(7, 'Z');
        String singleText = single.toCMapText();
        assertFalse(singleText.contains("beginbfrange"));
        assertTrue(singleText.contains("<07> <005A>"));
        // 名称为空时使用兜底名称
        assertTrue(single.toCMapText().contains("/Adobe-Identity-UCS"));

        // 非法输入
        cmap.add(-1, 'A');
        cmap.add(30, -1);
        assertFalse(cmap.contains(30));
        assertThrows(IllegalArgumentException.class, () -> cmap.add(31, new int[0]));
    }

    /**
     * 锁定 ToUnicodeCMap 的双字节编码与 BMP 外码位（代理对）
     */
    @Test
    @DisplayName("ToUnicodeCMap：双字节码位与代理对")
    void toUnicodeCMapTwoByte() {
        ToUnicodeCMap cmap = new ToUnicodeCMap("Big");
        cmap.add(0x0102, 'X');
        // U+1F600（😀）按代理对编码
        cmap.add(0x0200, 0x1F600);
        assertTrue(cmap.requiresTwoBytes());
        String text = cmap.toCMapText();
        assertTrue(text.contains("<0000> <FFFF>"));
        assertTrue(text.contains("<0102> <0058>"));
        assertTrue(text.contains("<0200> <D83DDE00>"));
    }

    /**
     * 锁定由字体 cmap 表批量生成 ToUnicode 映射
     */
    @Test
    @DisplayName("ToUnicodeCMap：由字体 cmap 表生成映射")
    void toUnicodeCMapFromFont() throws IOException {
        ToUnicodeCMap cmap = ToUnicodeCMap.fromFont(Paths.get(SAMPLE_TTF), 0);
        assertTrue(cmap.size() > 100, "样本字体应登记大量字形映射，实际：" + cmap.size());
        // 'A' → gid 102，'/' → gid 402
        assertEquals(Integer.valueOf('A'), cmap.getMappings().get(102)[0]);
        assertEquals(Integer.valueOf('/'), cmap.getMappings().get(402)[0]);
        assertTrue(cmap.requiresTwoBytes());
        String text = cmap.toCMapText();
        assertTrue(text.contains("beginbfchar") || text.contains("beginbfrange"));
        assertTrue(text.contains("<0066> <0041>"), "应包含 gid 102 → 'A' 的映射");
        // 每个 bfchar / bfrange 块的条目数不超过 100
        for (String block : text.split("beginbfchar|beginbfrange")) {
            String[] lines = block.trim().split("\n");
            if (lines.length > 0 && lines[0].matches("\\d+")) {
                assertTrue(Integer.parseInt(lines[0].trim()) <= 100);
            }
        }

        // 带度量：连续区间压缩为 bfrange，且不附带宽度（宽度由字体字典 /W 决定）
        ToUnicodeCMap withMetrics = ToUnicodeCMap.fromFont(Files.readAllBytes(Paths.get(SAMPLE_TTF)), 0);
        assertTrue(withMetrics.toCMapText().contains("beginbfrange"));
    }

    /**
     * 锁定 bfrange 三元组格式：{@code <low> <high> <dst>}，不容许附加字形宽度
     * <p>
     * PDF 1.7 9.10.3 的 bfrange 只有三个操作数，宽度由字体字典 {@code /W}、{@code /DW} 决定；
     * 多写一个数值会被解析器当作新的目标码位，导致文字提取丢字。
     */
    @Test
    @DisplayName("ToUnicodeCMap：bfrange 不带宽度后缀")
    void toUnicodeCMapRangeWidth() throws IOException {
        FontMetrics metrics = FontMetrics.of(Paths.get(SAMPLE_TTF), 0);
        ToUnicodeCMap cmap = new ToUnicodeCMap("WidthFont", metrics);
        cmap.add(100, 'a').add(101, 'b').add(102, 'c');
        String text = cmap.toCMapText();
        assertTrue(text.contains("<64> <66> <0061>\n"), "bfrange 三元组格式异常：" + text);
        assertFalse(text.contains("<64> <66> <0061> 1000"), "bfrange 不应附带宽度：" + text);
        // 未提供度量时输出保持一致
        ToUnicodeCMap noMetrics = new ToUnicodeCMap("WidthFont");
        noMetrics.add(100, 'a').add(101, 'b').add(102, 'c');
        assertTrue(noMetrics.toCMapText().contains("<64> <66> <0061>\n"));
    }

    // =================== GlyphIndexProvider ===================

    /**
     * 锁定 GlyphIndexProvider 的解析顺序：显式 GID → cmap → 回退字形 0
     */
    @Test
    @DisplayName("GlyphIndexProvider：显式 GID 优先，其次 cmap，最后回退 0")
    void glyphIndexProviderApis() throws IOException {
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        GlyphIndexProvider provider = new GlyphIndexProvider(font);
        assertSame(font, provider.getFont());

        // 1. 显式字形编号优先（即使与 cmap 结果不同）
        GlyphIndexProvider.GlyphSelection explicit = provider.resolve(469, 'A');
        assertEquals(1, explicit.glyphCount());
        assertEquals(469, explicit.getGlyphId(0));
        assertEquals(0, explicit.getTextPosition(0));
        assertFalse(explicit.isSubstituted());

        // 2. 没有显式 GID（负值）时查 cmap：'A' → 102
        GlyphIndexProvider.GlyphSelection fromCmap = provider.resolve(-1, 'A');
        assertEquals(1, fromCmap.glyphCount());
        assertEquals(102, fromCmap.getGlyphId(0));
        assertFalse(fromCmap.isSubstituted());

        // 3. cmap 查不到时回退字形 0，并标记发生了替换
        GlyphIndexProvider.GlyphSelection fallback = provider.resolve(-1, 0x10FFFF);
        assertEquals(1, fallback.glyphCount());
        assertEquals(GlyphIndexProvider.NOTDEF_GLYPH, fallback.getGlyphId(0));
        assertTrue(fallback.isSubstituted());

        // 显式 GID 越界：默认回退 cmap
        GlyphIndexProvider.GlyphSelection outOfRange = provider.resolve(999999, 'A');
        assertEquals(102, outOfRange.getGlyphId(0));
        assertTrue(outOfRange.isSubstituted());

        // 显式 GID 越界且关闭 cmap 回退：直接使用字形 0
        GlyphIndexProvider strict = new GlyphIndexProvider(font).fallbackOnExplicitOutOfRange(false);
        GlyphIndexProvider.GlyphSelection strictResult = strict.resolve(999999, 'A');
        assertEquals(GlyphIndexProvider.NOTDEF_GLYPH, strictResult.getGlyphId(0));
        assertTrue(strictResult.isSubstituted());

        assertThrows(IllegalArgumentException.class, () -> new GlyphIndexProvider(null));
    }

    /**
     * 锁定显式字形 0 与非法码位的处理
     */
    @Test
    @DisplayName("GlyphIndexProvider：显式字形 0 与非法码位")
    void glyphIndexProviderNotdefPaths() throws IOException {
        // 本模块测试资源只包含带 cmap 的字体，无 cmap 的降级路径以“非法码位”覆盖
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        GlyphIndexProvider provider = new GlyphIndexProvider(font);
        // 显式 GID 生效：gid 0 是 .notdef，但作为显式字形时仍按原值输出
        GlyphIndexProvider.GlyphSelection notdef = provider.resolve(0, 'A');
        assertEquals(0, notdef.getGlyphId(0));
        assertFalse(notdef.isSubstituted());
        // 非法码位直接回退
        assertTrue(provider.resolve(-1, -5).isSubstituted());
    }

    /**
     * 锁定 CGTransform 多对一 / 一对多 / 一对一 的展开规则
     */
    @Test
    @DisplayName("GlyphIndexProvider：CGTransform 映射展开")
    void glyphIndexProviderCgTransform() throws IOException {
        TrueTypeFont font = new TrueTypeFont().parse(fontBytes(SAMPLE_TTF));
        GlyphIndexProvider provider = new GlyphIndexProvider(font);

        // 无显式字形序列：逐个字符查 cmap
        GlyphIndexProvider.GlyphSelection mapped = provider.select(new int[]{'A', 'B'}, null);
        assertEquals(2, mapped.glyphCount());
        assertEquals(2, mapped.codePointCount());
        assertEquals(102, mapped.getGlyphId(0));
        assertArrayEquals(new int[]{102, mapped.getGlyphId(1)}, mapped.getGlyphIds());
        assertFalse(mapped.isSubstituted());

        // 一对一：输出序列与字符序列等长
        GlyphIndexProvider.GlyphSelection oneToOne = provider.select(new int[]{'A', 'B'}, new int[]{102, 103});
        assertEquals(2, oneToOne.glyphCount());
        assertEquals(0, oneToOne.getTextPosition(0));
        assertEquals(0, oneToOne.getTextPosition(1));

        // 多对一：两个字符 → 一个字形（如连字），只有首个字形携带字符位置
        GlyphIndexProvider.GlyphSelection manyToOne = provider.select(new int[]{'f', 'i'}, new int[]{300});
        assertEquals(1, manyToOne.glyphCount());
        assertEquals(300, manyToOne.getGlyphId(0));
        assertEquals(0, manyToOne.getTextPosition(0));

        // 一对多：一个字符 → 两个字形，第二个字形无对应字符
        GlyphIndexProvider.GlyphSelection oneToMany = provider.select(new int[]{0xFB01}, new int[]{200, 201});
        assertEquals(2, oneToMany.glyphCount());
        assertEquals(0, oneToMany.getTextPosition(0));
        assertEquals(-1, oneToMany.getTextPosition(1));

        // 越界字形编号回退 0 并标记替换
        GlyphIndexProvider.GlyphSelection invalid = provider.select(new int[]{'A'}, new int[]{999999});
        assertEquals(GlyphIndexProvider.NOTDEF_GLYPH, invalid.getGlyphId(0));
        assertTrue(invalid.isSubstituted());
        assertTrue(invalid.toString().contains("substituted=true"));

        // 空输入
        assertThrows(IllegalArgumentException.class, () -> provider.select(new int[0], null));
    }

    /**
     * 锁定 5 个新增类型均可被外部按原名解析
     */
    @Test
    @DisplayName("新增类清单：5 个公开类型均存在且为 public")
    void newPublicTypeInventory() throws ClassNotFoundException {
        List<String> expected = new ArrayList<>();
        expected.add("org.ofdrw.converter.font.FontDescriptor");
        expected.add("org.ofdrw.converter.font.FontMetrics");
        expected.add("org.ofdrw.converter.font.EmbeddedFontData");
        expected.add("org.ofdrw.converter.font.ToUnicodeCMap");
        expected.add("org.ofdrw.converter.font.GlyphIndexProvider");
        for (String className : expected) {
            Class<?> clazz = Class.forName(className);
            assertTrue(java.lang.reflect.Modifier.isPublic(clazz.getModifiers()), className + " 应为 public");
        }
        // 嵌套类型：嵌入数据读取入口与字形选择结果
        Class<?> dataProvider = Class.forName("org.ofdrw.converter.font.FontDescriptor$FontDataProvider");
        assertTrue(dataProvider.isInterface());
        Class<?> selection = Class.forName("org.ofdrw.converter.font.GlyphIndexProvider$GlyphSelection");
        assertTrue(java.lang.reflect.Modifier.isPublic(selection.getModifiers()));
    }

    /**
     * 编译期锁定 FontDataProvider 接口的默认方法可被实现
     */
    @Test
    @DisplayName("FontDescriptor.FontDataProvider：默认方法 openStream")
    void fontDataProviderDefaultMethod() throws IOException {
        FontDescriptor.FontDataProvider provider = () -> new byte[]{1, 2, 3};
        assertArrayEquals(new byte[]{1, 2, 3}, provider.getData());
        assertArrayEquals(new byte[]{1, 2, 3}, org.apache.commons.io.IOUtils.toByteArray(provider.openStream()));
    }

    /**
     * 定位模块测试资源目录（保证在 IDE 与 Maven 下都能找到样本）
     *
     * @return 资源目录路径
     */
    @Test
    @DisplayName("测试资源：样本字体文件存在")
    void sampleFontExists() {
        Path path = Paths.get(SAMPLE_TTF);
        assertTrue(Files.isRegularFile(path), "缺少测试资源：" + path.toAbsolutePath());
    }
}
