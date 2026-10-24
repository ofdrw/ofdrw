package org.ofdrw.converter.font;


import org.apache.commons.io.IOUtils;
import org.apache.fontbox.cff.CFFFont;
import org.apache.fontbox.cff.CFFParser;
import org.apache.fontbox.cff.Type2CharString;
import org.apache.fontbox.ttf.CmapLookup;
import org.apache.fontbox.ttf.CmapTable;
import org.apache.fontbox.type1.Type1Font;
import org.ofdrw.converter.font.type1.Type1SegSplitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(TrueTypeFont.class);

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

        // 裸CFF数据（首4字节为 CFF 主版本号与次版本号，如 0x01000402），没有 sfnt 表目录
        if (isBareCFF(version)) {
            if (!parseCFF(IOUtils.toByteArray(raf.getOriginalData()), null)) {
                throw new IllegalArgumentException("无法解析 CFF 字体数据");
            }
            return this;
        }

        // sfnt 版本标识：0x00010000 - TrueType；0x74727565('true') - TrueType（Apple 旧版标记）；
        // 0x4F54544F('OTTO') - OpenType/CFF；0x74746366('ttcf') - TTC 集合；
        // 以上版本均带有 sfnt 表目录，按同一条 sfnt 解析路径处理
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
            CFF   CFF 数据（OpenType/CFF 格式）
         */
        // =========> head
        if (!tables.containsKey("head")) {
            InputStream originalData = raf.getOriginalData();
            // head 表都不存在的情况，尝试使用CFF格式解析字体
            if (!parseCFF(IOUtils.toByteArray(originalData), null)) {
                throw new IllegalArgumentException("没有 head 表");
            }
            return this;
        }

        // =========> CFF 表（OpenType/CFF 格式）
        if (tables.containsKey("CFF ")) {
            // CFF 格式字体，不需要 loca/glyf 表
            // 读取 CFF 表
            long cffOffset = tables.get("CFF ")[0];
            long cffLength = tables.get("CFF ")[1];
            raf.seek(cffOffset);
            byte[] cffData = new byte[(int) cffLength];
            raf.read(cffData, 0, (int) cffLength);
            if (parseCFF(cffData, tables)) {
                // =========> hhea || hmtx (CFF 字体同样需要读取字宽，用于字符步进兜底)
                if (tables.containsKey("hhea") && tables.containsKey("hmtx")) {
                    parseHorizontalMetrics(tables);
                }

                // =========> cmap (CFF 字体也需要字符到字形的映射)
                if (tables.containsKey("cmap")) {
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
                return this;
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
            parseHorizontalMetrics(tables);
        }

        return this;
    }

    /**
     * 解析 hhea 与 hmtx 表，得到字体字宽信息
     * <p>
     * 表数据损坏时不影响字体其余能力的解析，仅记录告警并放弃字宽信息。
     *
     * @param tables sfnt 表目录，KEY 为表标签，VALUE 为 [偏移量, 长度]
     */
    private void parseHorizontalMetrics(Map<String, long[]> tables) {
        try {
            final long[] hhOff = tables.get("hhea");
            final long[] hmOff = tables.get("hmtx");
            int numberOfHMetrics = new HorizontalHeaderTable(hhOff).parse(data).getNumberOfHMetrics();
            hmt = new HorizontalMetricsTable(hmOff).parse(numberOfHMetrics, numGlyphs, data);
        } catch (Exception e) {
            hmt = null;
            logger.warn("解析 hhea/hmtx 字宽表失败，字符步进将无法按字宽兜底", e);
        }
    }

    /**
     * 通过字形的Index获取字形数据
     *
     * @param gid 字形Index
     * @throws IOException IOE
     */
    @Override
    public GlyphData getGlyph(int gid) throws IOException {
        if (gid < 0 || (numGlyphs > 0 && gid >= numGlyphs)) {
            return null;
        }

        if (cffFont != null) {
            // CFF 字体：通过 CFF 获取路径，包装成 GlyphData 返回
            Type2CharString cs = this.cffFont.getType2CharString(gid);
            if (cs != null) {
                return new GlyphData(cs.getPath());
            }
            return new GlyphData();
        }

        if (glyphOffsets == null || glyphOffsets.length <= gid + 1) {
            // 没有 sfnt 字形索引表，无法读取字形
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
        // 字形索引越界，找不到字形
        if (gid < 0 || (numGlyphs > 0 && gid >= numGlyphs)) {
            return null;
        }
        // 存在CFF的时候采用CFF直接获取字形
        if (this.cffFont != null) {
            try {
                org.apache.fontbox.cff.Type2CharString charString = this.cffFont.getType2CharString(gid);
                return charString != null ? charString.getPath() : null;
            } catch (Exception e) {
                // 字形数据异常时跳过该字形，不影响其余文字的渲染
                logger.warn("CFF 字体获取字形路径失败，gid=" + gid, e);
                return null;
            }
        }
        GlyphData glyph = getGlyph(gid);
        if (glyph == null) {
            return null;
        }
        return glyph.getPath();
    }

    /**
     * 获取字形在字体单位下的水平宽度（字宽）
     * <p>
     * 字宽取自字体 hmtx 表，单位为字体单位（1 个字体单位 = 1 / unitsPerEm 个 em），
     * 渲染时需要结合 {@link #getFontMatrix()} 换算为页面尺寸。
     *
     * @param gid 字形索引号
     * @return 字宽，单位为字体单位；字形索引越界或字体没有 hmtx 表时返回 -1
     */
    public int getAdvanceWidth(int gid) {
        if (gid < 0 || (numGlyphs > 0 && gid >= numGlyphs)) {
            return -1;
        }
        if (hmt == null) {
            return -1;
        }
        try {
            return hmt.getAdvanceWidth(gid);
        } catch (Exception e) {
            logger.debug("获取字形字宽失败，gid=" + gid, e);
            return -1;
        }
    }


    /**
     * 判断字体数据是否为裸 CFF 数据
     * <p>
     * 裸 CFF 数据没有 sfnt 表目录，首4字节为 CFF 头部的 主版本号(1字节) + 次版本号(1字节) + hdrSize(1字节) + offSize(1字节)，
     * 其中主次版本号固定为 1.0。
     *
     * @param version 字体数据的首4字节
     * @return true - 裸 CFF 数据；false - 其他格式
     */
    private static boolean isBareCFF(byte[] version) {
        if (version == null || version.length < 4) {
            return false;
        }
        // 主版本号必须为 1
        if ((version[0] & 0xff) != 0x01) {
            return false;
        }
        int headVersion = ((version[0] & 0xff) << 24) | ((version[1] & 0xff) << 16)
                | ((version[2] & 0xff) << 8) | (version[3] & 0xff);
        return headVersion == 0x01000402 || headVersion == 0x01000403;
    }

    /**
     * 使用 CFF 解析器解析字体数据，并记录 CFF 字体对象
     * <p>
     * 存在 sfnt 表目录时从 head、maxp 表读取 unitsPerEm 与 numGlyphs，字体名称由 name 表读取；
     * 否则（裸 CFF）字形数量取自 CFF 的 Top DICT（CharStrings INDEX），
     * unitsPerEm 取自 Top DICT（FontMatrix），字体名称来自 CFF 字体本身。
     *
     * @param cffData CFF 字体数据
     * @param tables  sfnt 表目录，KEY 为表标签，VALUE 为 [偏移量, 长度]；裸 CFF 时为 null
     * @return true - 解析成功；false - 数据中没有可用的 CFF 字体
     * @throws IOException IO异常
     */
    private boolean parseCFF(byte[] cffData, Map<String, long[]> tables) throws IOException {
        if (cffData == null || cffData.length == 0) {
            return false;
        }
        List<CFFFont> fonts = new CFFParser().parse(cffData);
        if (fonts == null || fonts.isEmpty()) {
            return false;
        }
        this.cffFont = fonts.get(0);
        // 字形数量来自 CFF 的 CharStrings INDEX
        numGlyphs = cffFont.getNumCharStrings();
        unitsPerEm = 0;
        if (tables != null && tables.containsKey("head")) {
            long headOffset = tables.get("head")[0];
            data.seek(headOffset + 18);
            unitsPerEm = data.readUnsignedShort();
            // CFF 字体同样需要从 maxp 读取字形数量（与 CFF CharStrings 一致）
            if (tables.containsKey("maxp")) {
                data.seek(tables.get("maxp")[0] + 4);
                numGlyphs = data.readUnsignedShort();
            }
        }
        if (tables == null) {
            // 裸 CFF 没有 head 表，unitsPerEm 由 CFF 的 Top DICT（FontMatrix）决定，
            // 与 getFontMatrix() 保持一致，无法确定时按 1000 单位/em 处理
            unitsPerEm = 1000;
            logger.debug("已按裸 CFF 数据解析字体 {}，字形数量 {}，unitsPerEm {}", cffFont.getName(), numGlyphs, unitsPerEm);
        }
        if (this.psName == null) {
            this.psName = cffFont.getName();
        }
        return true;
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
        if (unitsPerEm <= 0) {
            // 没有有效的 unitsPerEm（如裸 CFF 未提供 FontMatrix），按 1000 单位/em 处理
            float fallbackScale = 0.001f;
            return Arrays.<Number>asList(fallbackScale, 0, 0, fallbackScale, 0, 0);
        }
        float scale = 1000f / unitsPerEm;
        return Arrays.<Number>asList(0.001f * scale, 0, 0, 0.001f * scale, 0, 0);
    }

}
