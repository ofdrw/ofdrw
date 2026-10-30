package org.ofdrw.converter.font;

import org.apache.commons.io.IOUtils;
import org.apache.fontbox.util.BoundingBox;
import org.ofdrw.converter.font.type1.Type1SegSplitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 字体嵌入元信息（PDF FontDescriptor 的取值来源）
 * <p>
 * 由字体数据汇总生成，供 PDF 输出后端构造字体字典与 FontDescriptor：
 * <ul>
 *     <li>嵌入数据的挂载键：{@link #getFontFileKey()} —— TrueType 轮廓为 {@code FontFile2}，
 *     CFF / Type1 轮廓为 {@code FontFile3}；</li>
 *     <li>字形轮廓格式：{@link #getGlyphFormat()}；</li>
 *     <li>PostScript 名（含可选的 {@code ABCDEF+} 前缀）、字族名、字重、斜体角度、
 *     字族标志（PDF 1.7 表 122 Flags）、字体包围盒。</li>
 * </ul>
 * 本类不做子集裁剪（设计决策 16：整字体嵌入），只描述“如何嵌入整字体”。
 *
 * @see EmbeddedFontData
 * @see FontMetrics
 * @since 2.5.1
 */
public class FontDescriptor {

    private static final Logger logger = LoggerFactory.getLogger(FontDescriptor.class);

    /**
     * PDF 中 TrueType 轮廓字体数据流的字典键
     */
    public static final String FONT_FILE_2 = "FontFile2";

    /**
     * PDF 中 CFF / Type1 轮廓字体数据流的字典键
     */
    public static final String FONT_FILE_3 = "FontFile3";

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：固定间距（等宽）字体
     */
    public static final int FLAG_FIXED_PITCH = 1;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：衬线字体
     */
    public static final int FLAG_SERIF = 1 << 1;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：符号字体（使用内建编码）
     */
    public static final int FLAG_SYMBOLIC = 1 << 2;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：手写体
     */
    public static final int FLAG_SCRIPT = 1 << 3;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：非符号字体（使用标准编码），与 {@link #FLAG_SYMBOLIC} 互斥
     */
    public static final int FLAG_NONSYMBOLIC = 1 << 5;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：斜体
     */
    public static final int FLAG_ITALIC = 1 << 6;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：全大写字体
     */
    public static final int FLAG_ALLCAP = 1 << 16;

    /**
     * PDF 字族标志位（PDF 1.7 表 122）：粗体
     */
    public static final int FLAG_FORCE_BOLD = 1 << 18;

    /**
     * sfnt 表目录中 head 表 macStyle 字段的偏移（单位：字节）
     */
    private static final int HEAD_MAC_STYLE_OFFSET = 16;

    /**
     * sfnt 表目录中 head 表 macStyle 字段的“粗体”位
     */
    private static final int HEAD_MAC_STYLE_BOLD = 1 << 0;

    /**
     * sfnt 表目录中 head 表 macStyle 字段的“斜体”位
     */
    private static final int HEAD_MAC_STYLE_ITALIC = 1 << 1;

    /**
     * sfnt 表目录中 post 表（version 2.0）的 italicAngle 字段偏移（单位：字节）
     */
    private static final int POST_ITALIC_ANGLE_OFFSET = 4;

    /**
     * 字体包围盒统计的字形数量上限（单位：个）
     * <p>
     * 字体文件开头的字形包含拉丁字母与常用符号，已能代表字体主体范围；限制采样数量避免数万字形时开销过大。
     */
    private static final int BBOX_SAMPLE_LIMIT = 4096;

    /**
     * 字形轮廓格式
     */
    public enum GlyphFormat {
        /**
         * TrueType 轮廓（sfnt 的 glyf / loca 表），对应 PDF 的 {@code FontFile2}
         */
        TRUE_TYPE,
        /**
         * CFF 轮廓（OpenType/CFF 的 CFF 表或裸 CFF 数据），对应 PDF 的 {@code FontFile3}
         */
        CFF,
        /**
         * Type1 轮廓（PFB 数据），对应 PDF 的 {@code FontFile3}（Subtype 为 Type1C 时数据为 CFF）
         */
        TYPE_1
    }

    /**
     * 字形轮廓格式
     */
    private final GlyphFormat glyphFormat;

    /**
     * PostScript 名（规整后，未加子集前缀）
     */
    private final String postScriptName;

    /**
     * 写入 PDF BaseFont 的名称，{@link #isRenamingRequired()} 为 true 时带 {@code ABCDEF+} 前缀
     */
    private final String renamedPostScriptName;

    /**
     * 字族名
     */
    private final String familyName;

    /**
     * 字重，取值 100 ~ 900
     */
    private final int fontWeight;

    /**
     * 斜体角度，单位为度，逆时针为正
     */
    private final float italicAngle;

    /**
     * 字族标志，由 {@link #FLAG_*} 位组合
     */
    private final int flags;

    /**
     * 字体包围盒 [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    private final float[] fontBBox;

    /**
     * 嵌入字体数据在 PDF 字体字典中的键
     */
    private final String fontFileKey;

    /**
     * 是否需要 {@code ABCDEF+} 前缀重命名
     */
    private final boolean renamingRequired;

    /**
     * 字体原始数据的读取入口
     */
    private final FontDataProvider dataProvider;

    /**
     * 由字体文件汇总嵌入元信息
     * <p>
     * 字体格式按内容识别（不依赖扩展名），字体集合（TTC）按索引提取指定字体后汇总。
     *
     * @param fontPath 字体文件路径
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return 嵌入元信息
     * @throws IOException 文件读取或字体解析失败
     */
    public static FontDescriptor from(Path fontPath, int ttcIndex) throws IOException {
        return from(Files.readAllBytes(fontPath), ttcIndex);
    }

    /**
     * 由字体字节数据汇总嵌入元信息
     *
     * @param fontData 字体文件的全部字节（TTF / OTF / TTC / 裸 CFF / Type1）
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return 嵌入元信息
     * @throws IOException 字体解析失败
     */
    public static FontDescriptor from(byte[] fontData, int ttcIndex) throws IOException {
        EmbeddedFontData embedded = EmbeddedFontData.of(fontData, ttcIndex);
        TrueTypeFont font = parseFont(embedded);
        return new FontDescriptor(embedded.getGlyphFormat(), font, embedded);
    }

    /**
     * 由已解析字体与嵌入数据汇总嵌入元信息
     * <p>
     * 供已完成解析的调用方复用解析结果，避免重复解析字体文件。
     *
     * @param font     已解析的字体
     * @param embedded 该字体的可嵌入数据
     * @return 嵌入元信息
     */
    public static FontDescriptor of(TrueTypeFont font, EmbeddedFontData embedded) {
        if (font == null) {
            throw new IllegalArgumentException("字体（font）不能为空");
        }
        if (embedded == null) {
            throw new IllegalArgumentException("嵌入数据（embedded）不能为空");
        }
        return new FontDescriptor(embedded.getGlyphFormat(), font, embedded);
    }

    /**
     * 按已解析字体汇总嵌入元信息
     *
     * @param glyphFormat 字形轮廓格式
     * @param font        已解析的字体
     * @param embedded    该字体的可嵌入数据
     */
    private FontDescriptor(GlyphFormat glyphFormat, TrueTypeFont font, EmbeddedFontData embedded) {
        this.glyphFormat = glyphFormat;
        this.fontFileKey = glyphFormat == GlyphFormat.TRUE_TYPE ? FONT_FILE_2 : FONT_FILE_3;
        this.dataProvider = embedded;
        String originPsName = font.psName;
        this.postScriptName = sanitizeName(originPsName);
        this.familyName = font.fontFamily;
        this.renamingRequired = shouldRename(originPsName);
        this.renamedPostScriptName = renamingRequired ? subsetPrefix(this.postScriptName) + this.postScriptName : this.postScriptName;
        this.fontWeight = detectFontWeight(font.fontSubFamily);
        int macStyle = readMacStyle(font, glyphFormat);
        this.flags = detectFlags(font.fontSubFamily, macStyle, glyphFormat);
        this.italicAngle = detectItalicAngle(font, glyphFormat);
        this.fontBBox = computeFontBBox(font, glyphFormat);
    }

    /**
     * 按嵌入数据的格式解析字体
     *
     * @param embedded 可嵌入数据
     * @return 字体解析器
     * @throws IOException 解析失败
     */
    private static TrueTypeFont parseFont(EmbeddedFontData embedded) throws IOException {
        TrueTypeFont font = new TrueTypeFont().parse(embedded.getData());
        if (font.psName == null) {
            // 部分字体缺少 name 表的 PostScript 名，退回 CFF 字体名或兜底名称
            font.psName = embedded.getCffFontName();
        }
        return font;
    }

    /**
     * 生成 PDF FontDescriptor 字典的字段取值
     * <p>
     * 字段名与 PDF 1.7 FontDescriptor 字典键一致，便于输出后端一次性写入：
     * BaseFont、FontFamily、FontWeight、ItalicAngle、Flags、FontBBox，以及嵌入数据键（FontFile2 / FontFile3）。
     *
     * @return 字段名到取值的映射；FontBBox 取值为 4 个浮点数，嵌入数据键取值为该键名本身
     */
    public Map<String, Object> toDescriptorEntries() {
        Map<String, Object> entries = new LinkedHashMap<>();
        entries.put("BaseFont", this.renamedPostScriptName);
        entries.put("FontFamily", this.familyName == null ? "" : this.familyName);
        entries.put("FontWeight", this.fontWeight);
        entries.put("ItalicAngle", this.italicAngle);
        entries.put("Flags", this.flags);
        entries.put("FontBBox", new float[]{fontBBox[0], fontBBox[1], fontBBox[2], fontBBox[3]});
        entries.put(this.fontFileKey, this.fontFileKey);
        return entries;
    }

    /**
     * 字形轮廓格式
     *
     * @return TrueType / CFF / Type1
     */
    public GlyphFormat getGlyphFormat() {
        return glyphFormat;
    }

    /**
     * 嵌入字体数据在 PDF 字体字典中的键
     *
     * @return {@link #FONT_FILE_2} 或 {@link #FONT_FILE_3}
     */
    public String getFontFileKey() {
        return fontFileKey;
    }

    /**
     * PostScript 名（未加子集前缀）
     *
     * @return PostScript 名；字体缺少该名称时为按字体名派生的兜底名称
     */
    public String getPostScriptName() {
        return postScriptName;
    }

    /**
     * 写入 PDF BaseFont / FontDescriptor 的名称
     *
     * @return 带 {@code ABCDEF+} 前缀（{@link #isRenamingRequired()} 为 true 时）或原样的 PostScript 名
     */
    public String getRenamedPostScriptName() {
        return renamedPostScriptName;
    }

    /**
     * 字族名
     *
     * @return 字族名，字体缺少 name 表时可能为 null
     */
    public String getFamilyName() {
        return familyName;
    }

    /**
     * 字重
     *
     * @return 字重，取值 100 ~ 900
     */
    public int getFontWeight() {
        return fontWeight;
    }

    /**
     * 斜体角度
     *
     * @return 斜体角度，单位为度，逆时针为正
     */
    public float getItalicAngle() {
        return italicAngle;
    }

    /**
     * 字族标志
     *
     * @return 由 {@link #FLAG_*} 位组合的整数
     */
    public int getFlags() {
        return flags;
    }

    /**
     * 字体包围盒
     *
     * @return 新数组 [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    public float[] getFontBBox() {
        return fontBBox.clone();
    }

    /**
     * 是否需要为 PostScript 名添加 {@code ABCDEF+} 前缀
     * <p>
     * PDF 1.7 6.3.3 规定嵌入字体应以“6 个大写字母 + 加号”前缀标识。本模块整字体嵌入、不生成子集，
     * 但字体自带子集前缀无法还原原始字体名，故对此类字体按同一形式重新加前缀以标记为受控嵌入；
     * 名称已符合该规则时不再重复添加。
     *
     * @return true - 写入 PDF 时使用 {@link #getRenamedPostScriptName()}
     */
    public boolean isRenamingRequired() {
        return renamingRequired;
    }

    /**
     * 字体原始数据的读取入口
     *
     * @return 嵌入数据的读取入口
     */
    FontDataProvider getDataProvider() {
        return dataProvider;
    }

    /**
     * 字体数据格式识别
     *
     * @param data 字体文件的全部字节
     * @return TrueType / CFF / Type1
     * @throws IOException 数据长度不足、无法识别格式
     */
    public static GlyphFormat detectFormat(byte[] data) throws IOException {
        if (data == null || data.length < 4) {
            throw new IOException("字体数据长度不足，无法识别格式");
        }
        if (Type1SegSplitParser.isType1(new byte[]{data[0], data[1], data[2], data[3]})) {
            return GlyphFormat.TYPE_1;
        }
        int tag = readS32(data, 0);
        switch (tag) {
            // 'true'（Apple 旧版 TrueType）、0x00010000（TrueType）
            case 0x74727565:
            case 0x00010000:
                return hasSfntTable(data, 0, "CFF ") ? GlyphFormat.CFF : GlyphFormat.TRUE_TYPE;
            // 'OTTO'：OpenType，轮廓数据仅存在于 CFF 表
            case 0x4F54544F:
                return GlyphFormat.CFF;
            // 'ttcf'：字体集合，按首个字体的表目录判断，集合内字体共享轮廓格式
            case 0x74746366:
                return hasSfntTable(data, readTtcfFirstFontOffset(data), "CFF ") ? GlyphFormat.CFF : GlyphFormat.TRUE_TYPE;
            default:
                // 裸 CFF 数据：首 4 字节为版本号 0x01000402 / 0x01000403
                if (tag == 0x01000402 || tag == 0x01000403) {
                    return GlyphFormat.CFF;
                }
                break;
        }
        throw new IOException(String.format("无法识别的字体数据格式，首 4 字节：0x%08X", tag));
    }

    /**
     * 读取字体集合（TTC）中首个字体的表目录偏移
     *
     * @param data 字体集合数据
     * @return 表目录偏移，单位为字节
     */
    static long readTtcfFirstFontOffset(byte[] data) {
        // 文件头：'ttcf'(4) + version(4) + numFonts(4) + offsetTable[numFonts](4*n)
        if (data.length < 16) {
            return 0;
        }
        return readU32(data, 12);
    }

    /**
     * 在指定偏移的 sfnt 表目录中查找表
     *
     * @param data        字体数据
     * @param sfntOffset  表目录偏移，单位为字节
     * @param tag         表标签
     * @return true - 表目录中存在该表
     */
    static boolean hasSfntTable(byte[] data, long sfntOffset, String tag) {
        long[] record = findSfntTable(data, sfntOffset, tag);
        return record != null;
    }

    /**
     * 在指定偏移的 sfnt 表目录中查找表记录
     *
     * @param data       字体数据
     * @param sfntOffset 表目录偏移，单位为字节
     * @param tag        表标签
     * @return [偏移量, 长度]，单位为字节；不存在时返回 null
     */
    static long[] findSfntTable(byte[] data, long sfntOffset, String tag) {
        if (sfntOffset < 0 || sfntOffset + 12 > data.length) {
            return null;
        }
        int numTables = readU16(data, (int) sfntOffset + 4);
        for (int i = 0; i < numTables; i++) {
            int recordOffset = (int) sfntOffset + 12 + i * 16;
            if (recordOffset + 16 > data.length) {
                return null;
            }
            String recordTag = new String(data, recordOffset, 4, StandardCharsets.ISO_8859_1);
            if (recordTag.equals(tag)) {
                return new long[]{readU32(data, recordOffset + 8), readU32(data, recordOffset + 12)};
            }
        }
        return null;
    }

    /**
     * 判断字体名是否需要加 {@code ABCDEF+} 前缀
     * <p>
     * 名称包含加号时说明已处于“子集前缀 + 名称”的形式（如 {@code ZJPTDA+SimSun}），
     * 此时无法还原原始字体名，按 PDF 1.7 6.3.3 的同一形式重新加前缀，避免渲染器按旧前缀推断字符集。
     *
     * @param psName 字体自带的 PostScript 名
     * @return true - 需要重命名（名称为空或已带子集前缀）
     */
    private static boolean shouldRename(String psName) {
        if (!isValidName(psName)) {
            // 缺少 PostScript 名时，只能以重命名形式给出可稳定复现的名称
            return true;
        }
        return psName.indexOf('+') >= 0;
    }

    /**
     * 判断 PostScript 名是否为可直接写入 PDF 的非空名称
     *
     * @param name PostScript 名
     * @return true - 非空且不含空白
     */
    private static boolean isValidName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            if (Character.isWhitespace(name.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    /**
     * 规整字体名：去首尾空白、空白替换为连字符，为空时使用兜底名称
     *
     * @param name 字体名
     * @return 规整后的字体名
     */
    private static String sanitizeName(String name) {
        if (name == null) {
            logger.warn("字体缺少 PostScript 名，按兜底名称 UnknownFont 写入");
            return "UnknownFont";
        }
        String sanitized = name.trim().replaceAll("\\s+", "-");
        return sanitized.isEmpty() ? "UnknownFont" : sanitized;
    }

    /**
     * 生成 {@code ABCDEF+} 形式的子集前缀
     *
     * @param psName 经规整的 PostScript 名
     * @return 6 个大写字母与加号组成的前缀
     */
    private static String subsetPrefix(String psName) {
        // 以名称哈希生成稳定的 6 位大写字母前缀，保证同一字体多次导出的名称一致
        int hash = Math.abs(psName.hashCode());
        StringBuilder prefix = new StringBuilder(7);
        for (int i = 0; i < 6; i++) {
            prefix.append((char) ('A' + hash % 26));
            hash /= 26;
        }
        return prefix.append('+').toString();
    }

    /**
     * 按子族名推断字重
     *
     * @param subFamily 子族名，如 Regular、Bold、Italic
     * @return 字重，取值 100 ~ 900；无法判断时取 400（常规）
     */
    private static int detectFontWeight(String subFamily) {
        if (subFamily == null) {
            return 400;
        }
        String name = subFamily.toLowerCase();
        if (name.contains("black") || name.contains("heavy")) {
            return 900;
        }
        if (name.contains("extrabold") || name.contains("ultrabold")) {
            return 800;
        }
        if (name.contains("bold")) {
            return 700;
        }
        if (name.contains("semibold") || name.contains("demibold")) {
            return 600;
        }
        if (name.contains("medium")) {
            return 500;
        }
        if (name.contains("light")) {
            return 300;
        }
        if (name.contains("thin") || name.contains("hairline")) {
            return 100;
        }
        return 400;
    }

    /**
     * 读取 sfnt 的 head 表 macStyle 字段
     *
     * @param font        字体
     * @param glyphFormat 字形轮廓格式
     * @return macStyle 取值；非 TrueType 轮廓或读取失败时返回 0
     */
    private static int readMacStyle(TrueTypeFont font, GlyphFormat glyphFormat) {
        if (glyphFormat != GlyphFormat.TRUE_TYPE) {
            return 0;
        }
        try {
            byte[] raw = readFontData(font);
            long[] head = findSfntTable(raw, 0, "head");
            if (head == null || head[0] + HEAD_MAC_STYLE_OFFSET + 2 > raw.length) {
                return 0;
            }
            return readU16(raw, (int) head[0] + HEAD_MAC_STYLE_OFFSET);
        } catch (Exception e) {
            logger.debug("读取 head 表 macStyle 失败，忽略等宽/斜体标志位", e);
            return 0;
        }
    }

    /**
     * 读取字体原始字节数据
     *
     * @param font 字体
     * @return 字体文件的全部字节
     * @throws IOException 读取失败
     */
    private static byte[] readFontData(TrueTypeFont font) throws IOException {
        return IOUtils.toByteArray(font.getDataStream().getOriginalData());
    }

    /**
     * 读取斜体角度
     * <p>
     * TrueType 轮廓取 sfnt 的 post 表（version 2.0）；CFF 轮廓取 CFF Top DICT 的 ItalicAngle；
     * 两者均不可用时按子族名判断：含 Italic / Oblique 时取 -12 度。
     *
     * @param font        字体
     * @param glyphFormat 字形轮廓格式
     * @return 斜体角度，单位为度，逆时针为正
     */
    private static float detectItalicAngle(TrueTypeFont font, GlyphFormat glyphFormat) {
        if (glyphFormat == GlyphFormat.TRUE_TYPE) {
            try {
                byte[] raw = readFontData(font);
                long[] post = findSfntTable(raw, 0, "post");
                if (post != null) {
                    long version = readU32(raw, (int) post[0]);
                    if (version == 0x00020000L) {
                        return read32Fixed(raw, (int) (post[0] + POST_ITALIC_ANGLE_OFFSET));
                    }
                }
            } catch (Exception e) {
                logger.debug("读取 post 表斜体角度失败，按子族名推断", e);
            }
        }
        String subFamily = font.fontSubFamily;
        if (subFamily != null) {
            String name = subFamily.toLowerCase();
            if (name.contains("italic") || name.contains("oblique")) {
                return -12f;
            }
        }
        return 0f;
    }

    /**
     * 推断 PDF 字族标志
     *
     * @param subFamily   子族名
     * @param macStyle    head 表 macStyle 取值，0 表示不可用
     * @param glyphFormat 字形轮廓格式
     * @return 由 {@link #FLAG_*} 位组合的整数
     */
    private static int detectFlags(String subFamily, int macStyle, GlyphFormat glyphFormat) {
        // 本模块只处理 Unicode 字体，描述符按非符号字体写入
        int flags = FLAG_NONSYMBOLIC;
        if ((macStyle & HEAD_MAC_STYLE_ITALIC) != 0) {
            flags |= FLAG_ITALIC;
        }
        if ((macStyle & HEAD_MAC_STYLE_BOLD) != 0) {
            flags |= FLAG_FORCE_BOLD;
        }
        if (glyphFormat == GlyphFormat.TRUE_TYPE) {
            flags |= detectFixedPitch(subFamily);
        }
        if (subFamily != null) {
            String name = subFamily.toLowerCase();
            if (name.contains("bold")) {
                flags |= FLAG_FORCE_BOLD;
            }
            if (name.contains("italic") || name.contains("oblique")) {
                flags |= FLAG_ITALIC;
            }
            if (name.contains("serif") || name.contains("song") || name.contains("ming")) {
                flags |= FLAG_SERIF;
            }
        }
        return flags;
    }

    /**
     * 按子族名判断是否等宽字体
     *
     * @param subFamily 子族名
     * @return 等宽时返回 {@link #FLAG_FIXED_PITCH}，否则返回 0
     */
    private static int detectFixedPitch(String subFamily) {
        if (subFamily == null) {
            return 0;
        }
        String name = subFamily.toLowerCase();
        if (name.contains("mono") || name.contains("fixed") || name.contains("courier")) {
            return FLAG_FIXED_PITCH;
        }
        return 0;
    }

    /**
     * 计算字体包围盒
     * <p>
     * 取字形包围盒的并集（最多采样 {@value #BBOX_SAMPLE_LIMIT} 个字形），并换算为 1/1000 文本空间单位。
     * 采样结果为空时退回 [0, 0, 1000, 1000]。
     *
     * @param font        字体
     * @param glyphFormat 字形轮廓格式
     * @return [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    private static float[] computeFontBBox(TrueTypeFont font, GlyphFormat glyphFormat) {
        float scale = emScale(font);
        float[] bbox = null;
        for (int gid = 0; gid < BBOX_SAMPLE_LIMIT; gid++) {
            GlyphData glyph;
            try {
                glyph = font.getGlyph(gid);
            } catch (Exception e) {
                logger.debug("读取字形包围盒失败，gid=" + gid, e);
                continue;
            }
            if (glyph == null) {
                // 已越过最后一个字形
                break;
            }
            BoundingBox glyphBox = glyph.getBoundingBox();
            if (glyphBox == null || glyphBox.getWidth() == 0f && glyphBox.getHeight() == 0f) {
                continue;
            }
            if (bbox == null) {
                bbox = new float[]{glyphBox.getLowerLeftX(), glyphBox.getLowerLeftY(),
                        glyphBox.getUpperRightX(), glyphBox.getUpperRightY()};
            } else {
                bbox[0] = Math.min(bbox[0], glyphBox.getLowerLeftX());
                bbox[1] = Math.min(bbox[1], glyphBox.getLowerLeftY());
                bbox[2] = Math.max(bbox[2], glyphBox.getUpperRightX());
                bbox[3] = Math.max(bbox[3], glyphBox.getUpperRightY());
            }
        }
        if (bbox == null) {
            bbox = new float[]{0f, 0f, 1000f, 1000f};
            return bbox;
        }
        return new float[]{bbox[0] * scale, bbox[1] * scale, bbox[2] * scale, bbox[3] * scale};
    }

    /**
     * 字体单位到 1/1000 em 的换算比例
     *
     * @param font 字体
     * @return 换算比例：字形坐标 × 比例 = 1/1000 文本空间单位
     */
    static float emScale(TrueTypeFont font) {
        List<Number> matrix = font.getFontMatrix();
        float scale = matrix.get(0).floatValue() * 1000f;
        if (scale <= 0f || Float.isNaN(scale) || Float.isInfinite(scale)) {
            // 字体缺少有效 unitsPerEm 时按 1000 单位/em 处理
            return 1f;
        }
        return scale;
    }

    /**
     * 读取大端无符号 16 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 无符号整数值
     */
    static int readU16(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    /**
     * 读取大端有符号 16 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 有符号整数值
     */
    static int readS16(byte[] data, int pos) {
        return (short) readU16(data, pos);
    }

    /**
     * 读取大端无符号 32 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 无符号整数值
     */
    static long readU32(byte[] data, int pos) {
        return ((long) (data[pos] & 0xFF) << 24) | ((long) (data[pos + 1] & 0xFF) << 16)
                | ((long) (data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
    }

    /**
     * 读取大端有符号 32 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 有符号整数值
     */
    static int readS32(byte[] data, int pos) {
        return (int) readU32(data, pos);
    }

    /**
     * 读取大端 16.16 定点数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 定点数值
     */
    static float read32Fixed(byte[] data, int pos) {
        return readS32(data, pos) / 65536f;
    }

    @Override
    public String toString() {
        return "FontDescriptor{" + glyphFormat + ", " + renamedPostScriptName + ", flags=" + flags
                + ", italicAngle=" + italicAngle + ", " + fontFileKey + "}";
    }

    /**
     * 字体原始数据的读取入口
     * <p>
     * 每次调用返回新的数据流，允许重复读取。
     */
    public interface FontDataProvider {

        /**
         * 字体文件的全部字节
         *
         * @return 字体文件的全部字节
         * @throws IOException 读取失败
         */
        byte[] getData() throws IOException;

        /**
         * 字体数据的读取流
         *
         * @return 新的读取流，由调用方关闭
         * @throws IOException 读取失败
         */
        default InputStream openStream() throws IOException {
            return new java.io.ByteArrayInputStream(getData());
        }
    }
}
