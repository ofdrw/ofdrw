package org.ofdrw.converter.font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * ToUnicode CMap 生成器（保证 PDF 中的文字可提取、可搜索）
 * <p>
 * 按设计决策 3“字形优先 + ToUnicode”：文字以字形索引（GID）写入 PDF 后，渲染结果与 OFD 一致，
 * 但 PDF 中只有字形编号；由本类生成 ToUnicode CMap 把字形编号映射回 Unicode，
 * 阅读器即可正确复制、搜索与提取文字。
 * <p>
 * 生成的 CMap 遵循 PDF 1.7 9.10.3：
 * <ul>
 *     <li>头部：{@code /CIDInit /ProcSet findresource begin} 与 12 dict begin / begincmap；</li>
 *     <li>编码空间声明 {@code codespacerange}，单字节码位按 {@code <00> <FF>}，双字节码位按 {@code <0000> <FFFF>} 声明；</li>
 *     <li>{@code bfchar}：字形编号到单个或多个 Unicode 码位（多码位用于连字、组合字符）；</li>
 *     <li>{@code bfrange}：字形编号到连续 Unicode 区间的批量映射（压缩输出体积）。</li>
 * </ul>
 * 字形编号为 PDF 字符串中的字节序列：字形总数不超过 256 时使用 1 字节编码，否则使用 2 字节编码。
 *
 * @since 2.5.1
 */
public class ToUnicodeCMap {

    private static final Logger logger = LoggerFactory.getLogger(ToUnicodeCMap.class);

    /**
     * 单个 bfchar / bfrange 块的最大条目数（PDF 规范上限为 100）
     */
    private static final int MAX_BLOCK_SIZE = 100;

    /**
     * 字体 PostScript 名（写入 CMap 的 CMapName）
     */
    private final String fontName;

    /**
     * 字形编号到 Unicode 码位的映射，按键（字形编号）升序排列
     */
    private final TreeMap<Integer, Integer[]> mappings = new TreeMap<>();

    /**
     * 字体度量，可为 null；保留以兼容既有构造签名（bfrange 不再输出宽度，宽度由字体字典 /W 决定）
     */
    @SuppressWarnings("unused")
    private final FontMetrics metrics;

    /**
     * 显式指定的码位字节数（1 或 2），0 表示按映射表内容推断
     * <p>
     * Type0 复合字体一律使用 Identity-H 编码，文本字符串中每个字符恒为 2 字节 CID，
     * 此时无论映射表里是否只出现小于 256 的字形编号，都必须声明双字节码空间。
     */
    private int codeBytesOverride = 0;

    /**
     * 单字节编码的码位字节数
     */
    private static final int ONE_BYTE = 1;

    /**
     * 双字节编码的码位字节数
     */
    private static final int TWO_BYTE = 2;

    /**
     * 创建 ToUnicode CMap 生成器
     *
     * @param fontName 字体 PostScript 名，写入 CMapName；为 null 时使用兜底名称 UnknownFont
     */
    public ToUnicodeCMap(String fontName) {
        this.fontName = fontName == null || fontName.isEmpty() ? "UnknownFont" : fontName;
        this.metrics = null;
    }

    /**
     * 创建带字体度量的 ToUnicode CMap 生成器
     * <p>
     * 度量仅保留以兼容既有签名：PDF 1.7 9.10.3 的 bfrange 三元组不承载字形宽度，
     * 宽度由字体字典的 {@code /W}、{@code /DW} 决定，写入宽度会被解析器误读为新的目标码位。
     *
     * @param fontName 字体 PostScript 名，写入 CMapName；为 null 时使用兜底名称 UnknownFont
     * @param metrics  字体度量
     */
    public ToUnicodeCMap(String fontName, FontMetrics metrics) {
        this.fontName = fontName == null || fontName.isEmpty() ? "UnknownFont" : fontName;
        this.metrics = metrics;
    }

    /**
     * 由字体文件创建 ToUnicode CMap 生成器，并登记字体 cmap 表中的全部 Unicode 映射
     * <p>
     * 适用于“文字按 Unicode 写入 PDF”的字体；按字形索引写字的场景应使用
     * {@link #add(int, int)} 按实际使用的字形登记映射。
     *
     * @param fontPath 字体文件路径
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return ToUnicode CMap 生成器
     * @throws IOException 文件读取或字体解析失败
     */
    public static ToUnicodeCMap fromFont(Path fontPath, int ttcIndex) throws IOException {
        return fromFont(Files.readAllBytes(fontPath), ttcIndex);
    }

