package org.ofdrw.converter.font;


import org.apache.commons.io.IOUtils;
import org.apache.fontbox.cff.CFFFont;
import org.apache.fontbox.cff.CFFParser;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.CmapTable;
import org.apache.fontbox.type1.Type1Font;
import org.ofdrw.converter.font.type1.Type1SegSplitParser;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * TrueType 字体解析器
 * <p>
 * https://docs.microsoft.com/zh-cn/typography/opentype/spec/otff
 * <p>
 * CMAP见
 * <p>
 * https://docs.microsoft.com/zh-cn/typography/opentype/spec/cmap
 *
 * @author 权观宇
 * @since 2021-09-28 21:05:09
 */
public class TrueTypeFont implements GlyphDataProvider,FontDrawPathProvider {

    /**
     * 字形数量
     */
    private int numGlyphs;

    /**
     * 字形偏移量数组
     */
    private long[] glyphOffsets;

    /**
     * 字形数据对象
     */
    private GlyphData[] glyphs;

    /**
     * 字体随机访问对象
     */
    private TTFDataStream data;

    /**
     * 字符表偏移量
     */
    private long[] glyOffset;

    /**
     * 字符编码到字形映射表
     * <p>
     * 不同字符集使用独立子表表述，数组中的每一个表都是一种类型字符集
     */
    private CmapSubtable[] cmaps;

    /**
     * 字体的单位数，从header中获取
     * <p>
     * 默认0
     */
    private float unitsPerEm;


    /**
     * 字体名称来资源有 name表
     */
    public String fontFamily = null;
    // 字形样式，如 Regular - 常规；Bold - 加粗； Italic - 斜体
    public String fontSubFamily = null;
    public String psName = null;
    /**
     * 用于分析字体宽度的表
     */
    private HorizontalMetricsTable hmt;

    /**
     * Adobe Compact Font Format, 压缩字体格式
     * 在无法使用TTF表格式解析时，尝试采用CFF格式解析。
     */
    private CFFFont cffFont;

    /**
     * Adobe Type1 字体
     * 开头为 0x80 0x01 或 '%!' 的字体
     */
    private Type1Font type1Font;

    public TrueTypeFont() {
    }



    /**
     * 创建TTF字体解析器
     *
     * @param in 流
     * @return this
     * @throws IOException IOE
     */
    public TrueTypeFont parse(InputStream in) throws IOException {
        final byte[] buf = IOUtils.toByteArray(in);
        TTFDataStream dataStream = new MemoryTTFDataStream(buf);
        return new TrueTypeFont().parse(dataStream);
    }

    /**
     * 创建TTF字体解析器
     *
     * @param buf 读取到内存的字体数据
     * @return this
     * @throws IOException IOE
     */
    public TrueTypeFont parse(byte[] buf) throws IOException {
        TTFDataStream dataStream = new MemoryTTFDataStream(buf);
        return new TrueTypeFont().parse(dataStream);
    }

