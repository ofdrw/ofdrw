package font;


import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * https://docs.microsoft.com/zh-cn/typography/opentype/spec/otff
 *
 * @author 权观宇
 * @since 2021-09-28 21:05:09
 */
public class TrueTypeFont {

    /**
     * 偏移量计算方式
     * <pre>
     * 0 for short offsets (Offset16), 1 for long (Offset32).
     *  1: offset = readUnit16() * 2
     *  0: offset = readUnit32()
     * </pre>
     */
    private short indexToLocFormat;
    /**
     * 字形数量
     */
    private int numGlyphs;

    /**
     * 字形偏移量数组
     */
    private long[] glyphOffsets;

    void parse(TTFDataStream raf) throws IOException {
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
        indexToLocFormat = raf.readSignedShort();
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
        // todo 解析字形
    }

//    /**
//     * Returns the data for the glyph with the given GID.
//     *
//     * @param gid GID
//     * @throws IOException if the font cannot be read
//     */
//    public org.apache.fontbox.ttf.GlyphData getGlyph(int gid) throws IOException
//    {
//        if (gid < 0 || gid >= numGlyphs)
//        {
//            return null;
//        }
//
//        if (glyphs != null && glyphs[gid] != null)
//        {
//            return glyphs[gid];
//        }
//
//        org.apache.fontbox.ttf.GlyphData glyph;
//
//        // PDFBOX-4219: synchronize on data because it is accessed by several threads
//        // when PDFBox is accessing a standard 14 font for the first time
//        synchronized (data)
//        {
//            // read a single glyph
//            long[] offsets = loca.getOffsets();
//
//            if (offsets[gid] == offsets[gid + 1])
//            {
//                // no outline
//                // PDFBOX-5135: can't return null, must return an empty glyph because
//                // sometimes this is used in a composite glyph.
//                glyph = new org.apache.fontbox.ttf.GlyphData();
//                glyph.initEmptyData();
//            }
//            else
//            {
//                // save
//                long currentPosition = data.getCurrentPosition();
//
//                data.seek(getOffset() + offsets[gid]);
//
//                glyph = getGlyphData(gid);
//
//                // restore
//                data.seek(currentPosition);
//            }
//
//            if (glyphs != null && glyphs[gid] == null && cached < MAX_CACHED_GLYPHS)
//            {
//                glyphs[gid] = glyph;
//                ++cached;
//            }
//
//            return glyph;
//        }
//    }
//
//    private GlyphData getGlyphData(int gid) throws IOException
//    {
//        GlyphData glyph = new GlyphData();
//        glyph.initData(this, data, 0);
//        // resolve composite glyph
//        if (glyph.getDescription().isComposite())
//        {
//            glyph.getDescription().resolve();
//        }
//        return glyph;
//    }
}