    /**
     * 由字体字节数据创建 ToUnicode CMap 生成器，并登记字体 cmap 表中的全部 Unicode 映射
     *
     * @param fontData 字体文件的全部字节
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return ToUnicode CMap 生成器
     * @throws IOException 字体解析失败
     */
    public static ToUnicodeCMap fromFont(byte[] fontData, int ttcIndex) throws IOException {
        EmbeddedFontData embedded = EmbeddedFontData.of(fontData, ttcIndex);
        TrueTypeFont font = new TrueTypeFont().parse(embedded.getData());
        FontMetrics metrics = FontMetrics.of(font);
        ToUnicodeCMap cmap = new ToUnicodeCMap(font.psName, metrics);
        cmap.addAllFromFont(font);
        return cmap;
    }

    /**
     * 登记“字形编号 → Unicode 码位”映射
     *
     * @param gid    字形编号（PDF 字符串中的码位值）
     * @param unicode Unicode 码位；超出 0 ~ 0x10FFFF 的取值被忽略并记录 warning
     * @return this
     */
    public ToUnicodeCMap add(int gid, int unicode) {
        if (gid < 0) {
            logger.warn("忽略非法的字形编号：{}", gid);
            return this;
        }
        if (unicode < 0 || unicode > 0x10FFFF) {
            logger.warn("忽略非法的 Unicode 码位：0x{}，字形编号 {}", Integer.toHexString(unicode), gid);
            return this;
        }
        return add(gid, new int[]{unicode});
    }

    /**
     * 登记“字形编号 → Unicode 码位序列”映射（连字、组合字符等一对多情形）
     *
     * @param gid     字形编号（PDF 字符串中的码位值）
     * @param unicodes Unicode 码位序列，至少 1 个
     * @return this
     */
    public ToUnicodeCMap add(int gid, int[] unicodes) {
        if (gid < 0) {
            logger.warn("忽略非法的字形编号：{}", gid);
            return this;
        }
        if (unicodes == null || unicodes.length == 0) {
            throw new IllegalArgumentException("Unicode 码位序列不能为空");
        }
        Integer[] values = new Integer[unicodes.length];
        for (int i = 0; i < unicodes.length; i++) {
            if (unicodes[i] < 0 || unicodes[i] > 0x10FFFF) {
                logger.warn("忽略非法的 Unicode 码位：0x{}，字形编号 {}", Integer.toHexString(unicodes[i]), gid);
                return this;
            }
            values[i] = unicodes[i];
        }
        this.mappings.put(gid, values);
        return this;
    }

    /**
     * 登记“字符 → 字形编号”映射
     *
     * @param codePoint Unicode 字符
     * @param gid       该字符对应的字形编号
     * @return this
     */
    public ToUnicodeCMap addCodePoint(int codePoint, int gid) {
        return add(gid, codePoint);
    }

    /**
     * 判断是否已登记指定字形编号的映射
     *
     * @param gid 字形编号
     * @return true - 已登记
     */
    public boolean contains(int gid) {
        return mappings.containsKey(gid);
    }

    /**
     * 已登记映射的数量
     *
     * @return 数量，单位为个
     */
    public int size() {
        return mappings.size();
    }

    /**
     * 字形编号到 Unicode 码位的只读映射
     *
     * @return 按字形编号升序排列的只读映射
     */
    public Map<Integer, Integer[]> getMappings() {
        return Collections.unmodifiableMap(mappings);
    }

