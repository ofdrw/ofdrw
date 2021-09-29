package org.ofdrw.converter.font;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * TrueType 字体解析器
 * <p>
 * https://docs.microsoft.com/zh-cn/typography/opentype/spec/otff
 *
 * @author 权观宇
 * @since 2021-09-28 21:05:09
 */
public class TrueTypeFont implements GlyphDataProvider {

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

    private long[] glyOffset;

    public TrueTypeFont() {
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
        raf.read32Fixed();
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
        // head  头数据
        // maxp  字形数量
        // loca  配置参数
        // glyf  字形数据
        // =========> head
        if (!tables.containsKey("head")) {
            throw new IllegalArgumentException("没有 head 表");
        }
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
     * 从但前偏移量位置读取字形
     *
     * @param raf 字体数据
     * @param gid 字体ID
     * @return 字形数据
     * @throws IOException IOE
     */
    private GlyphData getGlyphData(TTFDataStream raf, int gid) throws IOException {
        GlyphData glyph = new GlyphData();
//        HorizontalMetricsTable hmt = font.getHorizontalMetrics();
//        int leftSideBearing = hmt == null ? 0 : hmt.getLeftSideBearing(gid);
        glyph.readData(raf, 0, this);
        // resolve composite glyph
        if (glyph.getDescription().isComposite()) {
            glyph.getDescription().resolve();
        }
        return glyph;
    }
}
