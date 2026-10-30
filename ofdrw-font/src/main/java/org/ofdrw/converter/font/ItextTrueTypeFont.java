package org.ofdrw.converter.font;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 基于 {@link TrueTypeFont} 的 TrueType 字体
 * <p>
 * 与历史 iText 实现中的 {@code com.itextpdf.io.font.ItextTrueTypeFont} 同名，用于保持该类型的可用性：
 * 直接解析单个字体文件或字体集合（TTC）中的指定字体，并提供字体名称、cmap 可用性与字形查询等访问器。
 *
 * @author qaqtutu
 * @since 2021-05-20 10:00:00
 * @deprecated 该类型仅为保持历史名称可用而保留，字体解析请直接使用 {@link TrueTypeFont}、
 * {@link TrueTypeCollection} 与 {@link FontLoader#getDefaultFont()}；释放 iText 依赖后该类型后续版本可能移除。
 */
@Deprecated
public class ItextTrueTypeFont {

    /**
     * 字体集合文件头标识
     */
    private static final String TTC_TAG = "ttcf";

    /**
     * 原始字体文件数据
     */
    private final byte[] raw;

    /**
     * 解析得到的字体
     */
    private final TrueTypeFont font;

    /**
     * cmap 表是否为空
     * <p>
     * 字体缺少 cmap 表，或 cmap 表中无法映射任何字符时为 true。
     */
    public boolean isEmptyCmap = false;

    /**
     * 字体名称
     */
    private String name;

    /**
     * 字族名称
     */
    private String familyName;

    /**
     * 使用原始字体数据创建字体
     * <p>
     * 字体集合（TTC）取集合中的第一个字体。
     *
     * @param ttf 字体文件数据
     * @throws IOException 字体数据无法解析
     */
    @Deprecated
    public ItextTrueTypeFont(byte[] ttf) throws IOException {
        this(ttf, 0);
    }

    /**
     * 使用字体集合数据与字体索引创建字体
     *
     * @param ttc      字体集合（TTC）数据
     * @param ttcIndex 字体集合内的字体索引，单位：个
     * @throws IOException 字体数据无法解析
     */
    @Deprecated
    public ItextTrueTypeFont(byte[] ttc, int ttcIndex) throws IOException {
        this.raw = ttc;
        this.font = parse(ttc, ttcIndex);
        init();
    }

    /**
     * 使用字体文件路径创建字体
     *
     * @param path 字体文件路径，支持 ttf、otf、ttc
     * @throws IOException 字体文件读取或解析失败
     */
    @Deprecated
    public ItextTrueTypeFont(String path) throws IOException {
        this(readFile(path), 0);
    }

    /**
     * 使用字体集合文件路径与字体索引创建字体
     *
     * @param ttcPath  字体集合（TTC）文件路径
     * @param ttcIndex 字体集合内的字体索引，单位：个
     * @throws IOException 字体文件读取或解析失败
     */
    @Deprecated
    public ItextTrueTypeFont(String ttcPath, int ttcIndex) throws IOException {
        this(readFile(ttcPath), ttcIndex);
    }

    /**
     * 判断字体数据是否为字体集合（TTC）
     *
     * @param data 字体文件数据
     * @return true - 字体集合；false - 单个字体文件
     *
     * @deprecated 请自行判断字体数据前 4 字节是否为 "ttcf"，或直接使用 {@link TrueTypeCollection}
     */
    @Deprecated
    public static boolean isTrueTypeCollection(byte[] data) {
        return data != null && data.length >= 4
                && TTC_TAG.equals(new String(data, 0, 4, java.nio.charset.StandardCharsets.ISO_8859_1));
    }

    /**
     * 获取字体集合中的字体数量
     *
     * @param data 字体集合数据
     * @return 字体数量，单位：个；不是字体集合时返回 0
     * @throws IOException 字体集合解析失败
     *
     * @deprecated 请使用 {@link TrueTypeCollection#parse(org.ofdrw.converter.font.TTFDataStream)} 后调用 {@link TrueTypeCollection#getNumFonts()}
     */
    @Deprecated
    public static int getTrueTypeCollectionFontCount(byte[] data) throws IOException {
        if (!isTrueTypeCollection(data)) {
            return 0;
        }
        return new TrueTypeCollection().parse(new MemoryTTFDataStream(data)).getNumFonts();
    }

    /**
     * 获取字体集合中指定索引的字体
     *
     * @param data     字体集合数据
     * @param ttcIndex 字体索引，单位：个
     * @return 字体
     * @throws IOException 字体集合解析失败
     *
     * @deprecated 请使用 {@link TrueTypeCollection#getFontAtIndex(int)}
     */
    @Deprecated
    public static TrueTypeFont getTrueTypeFontAtIndex(byte[] data, int ttcIndex) throws IOException {
        TrueTypeCollection collection = new TrueTypeCollection().parse(new MemoryTTFDataStream(data));
        int count = collection.getNumFonts();
        if (count <= 0) {
            throw new IOException("字体集合中没有可用字体");
        }
        return collection.getFontAtIndex(Math.max(0, Math.min(ttcIndex, count - 1)));
    }

    /**
     * 获取字体数据中的全部字体
     *
     * @param data 字体数据
     * @return 字体列表；单个字体文件时返回仅含该字体的列表
     * @throws IOException 字体集合解析失败
     *
     * @deprecated 请使用 {@link TrueTypeCollection#foreach(TrueTypeCollection.TrueTypeFontProcessor)}
     */
    @Deprecated
    public static List<TrueTypeFont> getTrueTypeFonts(byte[] data) throws IOException {
        List<TrueTypeFont> fonts = new ArrayList<>();
        int count = getTrueTypeCollectionFontCount(data);
        if (count <= 0) {
            // 单个字体文件
            fonts.add(new TrueTypeFont().parse(data));
            return fonts;
        }
        for (int i = 0; i < count; i++) {
            fonts.add(getTrueTypeFontAtIndex(data, i));
        }
        return fonts;
    }

    /**
     * 获取字体名称
     *
     * @return 字体名称，无法获取时返回空字符串
     *
     * @deprecated 请使用 {@link TrueTypeFont#psName}
     */
    @Deprecated
    public String getFontName() {
        return name;
    }

    /**
     * 获取字族名称
     *
     * @return 字族名称，无法获取时返回空字符串
     *
     * @deprecated 请使用 {@link TrueTypeFont#fontFamily}
     */
    @Deprecated
    public String getFamilyName() {
        return familyName;
    }

    /**
     * 获取字体变换矩阵
     *
     * @return 矩阵序列，将字体单位换算为 1/1000 em
     *
     * @deprecated 请使用 {@link TrueTypeFont#getFontMatrix()}
     */
    @Deprecated
    public List<Number> getFontMatrix() {
        return font.getFontMatrix();
    }

    /**
     * 获取字体解析对象
     *
     * @return 字体解析对象
     *
     * @deprecated 无需包装，直接使用 {@link TrueTypeFont}
     */
    @Deprecated
    public TrueTypeFont getFont() {
        return font;
    }

    /**
     * 获取原始字体文件数据
     *
     * @return 原始字体文件数据
     *
     * @deprecated 该类型不承担数据持有职责，请直接保存字体数据
     */
    @Deprecated
    public byte[] getRawData() {
        return raw;
    }

    /**
     * 获取 Unicode 编码到字形的映射
     *
     * @return 字形映射，字体没有 cmap 或无法获取时返回 null
     */
    public org.apache.fontbox.ttf.CmapLookup getUnicodeCmapLookup() {
        try {
            return font.getUnicodeCmapLookup();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 获取字形
     *
     * @param gid 字形索引
     * @return 字形数据
     * @throws IOException 字体数据读取异常
     *
     * @deprecated 请使用 {@link TrueTypeFont#getGlyph(int)}
     */
    @Deprecated
    public GlyphData getGlyph(int gid) throws IOException {
        return font.getGlyph(gid);
    }

    /**
     * 获取根据 Unicode 编码映射出的字形
     * <p>
     * 字体没有 cmap 时返回空白字形。
     *
     * @param c 字符
     * @return 字形数据
     * @throws IOException 字体数据读取异常
     *
     * @deprecated 请使用 {@link TrueTypeFont#getUnicodeGlyph(int)}
     */
    @Deprecated
    public GlyphData getUnicodeGlyph(int c) throws IOException {
        return font.getUnicodeGlyph(c);
    }

    /**
     * 解析字体数据
     *
     * @param data     字体文件数据
     * @param ttcIndex 字体集合内的字体索引，单位：个；非字体集合时忽略
     * @return 字体
     * @throws IOException 字体数据无法解析
     */
    private static TrueTypeFont parse(byte[] data, int ttcIndex) throws IOException {
        if (isTrueTypeCollection(data)) {
            return getTrueTypeFontAtIndex(data, ttcIndex);
        }
        return new TrueTypeFont().parse(data);
    }

    /**
     * 读取字体文件数据
     *
     * @param filePath 字体文件路径
     * @return 字体文件数据
     * @throws IOException 文件读取失败
     */
    private static byte[] readFile(String filePath) throws IOException {
        Path path = Paths.get(filePath);
        try (InputStream in = Files.newInputStream(path)) {
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int len;
            while ((len = in.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
            return bos.toByteArray();
        }
    }

    /**
     * 初始化字体名称与 cmap 状态
     */
    private void init() {
        name = font.psName == null ? "" : font.psName;
        familyName = font.fontFamily == null ? "" : font.fontFamily;
        try {
            // 字体没有 cmap 时 getUnicodeCmapLookup 会抛出异常，此时视为 cmap 为空
            org.apache.fontbox.ttf.CmapLookup lookup = font.getUnicodeCmapLookup();
            boolean empty = true;
            for (int c = 0x20; c < 0x7E; c++) {
                if (lookup.getGlyphId(c) != 0) {
                    empty = false;
                    break;
                }
            }
            isEmptyCmap = empty;
        } catch (Exception e) {
            isEmptyCmap = true;
        }
    }
}