    /**
     * 生成 ToUnicode CMap 文本
     * <p>
     * 字形编号超过 255 时按 2 字节编码输出，否则按 1 字节编码输出；
     * 连续的“字形 → Unicode”映射压缩为 bfrange，其余输出为 bfchar。
     * 字形宽度不写入 bfrange（由字体字典 /W 决定）。
     *
     * @return CMap 文本（PostScript 语法，使用 ISO-8859-1 兼容的 ASCII 字符集）
     */
    public String toCMapText() {
        /*
         * 码位宽度必须由「字体的编码方式」决定，而不是由「本映射表中最大的字形编号」决定：
         * Type0 复合字体使用 Identity-H 编码时，文本字符串里每个字符恒为 2 字节 CID，
         * 因此 ToUnicode 的 codespacerange 必须声明为 <0000> <FFFF>。
         * 若因映射里恰好只有小于 256 的字形而声明成单字节 <00> <FF>，
         * 阅读器会按 1 字节切分 2 字节 CID，导致取到错位的码位（实测单字符“1”被提取为“婑”）。
         */
        int codeBytes = codeBytesOverride != 0 ? codeBytesOverride
                : (requiresTwoBytes() ? TWO_BYTE : ONE_BYTE);
        StringBuilder out = new StringBuilder(1024);
        out.append("/CIDInit /ProcSet findresource begin\n");
        out.append("12 dict begin\n");
        out.append("begincmap\n");
        out.append("/CIDSystemInfo << /Registry (Adobe) /Ordering (UCS) /Supplement 0 >> def\n");
        out.append("/CMapName /Adobe-Identity-UCS def\n");
        out.append("/CMapType 2 def\n");
        if (codeBytes == TWO_BYTE) {
            out.append("1 begincodespacerange\n<0000> <FFFF>\nendcodespacerange\n");
        } else {
            out.append("1 begincodespacerange\n<00> <FF>\nendcodespacerange\n");
        }

        // 把映射拆分为“连续区间”与“单独条目”，连续区间输出 bfrange、单独条目输出 bfchar
        List<int[]> ranges = new ArrayList<>();
        List<Integer> singles = new ArrayList<>();
        collectRanges(ranges, singles);

        // 单元素区间与单独条目统一按 bfchar 输出，其余区间合并为 bfrange 块
        List<int[]> realRanges = new ArrayList<>();
        Set<Integer> charGids = new LinkedHashSet<>(singles);
        for (int[] range : ranges) {
            if (range[0] == range[1]) {
                charGids.add(range[0]);
            } else {
                realRanges.add(range);
            }
        }
        writeChars(out, new ArrayList<>(charGids), codeBytes);
        writeRanges(out, realRanges, codeBytes);
        out.append("endcmap\n");
        out.append("CMapName currentdict /CMap defineresource pop\n");
        out.append("end\n");
        out.append("end\n");
        return out.toString();
    }