    /**
     * 创建TTF字体解析器
     *
     * @param inPath 字体文件路径
     * @return this
     * @throws IOException IOE
     */
    public TrueTypeFont parse(Path inPath) throws IOException {
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(inPath));
        return new TrueTypeFont().parse(dataStream);
    }

    /**
     * 解析字体
     *
     * @param raf 随机读取字体
     * @return this
     * @throws IOException IOE
     */
    public TrueTypeFont parse(TTFDataStream raf) throws IOException {
        this.data = raf;
        // Version
        final byte[] version = raf.read(4);
        // 检查是否是Type1 字体
        if (Type1SegSplitParser.isType1(version)) {
            this.type1Font = Type1SegSplitParser.parse(raf.getOriginalData());
            return this;
        }


        int numberOfTables = raf.readUnsignedShort();
        int searchRange = raf.readUnsignedShort();
        int entrySelector = raf.readUnsignedShort();
        int rangeShift = raf.readUnsignedShort();
        Map<String, long[]> tables = new HashMap<>(numberOfTables);
        for (int i = 0; i < numberOfTables; i++) {
            String tag = raf.readString(4);
            long checkSum = raf.readUnsignedInt();   // 4 byte
            long offset = raf.readUnsignedInt();     // 4 byte
            long length = raf.readUnsignedInt();     // 4 byte
            tables.put(tag, new long[]{offset, length});
        }

        /*
            head  头数据
            maxp  字形数量
            loca  配置参数
            glyf  字形数据
         */
        // =========> head
        if (!tables.containsKey("head")) {
            InputStream originalData = raf.getOriginalData();
            // head 表都不存在的情况，尝试使用CFF格式解析字体
            List<CFFFont> fonts = new CFFParser().parse(IOUtils.toByteArray(originalData));
            if (fonts != null && !fonts.isEmpty()) {
                this.cffFont = fonts.get(0);
                return this;
            } else {
                throw new IllegalArgumentException("没有 head 表");
            }
        }

        raf.seek(tables.get("head")[0] + 18);
        unitsPerEm = data.readUnsignedShort();
        raf.seek(tables.get("head")[0] + 50);
        /*
         * 偏移量计算方式
         *  1: offset = readUnit16() * 2
         *  0: offset = readUnit32()
         */
        short indexToLocFormat = raf.readSignedShort();
        // =========> maxp
        if (!tables.containsKey("maxp")) {
            throw new IllegalArgumentException("没有 maxp 表");
        }
        raf.seek(tables.get("maxp")[0] + 4);
        numGlyphs = raf.readUnsignedShort();
        // =========> loca
        if (!tables.containsKey("loca")) {
            throw new IllegalArgumentException("没有 loca 表");
        }
        raf.seek(tables.get("loca")[0]);
        glyphOffsets = new long[numGlyphs + 1];
        for (int i = 0; i < numGlyphs + 1; i++) {
            if (indexToLocFormat == 0) {
                glyphOffsets[i] = raf.readUnsignedShort() * 2L;
            } else {
                glyphOffsets[i] = raf.readUnsignedInt();
            }
        }
        // =========> glyf
        if (!tables.containsKey("glyf")) {
            throw new IllegalArgumentException("没有 glyf 表");
        }
        glyphs = new GlyphData[numGlyphs + 1];
        glyOffset = tables.get("glyf");
        // =========> cmap
        if (tables.containsKey("cmap")) {
            // 读取cmap
            this.cmaps = readCMap(tables.get("cmap")[0], raf);
        }
        // =========> name
        if (tables.containsKey("name")) {
            NamingTable nt = new NamingTable(tables.get("name")).read(raf);
            this.fontFamily = nt.getFontFamily();
            this.fontSubFamily = nt.getFontSubFamily();
            this.psName = nt.getPostScriptName();
            // GC
            nt = null;
        }
        // =========> hhea || hmtx 字体宽度
        if (tables.containsKey("hhea") && tables.containsKey("hmtx")) {
            final long[] hhOff = tables.get("hhea");
            final long[] hmOff = tables.get("hmtx");
            int numberOfHMetrics = new HorizontalHeaderTable(hhOff).parse(data).getNumberOfHMetrics();
            hmt = new HorizontalMetricsTable(hmOff).parse(numberOfHMetrics, numGlyphs, data);
        }

        return this;
    }

    /**
     * 通过字形的Index获取字形数据
     *
     * @param gid 字形Index
     * @throws IOException IOE
     */
    @Override
    public GlyphData getGlyph(int gid) throws IOException {
        if (gid < 0 || gid >= numGlyphs) {
            return null;
        }

        if (cffFont != null) {
            // CFF 字体不含字形信息，只有路径，因此直接返回null
            return null;
        }


        if (glyphs != null && glyphs[gid] != null) {
            return glyphs[gid];
        }

        GlyphData glyph;

        // PDFBOX-4219: synchronize on data because it is accessed by several threads
        // when PDFBox is accessing a standard 14 font for the first time
        synchronized (data) {
            // read a single glyph

            if (glyphOffsets[gid] == glyphOffsets[gid + 1]) {
                // no outline
                // PDFBOX-5135: can't return null, must return an empty glyph because
                // sometimes this is used in a composite glyph.
                glyph = new GlyphData();
            } else {
                // save
                long currentPosition = data.getCurrentPosition();

                data.seek(glyOffset[0] + glyphOffsets[gid]);
                // 解析字形
                glyph = getGlyphData(data, gid);

                // restore
                data.seek(currentPosition);
            }

            if (glyphs != null && glyphs[gid] == null) {
                glyphs[gid] = glyph;
            }

            return glyph;
        }
    }

    /**
     * 通过字体索引号获取字形绘制路径
     *
     * @param gid 字形索引号
     * @return 字形路径或null
     * @throws IOException 字体解析异常
     */
    @Override
    public GeneralPath getPath(int gid) throws IOException {
        // 存在CFF的时候采用CFF直接获取字形
        if (this.cffFont != null) {
            return this.cffFont.getType2CharString(gid).getPath();
        }
        return getGlyph(gid).getPath();
    }


    /**
     * 从但前偏移量位置读取字形
     *
     * @param raf 字体数据
     * @param gid 字体ID
     * @return 字形数据
     * @throws IOException IOE
     */
    private GlyphData getGlyphData(TTFDataStream raf, int gid) throws IOException {
        GlyphData glyph = new GlyphData();
        int leftSideBearing = hmt == null ? 0 : hmt.getLeftSideBearing(gid);
        glyph.readData(raf, leftSideBearing, this);
        // resolve composite glyph
        if (glyph.getDescription().isComposite()) {
            glyph.getDescription().resolve();
        }
        return glyph;
    }

    /**
     * 解析cmap
     *
     * @param cmapOffset cmap开始偏移量
     * @param data       随机访问流
     * @return cmap子表数组
     * @throws IOException IOE
     */
    public CmapSubtable[] readCMap(long cmapOffset, TTFDataStream data) throws IOException {
        // 定位到cmap
        data.seek(cmapOffset);
        int version = data.readUnsignedShort();
        int numberOfTables = data.readUnsignedShort();
        CmapSubtable[] cmaps = new CmapSubtable[numberOfTables];
        // 依次子表信息数组
        for (int i = 0; i < numberOfTables; i++) {
            cmaps[i] = new CmapSubtable().initData(data);
        }
        for (int i = 0; i < numberOfTables; i++) {
            // 依次解析子表
            cmaps[i].initSubtable(cmapOffset, this.numGlyphs, data);
        }
        return cmaps;
    }

    /**
     * 获取特定平台特定编码的字符映射子表
     * <p>
     * Returns the subtable, if any, for the given platform and encoding.
     *
     * @param platformId         平台ID
     * @param platformEncodingId 平台编码ID
     * @return 映射子表
     */
    public CmapSubtable getSubtable(int platformId, int platformEncodingId) {
        if (cmaps == null || cmaps.length == 0) {
            return null;
        }
        for (CmapSubtable cmap : cmaps) {
            if (cmap.getPlatformId() == platformId &&
                    cmap.getPlatformEncodingId() == platformEncodingId) {
                return cmap;
            }
        }
        return null;
    }

    /**
     * 获取Unicode到字形的映射表
     *
     * @return 映射表
     * @throws IllegalArgumentException 字体没有cmap表
     */
    public CmapLookup getUnicodeCmapLookup() {
        if (cmaps == null || cmaps.length == 0) {
            throw new IllegalArgumentException("字体中没有cmap");
        }
        CmapSubtable cmap = getSubtable(CmapTable.PLATFORM_UNICODE, CmapTable.ENCODING_UNICODE_2_0_FULL);
        if (cmap == null) {
            cmap = getSubtable(CmapTable.PLATFORM_WINDOWS, CmapTable.ENCODING_WIN_UNICODE_FULL);
        }
        if (cmap == null) {
            cmap = getSubtable(CmapTable.PLATFORM_UNICODE, CmapTable.ENCODING_UNICODE_2_0_BMP);
        }
        if (cmap == null) {
            cmap = getSubtable(CmapTable.PLATFORM_WINDOWS, CmapTable.ENCODING_WIN_UNICODE_BMP);
        }
        if (cmap == null) {
            // Microsoft's "Recommendations for OpenType Fonts" says that "Symbol" encoding
            // actually means "Unicode, non-standard character set"
            cmap = getSubtable(CmapTable.PLATFORM_WINDOWS, CmapTable.ENCODING_WIN_SYMBOL);
        }
        if (cmap == null) {
            // fallback to the first cmap (may not be Unicode, so may produce poor results)
            cmap = cmaps[0];
        }
        return cmap;
    }

    /**
     * 通过Unicode获取字形数据
     * <p>
     * 如果没有cmap那么返回空白字符
     *
     * @param code unicode
     * @return 字符数据
     * @throws IOException 字体文件解析异常
     */
    public GlyphData getUnicodeGlyph(int code) throws IOException {
        int gid;
        if (cmaps == null || cmaps.length == 0) {
            // 没有cmap的情况直接返回第一个字符也就是空白字符
            gid = 0;
        } else {
            gid = getUnicodeCmapLookup().getGlyphId(code);
        }
        return getGlyph(gid);
    }


    /**
     * 获取变换矩阵
     *
     * @return 矩阵序列
     */
    public List<Number> getFontMatrix() {
        if (cffFont !=null){
            return cffFont.getFontMatrix();
        }
        float scale = 1000f / unitsPerEm;
        return Arrays.<Number>asList(0.001f * scale, 0, 0, 0.001f * scale, 0, 0);
    }

}
