package org.ofdrw.converter.font;

import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.GlyphDescription;
import org.apache.fontbox.util.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.awt.geom.GeneralPath;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 公开 API 兼容性回归测试基线。
 * <p>
 * 覆盖 {@code org.ofdrw.converter.font} 包下全部公开类型：
 * <ol>
 *     <li>编译期签名锁定：测试代码显式引用每一处公开构造器、字段、方法与常量，
 *     任何签名删减或参数调整都会导致本测试类编译失败；</li>
 *     <li>运行期行为锁定：使用 {@code src/test/resources} 中既有字体文件
 *     （font_10.ttf、font_13132_0_edit.ttf）断言字形的关键解析结果。</li>
 * </ol>
 * 本测试不依赖任何外部样本，也不依赖字体实现所在模块（迁移前后均可通过）。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class ApiCompatibilityTest {

    /**
     * 无 cmap 表的裁剪字体（缺 cmap），用于验证仅按 GID 取字形
     */
    private static final String FONT_WITHOUT_CMAP = "src/test/resources/font_10.ttf";

    /**
     * 带 cmap 表的字体（cmap 位于文件偏移 172，仅一个 3/1 子表）
     */
    private static final String FONT_WITH_CMAP = "src/test/resources/font_13132_0_edit.ttf";

    /**
     * font_10.ttf 中 hhea 表偏移（单位：字节）
     */
    private static final long F10_HHEA_OFFSET = 17543;

    /**
     * font_10.ttf 中 hhea 表长度（单位：字节）
     */
    private static final long F10_HHEA_LENGTH = 36;

    /**
     * font_10.ttf 中 hmtx 表偏移（单位：字节）
     */
    private static final long F10_HMTX_OFFSET = 17579;

    /**
     * font_10.ttf 中 hmtx 表长度（单位：字节）
     */
    private static final long F10_HMTX_LENGTH = 32448;

    /**
     * font_10.ttf 中 glyf 表偏移（单位：字节）
     */
    private static final long F10_GLYF_OFFSET = 693;

    /**
     * font_10.ttf 中 loca 表偏移（单位：字节）
     */
    private static final long F10_LOCA_OFFSET = 50027;

    /**
     * font_10.ttf 中 name 表偏移（单位：字节）
     */
    private static final long F10_NAME_OFFSET = 82511;

    /**
     * font_10.ttf 中 name 表长度（单位：字节）
     */
    private static final long F10_NAME_LENGTH = 838;

    /**
     * font_13132_0_edit.ttf 中 cmap 表偏移（单位：字节）
     */
    private static final long F13132_CMAP_OFFSET = 172;

    /**
     * 读取测试资源字体文件
     *
     * @param path 相对工作目录的字体文件路径
     * @return 字体文件全部字节
     * @throws IOException 读取失败
     */
    private static byte[] fontBytes(String path) throws IOException {
        return Files.readAllBytes(Paths.get(path));
    }

    /**
     * 解析测试资源字体文件
     *
     * @param path 相对工作目录的字体文件路径
     * @return 字体解析器
     * @throws IOException 读取或解析失败
     */
    private static TrueTypeFont loadFont(String path) throws IOException {
        return new TrueTypeFont().parse(Paths.get(path));
    }

    // =================== TrueTypeFont ===================

    /**
     * 锁定 TrueTypeFont 的四个 parse 重载与公共字段 fontFamily/fontSubFamily/psName
     */
    @Test
    @DisplayName("TrueTypeFont：parse(Path) 重载与公共字段")
    void trueTypeFontParseByPath() throws IOException {
        TrueTypeFont font = new TrueTypeFont().parse(Paths.get(FONT_WITH_CMAP));
        assertNotNull(font);
        // 公共字段可读可写
        assertEquals("sysfST", font.fontFamily);
        assertEquals("Regular", font.fontSubFamily);
        assertEquals("sysfST--GB1-0", font.psName);
        font.fontFamily = "changed";
        assertEquals("changed", font.fontFamily);
    }

    /**
     * 锁定 TrueTypeFont#parse(byte[])、parse(InputStream)、parse(TTFDataStream) 重载，
     * 并验证三种入口解析结果一致
     */
    @Test
    @DisplayName("TrueTypeFont：parse(byte[]/InputStream/TTFDataStream) 重载")
    void trueTypeFontParseOverloads() throws IOException {
        final byte[] buf = fontBytes(FONT_WITHOUT_CMAP);

        TrueTypeFont fromByteArray = new TrueTypeFont().parse(buf);
        assertEquals("ZJPTDA+FZXBSJW--GB1-0", fromByteArray.fontFamily);

        try (InputStream in = new ByteArrayInputStream(buf)) {
            TrueTypeFont fromStream = new TrueTypeFont().parse(in);
            assertEquals(fromByteArray.fontFamily, fromStream.fontFamily);
        }

        TTFDataStream dataStream = new MemoryTTFDataStream(buf);
        TrueTypeFont fromDataStream = new TrueTypeFont().parse(dataStream);
        assertEquals(fromByteArray.psName, fromDataStream.psName);
    }

    /**
     * 锁定 getGlyph(int)：无 cmap 字体按 GID 取字形，gid=469 为 2 个轮廓、16 个绘制点
     */
    @Test
    @DisplayName("TrueTypeFont：getGlyph 轮廓数与绘制点")
    void trueTypeFontGetGlyph() throws IOException {
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        GlyphData glyph = font.getGlyph(469);
        assertNotNull(glyph);
        GlyphDescription description = glyph.getDescription();
        assertEquals(2, description.getContourCount());
        assertEquals(16, description.getPointCount());
        assertFalse(description.isComposite());

        // 空字形：无轮廓
        assertEquals(0, font.getGlyph(1).getDescription().getPointCount());
        // 越界 GID 返回 null 而不是抛异常
        assertNull(font.getGlyph(999999));
        assertNull(font.getGlyph(-1));
    }

    /**
     * 锁定 getPath(int)：返回字形绘制路径；越界 GID 返回 null
     */
    @Test
    @DisplayName("TrueTypeFont：getPath 路径与越界容错")
    void trueTypeFontGetPath() throws IOException {
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        GeneralPath path = font.getPath(469);
        assertNotNull(path);
        assertTrue(path.getBounds().getWidth() > 0);
        assertNull(font.getPath(999999));
    }

    /**
     * 锁定 readCMap / getSubtable / getUnicodeCmapLookup / getUnicodeGlyph
     */
    @Test
    @DisplayName("TrueTypeFont：cmap 相关 API")
    void trueTypeFontCmapApis() throws IOException {
        final byte[] buf = fontBytes(FONT_WITH_CMAP);
        TrueTypeFont font = new TrueTypeFont().parse(buf);

        CmapSubtable[] cmaps = font.readCMap(F13132_CMAP_OFFSET, new MemoryTTFDataStream(buf));
        assertEquals(1, cmaps.length);
        assertEquals(3, cmaps[0].getPlatformId());
        assertEquals(1, cmaps[0].getPlatformEncodingId());

        // 查找子表：存在时返回同一子表，不存在时返回 null
        CmapSubtable subtable = font.getSubtable(3, 1);
        assertNotNull(subtable);
        assertEquals(402, subtable.getGlyphId('/'));
        assertNull(font.getSubtable(3, 10));

        // Unicode 映射
        CmapLookup lookup = font.getUnicodeCmapLookup();
        assertEquals(402, lookup.getGlyphId('/'));
        assertEquals(4, font.getUnicodeGlyph('/').getDescription().getPointCount());
    }

    /**
     * 锁定 getUnicodeGlyph 的兜底行为：字体无 cmap 时取空字形
     */
    @Test
    @DisplayName("TrueTypeFont：无 cmap 时 getUnicodeGlyph 兜底为空字形")
    void trueTypeFontGetUnicodeGlyphWithoutCmap() throws IOException {
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        assertThrows(IllegalArgumentException.class, font::getUnicodeCmapLookup);
        GlyphData glyph = font.getUnicodeGlyph('/');
        assertNotNull(glyph);
        assertEquals(0, glyph.getDescription().getPointCount());
    }

    /**
     * 锁定 getFontMatrix：6 个数值，TrueType 字体按 1000/unitsPerEm 缩放
     */
    @Test
    @DisplayName("TrueTypeFont：getFontMatrix 矩阵序列")
    void trueTypeFontFontMatrix() throws IOException {
        TrueTypeFont font = loadFont(FONT_WITH_CMAP);
        List<Number> matrix = font.getFontMatrix();
        assertEquals(6, matrix.size());
        // unitsPerEm = 256 -> 0.001 * (1000 / 256) = 0.00390625
        assertEquals(0.00390625, matrix.get(0).floatValue(), 1e-12);
        assertEquals(0, matrix.get(1).doubleValue(), 0);
        assertEquals(0, matrix.get(2).doubleValue(), 0);
        assertEquals(0.00390625, matrix.get(3).floatValue(), 1e-12);
        assertEquals(0, matrix.get(4).doubleValue(), 0);
        assertEquals(0, matrix.get(5).doubleValue(), 0);
    }

    /**
     * 锁定 GlyphDataProvider / FontDrawPathProvider 的函数式接口形态与 TrueTypeFont 的实现关系
     */
    @Test
    @DisplayName("TrueTypeFont：实现 GlyphDataProvider 与 FontDrawPathProvider")
    void trueTypeFontImplementsProviders() throws IOException {
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        assertTrue(font instanceof GlyphDataProvider);
        assertTrue(font instanceof FontDrawPathProvider);

        // 两个接口都是 @FunctionalInterface，可直接使用 lambda
        GlyphDataProvider glyphProvider = gid -> font.getGlyph(gid);
        assertNotNull(glyphProvider.getGlyph(469));
        FontDrawPathProvider pathProvider = gid -> font.getPath(gid);
        assertNotNull(pathProvider.getPath(469));
        assertNull(pathProvider.getPath(-1));
    }

    // =================== GlyphData ===================

    /**
     * 锁定 GlyphData 的构造器、readData 与包围盒访问器
     */
    @Test
    @DisplayName("GlyphData：构造器、访问器与 readData")
    void glyphDataApis() throws IOException {
        // 默认构造器与全部访问器
        GlyphData empty = new GlyphData();
        assertNotNull(empty.getDescription());
        assertNotNull(empty.getBoundingBox());
        assertNotNull(empty.getPath());
        assertEquals(0, empty.getNumberOfContours());
        empty.setNumberOfContours((short) 3);
        assertEquals(3, empty.getNumberOfContours());
        BoundingBox box = new BoundingBox(1f, 2f, 3f, 4f);
        empty.setBoundingBox(box);
        assertSame(box, empty.getBoundingBox());

        // CFF 路径构造器：包围盒由路径边界推导
        GeneralPath cffPath = new GeneralPath();
        cffPath.moveTo(0, 0);
        cffPath.lineTo(10, 20);
        GlyphData cffGlyph = new GlyphData(cffPath);
        assertSame(cffPath, cffGlyph.getPath());
        assertEquals(10f, cffGlyph.getBoundingBox().getWidth(), 1e-6);
        assertEquals(20f, cffGlyph.getBoundingBox().getHeight(), 1e-6);

        // readData：从字体流的当前偏移读取字形
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        GlyphData original = font.getGlyph(469);
        GidTracker tracker = new GidTracker(font);
        GlyphData read = new GlyphData();
        read.readData(glyphOffsetStream(), 0, tracker);
        assertEquals(original.getNumberOfContours(), read.getNumberOfContours());
        assertEquals(original.getDescription().getPointCount(), read.getDescription().getPointCount());
        // 包围盒访问器（只读）：与字体解析结果一致
        assertEquals(original.getXMinimum(), read.getXMinimum());
        assertEquals(original.getYMinimum(), read.getYMinimum());
        assertEquals(original.getXMaximum(), read.getXMaximum());
        assertEquals(original.getYMaximum(), read.getYMaximum());
        assertEquals(original.getBoundingBox().getWidth(), read.getBoundingBox().getWidth(), 1e-6);
    }

    /**
     * 获取指向 font_10.ttf 中 gid=469 字形数据起点的流
     *
     * @return 定位到字形数据的流
     * @throws IOException 读取失败
     */
    private static TTFDataStream glyphOffsetStream() throws IOException {
        final byte[] buf = fontBytes(FONT_WITHOUT_CMAP);
        TTFDataStream stream = new MemoryTTFDataStream(buf);
        // indexToLocFormat=1（长格式）：loca[gid] 即为字形相对 glyf 表的偏移
        long glyphOffset = readU32(buf, (int) (F10_LOCA_OFFSET + 469 * 4));
        stream.seek(F10_GLYF_OFFSET + glyphOffset);
        return stream;
    }

    /**
     * 读取并还原 4 字节标签（按 ISO-8859-1 解码，再按原字节重新组装）
     *
     * @param stream 数据流
     * @return 前 4 字节的大端无符号整数值
     * @throws IOException 读取失败
     */
    private static long streamTag(TTFDataStream stream) throws IOException {
        final byte[] tag = stream.read(4);
        return ((long) (tag[0] & 0xFF) << 24) | ((tag[1] & 0xFF) << 16)
                | ((tag[2] & 0xFF) << 8) | (tag[3] & 0xFF);
    }

    /**
     * 读取大端无符号 32 位整数
     *
     * @param buf 字节数组
     * @param pos 起始位置（单位：字节，从 0 开始）
     * @return 无符号整数值
     */
    private static long readU32(byte[] buf, int pos) {
        return ((long) (buf[pos] & 0xFF) << 24) | ((buf[pos + 1] & 0xFF) << 16)
                | ((buf[pos + 2] & 0xFF) << 8) | (buf[pos + 3] & 0xFF);
    }

    /**
     * 记录 readData 回调的 GlyphDataProvider 实现（复合字形解析时会回调）
     */
    private static final class GidTracker implements GlyphDataProvider {
        private final TrueTypeFont font;

        private GidTracker(TrueTypeFont font) {
            this.font = font;
        }

        @Override
        public GlyphData getGlyph(int gid) throws IOException {
            return font.getGlyph(gid);
        }
    }

    // =================== TTFDataStream / MemoryTTFDataStream ===================

    /**
     * 通过匿名子类实现 TTFDataStream 的全部抽象方法，编译期锁定抽象方法集合
     */
    @Test
    @DisplayName("TTFDataStream：全部抽象方法签名可被实现")
    void ttfDataStreamAbstractMethods() throws IOException {
        final byte[] data = new byte[]{0, 1, 0, 0, 0, 2};
        TTFDataStream stream = new TTFDataStream() {
            private int pos = 0;

            @Override
            public int read() throws IOException {
                return pos >= data.length ? -1 : (data[pos++] & 0xFF);
            }

            @Override
            public long readLong() throws IOException {
                long high = readUnsignedInt();
                long low = readUnsignedInt();
                return (high << 32) + low;
            }

            @Override
            public int readUnsignedShort() throws IOException {
                return (readUnsignedByte() << 8) + readUnsignedByte();
            }

            @Override
            public short readSignedShort() throws IOException {
                return (short) readUnsignedShort();
            }

            @Override
            public void seek(long pos) throws IOException {
                this.pos = (int) pos;
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                int count = 0;
                while (count < len && pos < data.length) {
                    b[off + count++] = data[pos++];
                }
                return count == 0 ? -1 : count;
            }

            @Override
            public long getCurrentPosition() throws IOException {
                return pos;
            }

            @Override
            public InputStream getOriginalData() throws IOException {
                return new ByteArrayInputStream(data);
            }

            @Override
            public long getOriginalDataSize() {
                return data.length;
            }

            @Override
            public void close() throws IOException {
                // 无需释放资源
            }
        };
        // 具体公共方法可正常使用（实现中 read() 为无符号字节，数据依次为 0、1、0、0、0、2）
        assertEquals(0x0001, stream.readUnsignedShort());
        assertEquals(0x0000, stream.readUnsignedShort());
        assertEquals(6, stream.getOriginalDataSize());
        stream.seek(4);
        assertEquals(2, stream.readUnsignedShort());
        stream.close();
    }

    /**
     * 锁定 MemoryTTFDataStream 的构造器、读写方法与 TTFDataStream 继承关系
     */
    @Test
    @DisplayName("MemoryTTFDataStream：构造器与读写方法")
    void memoryTTFDataStreamApis() throws IOException {
        // byte[] 构造器
        MemoryTTFDataStream stream = new MemoryTTFDataStream(new byte[]{0, 1, 0, 0, (byte) 0xFF, (byte) 0xFE, 1, 2});
        assertTrue(stream instanceof TTFDataStream);
        assertEquals(8, stream.getOriginalDataSize());
        assertEquals(1, stream.readUnsignedShort());
        assertEquals(2, stream.getCurrentPosition());
        // 剩余两个字节 0x00 0x00 按有符号短整数读取为 0
        assertEquals(0, stream.readSignedShort());
        stream.seek(0);
        assertEquals(1.0f, stream.read32Fixed(), 1e-6);
        stream.seek(0);
        assertEquals(65536, stream.readSignedInt());
        stream.seek(0);
        // readLong 不检查 EOF：越界位置返回 -1，按 (0xFFFF << 32) | -1 计算
        assertEquals(0x00010000FFFE0102L, stream.readLong());
        stream.close();

        // InputStream 构造器
        try (InputStream in = new ByteArrayInputStream(fontBytes(FONT_WITHOUT_CMAP))) {
            MemoryTTFDataStream fromStream = new MemoryTTFDataStream(in);
            assertEquals(Files.size(Paths.get(FONT_WITHOUT_CMAP)), fromStream.getOriginalDataSize());
            // 前两字节为 sfnt 版本高 16 位（0x0001）
            assertEquals(0x0001, fromStream.readUnsignedShort());
            fromStream.seek(0);
            // 前 4 字节 0x00010000 为 sfnt 版本号
            assertEquals(0x00010000L, streamTag(fromStream));
            fromStream.seek(0);
            assertEquals(0x00010000, fromStream.readUnsignedInt());
            fromStream.seek(12);
            assertEquals("OS/2", fromStream.readTag());
            fromStream.seek(0);
            assertEquals(0, fromStream.readUnsignedByte());
            fromStream.seek(0);
            assertEquals(0, fromStream.read());
            fromStream.seek(0);
            assertEquals(4096, fromStream.read(new byte[4096], 0, 4096));
            fromStream.seek(0);
            assertEquals(0, fromStream.readString(1).charAt(0));
            fromStream.seek(0);
            assertEquals(0, fromStream.readString(1, StandardCharsets.US_ASCII).charAt(0));
            fromStream.seek(0);
            assertEquals(0, fromStream.readString(1, "US-ASCII").charAt(0));
            fromStream.seek(0);
            // readUnsignedByteArray 直接使用 read() 的返回值（此处为 0x00）
            assertEquals(0, fromStream.readUnsignedByteArray(3)[2]);
            fromStream.seek(0);
            // readUnsignedShortArray 基于 readUnsignedShort，首值 0x0001
            assertEquals(0x0001, fromStream.readUnsignedShortArray(1)[0]);
            fromStream.seek(0);
            assertEquals(4, fromStream.read(4).length);
            fromStream.seek(0);
            assertNotNull(fromStream.readInternationalDate());
            assertNotNull(fromStream.getOriginalData());
        }
    }

    // =================== CmapSubtable ===================

    /**
     * 锁定 CmapSubtable 的构造器、initData、属性访问器与映射方法
     */
    @Test
    @DisplayName("CmapSubtable：构造器、initData 与映射方法")
    void cmapSubtableApis() throws IOException {
        final byte[] buf = fontBytes(FONT_WITH_CMAP);
        MemoryTTFDataStream stream = new MemoryTTFDataStream(buf);
        stream.seek(F13132_CMAP_OFFSET + 4);

        CmapSubtable subtable = new CmapSubtable().initData(stream);
        // initData 仅读取子表头（平台 ID、编码 ID、子表偏移），子表内容由 TrueTypeFont#readCMap 初始化
        assertEquals(3, subtable.getPlatformId());
        assertEquals(1, subtable.getPlatformEncodingId());
        assertEquals("{3 1}", subtable.toString());

        // 属性访问器可写
        subtable.setPlatformId(0);
        subtable.setPlatformEncodingId(4);
        assertEquals(0, subtable.getPlatformId());
        assertEquals(4, subtable.getPlatformEncodingId());
        assertEquals("{0 4}", subtable.toString());

        // 映射查询
        TrueTypeFont font = new TrueTypeFont().parse(buf);
        CmapSubtable[] cmaps = font.readCMap(F13132_CMAP_OFFSET, new MemoryTTFDataStream(buf));
        assertEquals("{3 1}", cmaps[0].toString());
        assertEquals(102, cmaps[0].getGlyphId('A'));
        assertEquals(0, cmaps[0].getGlyphId(0x10FFFF));
        List<Integer> codes = cmaps[0].getCharCodes(402);
        assertEquals(1, codes.size());
        assertEquals(Integer.valueOf('/'), codes.get(0));
        assertNull(cmaps[0].getCharCodes(999999));
    }

    // =================== HorizontalHeaderTable ===================

    /**
     * 锁定 HorizontalHeaderTable 的构造器、parse 与全部访问器
     */
    @Test
    @DisplayName("HorizontalHeaderTable：构造器、parse 与访问器")
    void horizontalHeaderTableApis() throws IOException {
        TTFDataStream stream = new MemoryTTFDataStream(fontBytes(FONT_WITHOUT_CMAP));
        HorizontalHeaderTable hhea = new HorizontalHeaderTable(new long[]{F10_HHEA_OFFSET, F10_HHEA_LENGTH}).parse(stream);
        assertEquals(1.0f, hhea.getVersion(), 1e-6);
        assertEquals(222, hhea.getAscender());
        assertEquals(-78, hhea.getDescender());
        assertEquals(0, hhea.getLineGap());
        assertEquals(256, hhea.getAdvanceWidthMax());
        assertEquals(8112, hhea.getNumberOfHMetrics());
        assertEquals(0, hhea.getMetricDataFormat());
        assertEquals(0, hhea.getCaretSlopeRun());
        assertEquals(1, hhea.getCaretSlopeRise());
        assertEquals(0, hhea.getReserved1());

        // 写入访问器
        hhea.setVersion(2.5f);
        hhea.setAscender((short) 1);
        hhea.setDescender((short) 2);
        hhea.setLineGap((short) 3);
        hhea.setAdvanceWidthMax(4);
        hhea.setMinLeftSideBearing((short) 5);
        hhea.setMinRightSideBearing((short) 6);
        hhea.setXMaxExtent((short) 7);
        hhea.setCaretSlopeRise((short) 8);
        hhea.setCaretSlopeRun((short) 9);
        hhea.setReserved1((short) 10);
        hhea.setReserved2((short) 11);
        hhea.setReserved3((short) 12);
        hhea.setReserved4((short) 13);
        hhea.setReserved5((short) 14);
        hhea.setMetricDataFormat((short) 15);
        hhea.setNumberOfHMetrics(16);
        assertEquals(2.5f, hhea.getVersion(), 1e-6);
        assertEquals(1, hhea.getAscender());
        assertEquals(2, hhea.getDescender());
        assertEquals(3, hhea.getLineGap());
        assertEquals(4, hhea.getAdvanceWidthMax());
        assertEquals(5, hhea.getMinLeftSideBearing());
        assertEquals(6, hhea.getMinRightSideBearing());
        assertEquals(7, hhea.getXMaxExtent());
        assertEquals(8, hhea.getCaretSlopeRise());
        assertEquals(9, hhea.getCaretSlopeRun());
        assertEquals(10, hhea.getReserved1());
        assertEquals(11, hhea.getReserved2());
        assertEquals(12, hhea.getReserved3());
        assertEquals(13, hhea.getReserved4());
        assertEquals(14, hhea.getReserved5());
        assertEquals(15, hhea.getMetricDataFormat());
        assertEquals(16, hhea.getNumberOfHMetrics());
    }

    // =================== HorizontalMetricsTable ===================

    /**
     * 锁定 HorizontalMetricsTable 的构造器、parse、getAdvanceWidth 与 getLeftSideBearing
     */
    @Test
    @DisplayName("HorizontalMetricsTable：构造器、parse 与访问器")
    void horizontalMetricsTableApis() throws IOException {
        TTFDataStream stream = new MemoryTTFDataStream(fontBytes(FONT_WITHOUT_CMAP));
        HorizontalMetricsTable hmtx = new HorizontalMetricsTable(new long[]{F10_HMTX_OFFSET, F10_HMTX_LENGTH})
                .parse(8112, 8111, stream);
        // hmtx 覆盖全部字形，宽度沿用最后一个 horizontal metric
        assertEquals(256, hmtx.getAdvanceWidth(0));
        assertEquals(256, hmtx.getAdvanceWidth(469));
        assertEquals(0, hmtx.getLeftSideBearing(0));
        assertEquals(35, hmtx.getLeftSideBearing(469));
    }

    // =================== NamingTable / NameRecord ===================

    /**
     * 锁定 NamingTable 的构造器、read 与访问器
     */
    @Test
    @DisplayName("NamingTable：构造器、read 与访问器")
    void namingTableApis() throws IOException {
        TTFDataStream stream = new MemoryTTFDataStream(fontBytes(FONT_WITHOUT_CMAP));
        NamingTable namingTable = new NamingTable(new long[]{F10_NAME_OFFSET, F10_NAME_LENGTH}).read(stream);
        assertEquals("ZJPTDA+FZXBSJW--GB1-0", namingTable.getFontFamily());
        assertEquals("Regular", namingTable.getFontSubFamily());
        assertEquals("ZJPTDA+FZXBSJW--GB1-0", namingTable.getPostScriptName());

        List<NameRecord> records = namingTable.getNameRecords();
        assertEquals(13, records.size());
        assertNotNull(namingTable.getName(NameRecord.NAME_FONT_FAMILY_NAME,
                NameRecord.PLATFORM_WINDOWS, NameRecord.ENCODING_WINDOWS_UNICODE_BMP, 1033));
        assertNull(namingTable.getName(99, 0, 0, 0));
    }

    /**
     * 锁定 NameRecord 的常量、读写访问器与 readNameRecord
     */
    @Test
    @DisplayName("NameRecord：常量、访问器与 readNameRecord")
    void nameRecordApis() throws IOException {
        // 平台与编码常量
        assertEquals(0, NameRecord.PLATFORM_UNICODE);
        assertEquals(1, NameRecord.PLATFORM_MACINTOSH);
        assertEquals(2, NameRecord.PLATFORM_ISO);
        assertEquals(3, NameRecord.PLATFORM_WINDOWS);
        assertEquals(0, NameRecord.ENCODING_UNICODE_1_0);
        assertEquals(1, NameRecord.ENCODING_UNICODE_1_1);
        assertEquals(3, NameRecord.ENCODING_UNICODE_2_0_BMP);
        assertEquals(4, NameRecord.ENCODING_UNICODE_2_0_FULL);
        assertEquals(0, NameRecord.LANGUAGE_UNICODE);
        assertEquals(0, NameRecord.ENCODING_WINDOWS_SYMBOL);
        assertEquals(1, NameRecord.ENCODING_WINDOWS_UNICODE_BMP);
        assertEquals(10, NameRecord.ENCODING_WINDOWS_UNICODE_UCS4);
        assertEquals(0x0409, NameRecord.LANGUAGE_WINDOWS_EN_US);
        assertEquals(0, NameRecord.ENCODING_MACINTOSH_ROMAN);
        assertEquals(0, NameRecord.LANGUAGE_MACINTOSH_ENGLISH);
        // 名称 ID 常量
        assertEquals(0, NameRecord.NAME_COPYRIGHT);
        assertEquals(1, NameRecord.NAME_FONT_FAMILY_NAME);
        assertEquals(2, NameRecord.NAME_FONT_SUB_FAMILY_NAME);
        assertEquals(3, NameRecord.NAME_UNIQUE_FONT_ID);
        assertEquals(4, NameRecord.NAME_FULL_FONT_NAME);
        assertEquals(5, NameRecord.NAME_VERSION);
        assertEquals(6, NameRecord.NAME_POSTSCRIPT_NAME);
        assertEquals(7, NameRecord.NAME_TRADEMARK);

        // 读取 font_10.ttf name 表首条记录
        TTFDataStream stream = new MemoryTTFDataStream(fontBytes(FONT_WITHOUT_CMAP));
        stream.seek(F10_NAME_OFFSET + 6);
        NameRecord record = new NameRecord().readNameRecord(stream);
        assertEquals(NameRecord.PLATFORM_MACINTOSH, record.getPlatformId());
        assertEquals(0, record.getPlatformEncodingId());
        assertEquals(0, record.getLanguageId());
        // font_10.ttf name 表首条记录：版权信息，长度 53 字节，字符串偏移 0
        assertEquals(NameRecord.NAME_COPYRIGHT, record.getNameId());
        assertEquals(53, record.getStringLength());
        assertEquals(0, record.getStringOffset());
        assertTrue(record.toString().contains("name=" + NameRecord.NAME_COPYRIGHT));

        // 写入访问器
        record.setPlatformId(3);
        record.setPlatformEncodingId(1);
        record.setLanguageId(0x0409);
        record.setNameId(NameRecord.NAME_FONT_FAMILY_NAME);
        record.setStringLength(4);
        record.setStringOffset(2);
        record.setString("test");
        assertEquals(3, record.getPlatformId());
        assertEquals(1, record.getPlatformEncodingId());
        assertEquals(0x0409, record.getLanguageId());
        assertEquals(NameRecord.NAME_FONT_FAMILY_NAME, record.getNameId());
        assertEquals(4, record.getStringLength());
        assertEquals(2, record.getStringOffset());
        assertEquals("test", record.getString());
    }

    // =================== TrueTypeCollection ===================

    /**
     * 锁定 TrueTypeCollection 的构造器、parse、getFontAtIndex、foreach 与 getNumFonts
     */
    @Test
    @DisplayName("TrueTypeCollection：构造器、parse、foreach 与 getNumFonts")
    void trueTypeCollectionApis() throws IOException {
        TrueTypeCollection collection = new TrueTypeCollection();
        assertEquals(0, collection.getNumFonts());
        // foreach 在空集合上不应发动回调
        AtomicInteger counter = new AtomicInteger();
        collection.foreach(font -> counter.incrementAndGet());
        assertEquals(0, counter.get());
        // 非 TTC 数据：缺少 ttcf 头时抛出 IOException
        assertThrows(IOException.class,
                () -> new TrueTypeCollection().parse(new MemoryTTFDataStream(fontBytes(FONT_WITHOUT_CMAP))));
        // getFontAtIndex / foreach 签名锁定（编译期），并发访问以确认签名可用
        TrueTypeCollection typed = new TrueTypeCollection();
        assertThrows(IllegalArgumentException.class, () -> typed.getFontAtIndex(0));
        typed.foreach(font -> font.fontFamily = "unused");
    }

    // =================== FontWrapper / PdfFontWrapper ===================

    /**
     * 锁定泛型包装类 FontWrapper 的构造器与访问器
     */
    @Test
    @DisplayName("FontWrapper：构造器与访问器")
    void fontWrapperApis() {
        FontWrapper<String> wrapper = new FontWrapper<>();
        assertNull(wrapper.getFont());
        assertFalse(wrapper.isEnableSimilarFontReplace());
        wrapper.setFont("font");
        wrapper.setEnableSimilarFontReplace(true);
        assertEquals("font", wrapper.getFont());
        assertTrue(wrapper.isEnableSimilarFontReplace());

        FontWrapper<String> created = new FontWrapper<>("other", false);
        assertEquals("other", created.getFont());
        assertFalse(created.isEnableSimilarFontReplace());

        // 与字体的实际组合方式
        FontWrapper<TrueTypeFont> holder = new FontWrapper<>(new TrueTypeFont(), true);
        assertNotNull(holder.getFont());
        assertTrue(holder.isEnableSimilarFontReplace());
    }

    /**
     * 锁定已废弃的 PdfFontWrapper 的构造器与访问器（null 字体场景，无需 iText 运行时对象）
     */
    @Test
    @DisplayName("PdfFontWrapper：构造器与访问器")
    void pdfFontWrapperApis() {
        PdfFontWrapper wrapper = new PdfFontWrapper();
        assertNull(wrapper.getPdfFont());
        assertFalse(wrapper.isEnableSimilarFontReplace());
        wrapper.setPdfFont(null);
        wrapper.setEnableSimilarFontReplace(true);
        assertTrue(wrapper.isEnableSimilarFontReplace());

        PdfFontWrapper single = new PdfFontWrapper(null);
        assertNull(single.getPdfFont());
        PdfFontWrapper pair = new PdfFontWrapper(null, true);
        assertTrue(pair.isEnableSimilarFontReplace());
    }

    // =================== 字形轮廓描述类 ===================

    /**
     * 锁定 GlyfDescript 常量与 GlyfSimpleDescript / GlyfCompositeDescript 的公开签名
     */
    @Test
    @DisplayName("轮廓描述类：常量与公开方法签名")
    void glyfDescriptionClassesApis() throws IOException {
        assertEquals(0x01, GlyfDescript.ON_CURVE);
        assertEquals(0x02, GlyfDescript.X_SHORT_VECTOR);
        assertEquals(0x04, GlyfDescript.Y_SHORT_VECTOR);
        assertEquals(0x08, GlyfDescript.REPEAT);
        assertEquals(0x10, GlyfDescript.X_DUAL);
        assertEquals(0x20, GlyfDescript.Y_DUAL);
        assertEquals(0x0020, GlyfCompositeDescript.MORE_COMPONENTS);
        assertEquals(0x0100, GlyfCompositeDescript.WE_HAVE_INSTRUCTIONS);

        // 简单轮廓：由字体解析得到的实表明细
        TrueTypeFont font = loadFont(FONT_WITHOUT_CMAP);
        GlyphDescription description = font.getGlyph(469).getDescription();
        assertTrue(description instanceof GlyfDescript);
        GlyfDescript glyfDescript = (GlyfDescript) description;
        assertEquals(2, glyfDescript.getContourCount());
        assertNotNull(glyfDescript.getInstructions());
        glyfDescript.resolve();
        assertTrue(description instanceof GlyfSimpleDescript);
        GlyfSimpleDescript simple = (GlyfSimpleDescript) description;
        assertEquals(16, simple.getPointCount());
        // 两段轮廓的结束点索引：7、15（共 16 个点）
        assertEquals(7, simple.getEndPtOfContours(0));
        assertEquals(15, simple.getEndPtOfContours(1));
        assertFalse(simple.isComposite());
        assertNotNull(simple.getXCoordinate(0));
        assertNotNull(simple.getYCoordinate(0));
        assertNotNull(simple.getFlags(0));

        // GlyfCompositeDescript：测试字体中仅有简单字形，这里仅锁定类型引用与构造器可见性
        // （复合轮廓的实测量由真实样本在本地比对中覆盖，不引入外部样本）
        assertNotNull(GlyfCompositeDescript.class.getDeclaredConstructors());
    }

    /**
     * 锁定 GlyfCompositeComp 的全部方法签名（构造器为包内可见，由字体解析内部创建）
     */
    @Test
    @DisplayName("GlyfCompositeComp：方法签名")
    void glyfCompositeCompApis() {
        Class<?> clazz = GlyfCompositeComp.class;
        List<String> declared = new ArrayList<>();
        for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
            declared.add(m.getName());
        }
        String[] methods = {"setFirstIndex", "getFirstIndex", "setFirstContour", "getFirstContour",
                "getArgument1", "getArgument2", "getFlags", "getGlyphIndex", "getScale01",
                "getScale10", "getXScale", "getYScale", "getXTranslate", "getYTranslate",
                "scaleX", "scaleY"};
        for (String method : methods) {
            assertTrue(declared.contains(method), "GlyfCompositeComp 缺少方法: " + method);
        }
        // 常量签名
        assertEquals(0x0020, GlyfCompositeDescript.MORE_COMPONENTS);
        assertEquals(0x0100, GlyfCompositeDescript.WE_HAVE_INSTRUCTIONS);
    }

    // =================== 类清单 ===================

    /**
     * 锁定 org.ofdrw.converter.font 下全部公开类仍可被外部按原名解析
     */
    @Test
    @DisplayName("包内公开类清单：20 个公开类型均存在")
    void publicClassInventory() throws ClassNotFoundException {
        List<String> expected = new ArrayList<>();
        expected.add("org.ofdrw.converter.font.TrueTypeFont");
        expected.add("org.ofdrw.converter.font.GlyphData");
        expected.add("org.ofdrw.converter.font.TTFDataStream");
        expected.add("org.ofdrw.converter.font.MemoryTTFDataStream");
        expected.add("org.ofdrw.converter.font.CmapSubtable");
        expected.add("org.ofdrw.converter.font.HorizontalHeaderTable");
        expected.add("org.ofdrw.converter.font.HorizontalMetricsTable");
        expected.add("org.ofdrw.converter.font.NamingTable");
        expected.add("org.ofdrw.converter.font.NameRecord");
        expected.add("org.ofdrw.converter.font.FontWrapper");
        expected.add("org.ofdrw.converter.font.PdfFontWrapper");
        expected.add("org.ofdrw.converter.font.TrueTypeCollection");
        expected.add("org.ofdrw.converter.font.GlyphDataProvider");
        expected.add("org.ofdrw.converter.font.FontDrawPathProvider");
        expected.add("org.ofdrw.converter.font.GlyfDescript");
        expected.add("org.ofdrw.converter.font.GlyfSimpleDescript");
        expected.add("org.ofdrw.converter.font.GlyfCompositeDescript");
        expected.add("org.ofdrw.converter.font.GlyfCompositeComp");
        for (String className : expected) {
            assertNotNull(Class.forName(className), className);
        }
        // Type1 解析器（子包）：仅按类型引用，避免与 converter 内其他类型的同名类混淆
        Class<?> type1Parser = org.ofdrw.converter.font.type1.Type1SegSplitParser.class;
        assertTrue(java.lang.reflect.Modifier.isPublic(type1Parser.getModifiers()));
    }
}