    /**
     * 生成 ToUnicode CMap 的字节内容
     *
     * @return CMap 字节（US-ASCII / ISO-8859-1 编码）
     */
    public byte[] toCMapBytes() {
        return toCMapText().getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * 判断字形编号是否需要 2 字节编码
     *
     * @return true - 存在大于 255 的字形编号
     */
    public boolean requiresTwoBytes() {
        return !mappings.isEmpty() && mappings.lastKey() > 0xFF;
    }

    /**
     * 指定码位字节数，替代按映射表内容推断的结果
     * <p>
     * 使用 Identity-H 编码的 Type0 复合字体必须指定为 {@code 2}：即使映射表中只有小于 256 的字形编号，
     * 也必须声明双字节码空间 {@code <0000> <FFFF>}，否则阅读器会按单字节切分双字节 CID 得到错位码位。
     *
     * @param codeBytes 码位字节数，取 1 或 2；传 0 表示恢复按映射表推断
     * @return this
     * @throws IllegalArgumentException 取值不在 {0, 1, 2} 内
     */
    public ToUnicodeCMap setCodeBytes(int codeBytes) {
        if (codeBytes != 0 && codeBytes != ONE_BYTE && codeBytes != TWO_BYTE) {
            throw new IllegalArgumentException("码位字节数只能是 1 或 2：" + codeBytes);
        }
        this.codeBytesOverride = codeBytes;
        return this;
    }

    /**
     * 把映射拆分为连续区间与单独条目
     * <p>
     * 连续区间条件：字形编号与 Unicode 码位同时递增 1，且两者均映射到单个码位。
     *
     * @param ranges  输出的连续区间，元素为 [起始字形编号, 结束字形编号, 起始 Unicode 码位]
     * @param singles 输出的单独条目（字形编号）
     */
    private void collectRanges(List<int[]> ranges, List<Integer> singles) {
        int rangeStartGid = -1;
        int rangeStartUnicode = -1;
        int prevGid = -1;
        int prevUnicode = -1;
        for (Map.Entry<Integer, Integer[]> entry : mappings.entrySet()) {
            int gid = entry.getKey();
            Integer[] unicode = entry.getValue();
            boolean single = unicode.length == 1;
            boolean continuous = rangeStartGid >= 0 && single && gid == prevGid + 1
                    && unicode[0] == prevUnicode + 1;
            if (continuous) {
                prevGid = gid;
                prevUnicode = unicode[0];
                continue;
            }
            // 结束上一个区间
            if (rangeStartGid >= 0) {
                ranges.add(new int[]{rangeStartGid, prevGid, rangeStartUnicode});
                rangeStartGid = -1;
            }
            if (single) {
                rangeStartGid = gid;
                rangeStartUnicode = unicode[0];
                prevGid = gid;
                prevUnicode = unicode[0];
            } else {
                singles.add(gid);
                prevGid = gid;
                prevUnicode = -1;
            }
        }
        if (rangeStartGid >= 0) {
            ranges.add(new int[]{rangeStartGid, prevGid, rangeStartUnicode});
        }
    }

    /**
     * 输出 bfchar 块
     *
     * @param out      输出缓冲
     * @param singles  单独条目的字形编号
     * @param codeBytes 码位字节数
     */
    private void writeChars(StringBuilder out, List<Integer> singles, int codeBytes) {
        if (singles.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < singles.size(); offset += MAX_BLOCK_SIZE) {
            int end = Math.min(offset + MAX_BLOCK_SIZE, singles.size());
            out.append(end - offset).append(" beginbfchar\n");
            for (int i = offset; i < end; i++) {
                int gid = singles.get(i);
                out.append('<').append(hex(gid, codeBytes)).append("> <");
                for (Integer unicode : mappings.get(gid)) {
                    out.append(utf16Hex(unicode));
                }
                out.append(">\n");
            }
            out.append("endbfchar\n");
        }
    }

    /**
     * 输出 bfrange 块
     *
     * @param out       输出缓冲
     * @param ranges    连续区间，元素为 [起始字形编号, 结束字形编号, 起始 Unicode 码位]
     * @param codeBytes 码位字节数
     */
    private void writeRanges(StringBuilder out, List<int[]> ranges, int codeBytes) {
        if (ranges.isEmpty()) {
            return;
        }
        for (int offset = 0; offset < ranges.size(); offset += MAX_BLOCK_SIZE) {
            int end = Math.min(offset + MAX_BLOCK_SIZE, ranges.size());
            out.append(end - offset).append(" beginbfrange\n");
            for (int i = offset; i < end; i++) {
                int[] range = ranges.get(i);
                // PDF 1.7 9.10.3：bfrange 三元组为 <low> <high> <dst>，不容许附加宽度，
                // 字形宽度由字体字典的 /W 或 /DW 决定；多写的数值会被解析器当作新的目标码位
                out.append('<').append(hex(range[0], codeBytes)).append("> <")
                        .append(hex(range[1], codeBytes)).append("> <")
                        .append(utf16Hex(range[2])).append(">\n");
            }
            out.append("endbfrange\n");
        }
    }

    /**
     * 登记字体 cmap 表中的全部 Unicode 映射
     * <p>
     * 只登记可唯一反查的映射：一个字形对应多个字符时取最小字符编码，保证提取文本稳定。
     *
     * @param font 已解析字体
     */
    private void addAllFromFont(TrueTypeFont font) {
        CmapSubtable[] cmaps = font.getCmaps();
        if (cmaps == null || cmaps.length == 0) {
            logger.warn("字体 {} 没有 cmap 表，无法生成 ToUnicode 映射", font.psName);
            return;
        }
        CmapSubtable unicodeCmap;
        try {
            unicodeCmap = (CmapSubtable) font.getUnicodeCmapLookup();
        } catch (IllegalArgumentException e) {
            logger.warn("字体 {} 没有可用的 Unicode cmap 表，无法生成 ToUnicode 映射", font.psName);
            return;
        }
        // 以字形编号升序登记（TreeMap 保证输出顺序），便于把连续映射压缩为 bfrange
        int registered = 0;
        int numGlyphs = font.getNumGlyphs();
        for (int gid = 1; gid < numGlyphs; gid++) {
            List<Integer> codes = unicodeCmap.getCharCodes(gid);
            if (codes == null || codes.isEmpty()) {
                continue;
            }
            // 一个字形对应多个字符时取最小字符编码，保证文本提取结果稳定
            int code = codes.get(0);
            for (Integer candidate : codes) {
                if (candidate >= 0 && candidate < code) {
                    code = candidate;
                }
            }
            if (code < 0 || code > 0x10FFFF || (code >= 0xD800 && code <= 0xDFFF)) {
                continue;
            }
            mappings.put(gid, new Integer[]{code});
            registered++;
        }
        logger.debug("字体 {} 生成 ToUnicode 映射 {} 条", font.psName, registered);
    }

    /**
     * 输出指定位数的十六进制编码
     *
     * @param value 数值
     * @param bytes 字节数
     * @return 大写十六进制字符串
     */
    private static String hex(int value, int bytes) {
        String format = bytes == ONE_BYTE ? "%02X" : "%04X";
        return String.format(format, value);
    }

    /**
     * 把 Unicode 码位编码为 UTF-16BE 十六进制字符串
     * <p>
     * BMP 内字符编码为 4 位十六进制；BMP 外字符按代理对编码为 8 位十六进制。
     *
     * @param codePoint Unicode 码位
     * @return 大写十六进制字符串
     */
    private static String utf16Hex(int codePoint) {
        StringBuilder out = new StringBuilder(8);
        for (char ch : Character.toChars(codePoint)) {
            out.append(String.format("%04X", (int) ch));
        }
        return out.toString();
    }
}
