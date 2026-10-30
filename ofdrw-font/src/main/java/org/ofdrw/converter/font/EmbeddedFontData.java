package org.ofdrw.converter.font;

import org.ofdrw.converter.font.type1.Type1SegSplitParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 可嵌入 PDF 的字体数据（整字体，不做子集裁剪）
 * <p>
 * 职责：
 * <ul>
 *     <li>识别字体格式（TrueType / CFF / Type1 / 字体集合），并按 PDF 的嵌入要求原样导出数据；</li>
 *     <li>字体集合（TTC）中提取指定字体，重建只含该字体的 sfnt 文件（TTC 头与共享表被剥离）；</li>
 *     <li>给出 PDF FontFile 流的 <em>Length1</em>（未压缩长度）取值。</li>
 * </ul>
 * 设计决策 16：**不做子集裁剪，全部整字体嵌入**——字体数据原样嵌入，因此不会引入字形编号错乱；
 * 代价是字体会整体进入 PDF（体积较大，属已知取舍）。
 * <p>
 * 支持的输入与产出：
 * <table border="1">
 *     <caption>格式映射</caption>
 *     <tr><th>输入</th><th>{@link #getGlyphFormat()}</th><th>{@link #getData()} 内容</th></tr>
 *     <tr><td>TTF / OTF(TrueType 轮廓)</td><td>TRUE_TYPE</td><td>原始 sfnt 数据</td></tr>
 *     <tr><td>OTF(CFF 轮廓)</td><td>CFF</td><td>原始数据（PDF 以 Subtype=OpenType 嵌入）</td></tr>
 *     <tr><td>字体集合 TTC</td><td>TRUE_TYPE 或 CFF</td><td>提取指定字体后重建的 sfnt 数据</td></tr>
 *     <tr><td>Type1(PFB)</td><td>TYPE_1</td><td>原始 PFB 数据（Length1/Length2/Length3 有意义）</td></tr>
 * </table>
 * 裸 CFF 数据没有 sfnt 表目录，PDF 无法直接以 FontFile3 表达，{@link #of(byte[], int)} 对此抛出 {@link IOException}。
 *
 * @see FontDescriptor
 * @since 2.5.1
 */
public class EmbeddedFontData implements FontDescriptor.FontDataProvider {

    private static final Logger logger = LoggerFactory.getLogger(EmbeddedFontData.class);

    /**
     * 字体集合文件头标识：'ttcf'
     */
    private static final int TAG_TTCF = 0x74746366;

    /**
     * sfnt 表目录每条表记录的长度（单位：字节）
     */
    private static final int SFNT_RECORD_LENGTH = 16;

    /**
     * sfnt 表目录中表记录与表数据之间的对齐值（单位：字节）
     */
    private static final int SFNT_ALIGNMENT = 4;

    /**
     * Type1 PFB 段类型：ASCII（明文）段
     */
    private static final int PFB_ASCII = 1;

    /**
     * Type1 PFB 段类型：BINARY（eexec 加密）段
     */
    private static final int PFB_BINARY = 2;

    /**
     * Type1 PFB 段类型：END（结束）段
     */
    private static final int PFB_END = 3;

    /**
     * 字形轮廓格式
     */
    private final FontDescriptor.GlyphFormat glyphFormat;

    /**
     * 可嵌入 PDF 的字体数据
     */
    private final byte[] data;

    /**
     * 数据是否由字体集合（TTC）中的字体重建得到
     */
    private final boolean extractedFromCollection;

    /**
     * 原字体集合内被提取字体的索引，单位：个；非字体集合时为 -1
     */
    private final int collectionIndex;

    /**
     * CFF 字体名（CFF 的 Name INDEX）；非 CFF 字体或读取失败时为 null
     */
    private final String cffFontName;

    /**
     * 构造可嵌入字体数据
     *
     * @param glyphFormat           字形轮廓格式
     * @param data                  可嵌入 PDF 的字体数据
     * @param extractedFromCollection 数据是否由字体集合重建得到
     * @param collectionIndex       原字体集合内索引，单位：个；非字体集合时为 -1
     * @param cffFontName           CFF 字体名，可为 null
     */
    private EmbeddedFontData(FontDescriptor.GlyphFormat glyphFormat, byte[] data,
                             boolean extractedFromCollection, int collectionIndex, String cffFontName) {
        this.glyphFormat = glyphFormat;
        this.data = data;
        this.extractedFromCollection = extractedFromCollection;
        this.collectionIndex = collectionIndex;
        this.cffFontName = cffFontName;
    }

    /**
     * 准备字体文件的可嵌入数据
     * <p>
     * 字体格式按内容识别；字体集合（TTC）按索引提取指定字体并重建 sfnt；
     * 裸 CFF 数据无法直接嵌入 PDF，抛出 {@link IOException} 由调用方改为使用替代字体。
     *
     * @param fontPath 字体文件路径
     * @param ttcIndex 字体集合内索引，单位：个；越界时取最后一个字体；非字体集合时忽略
     * @return 可嵌入字体数据
     * @throws IOException 文件读取失败或字体数据长度不足
     */
    public static EmbeddedFontData of(Path fontPath, int ttcIndex) throws IOException {
        return of(Files.readAllBytes(fontPath), ttcIndex);
    }

    /**
     * 准备字体数据的可嵌入内容
     *
     * @param fontData 字体文件的全部字节（TTF / OTF / TTC / 裸 CFF / Type1）
     * @param ttcIndex 字体集合内索引，单位：个；越界时取最后一个字体；非字体集合时忽略
     * @return 可嵌入字体数据
     * @throws IOException 数据长度不足或字体集合结构损坏
     */
    public static EmbeddedFontData of(byte[] fontData, int ttcIndex) throws IOException {
        if (fontData == null || fontData.length < 4) {
            throw new IOException("字体数据长度不足，无法识别格式");
        }
        FontDescriptor.GlyphFormat format = FontDescriptor.detectFormat(fontData);
        if (format == FontDescriptor.GlyphFormat.TYPE_1) {
            return new EmbeddedFontData(format, fontData.clone(), false, -1, null);
        }
        int tag = FontDescriptor.readS32(fontData, 0);
        if (tag == TAG_TTCF) {
            return extractFromCollection(fontData, ttcIndex);
        }
        if (tag == 0x4F54544F) {
            // OpenType/CFF：原样导出，CFF 字体名从 CFF 表读取
            return new EmbeddedFontData(format, fontData.clone(), false, -1,
                    readCffName(fontData, FontDescriptor.findSfntTable(fontData, 0, "CFF ")));
        }
        if (tag == 0x00010000 || tag == 0x74727565) {
            // TrueType 轮廓（含带 CFF 表的特殊字体，按内容识别结果标注）
            return new EmbeddedFontData(format, fontData.clone(), false, -1, null);
        }
        // 裸 CFF 数据：PDF 无法直接以 FontFile3 表达，明确提示调用方降级
        throw new IOException("裸 CFF 字体数据没有 sfnt 表目录，无法直接嵌入 PDF；请在转换时改用替代字体");
    }

    /**
     * 从字体集合（TTC）中提取指定字体并重建只含该字体的 sfnt 数据
     * <p>
     * 重建规则（OpenType 1.9 §5“Font Collections”）：
     * <ol>
     *     <li>输出 sfnt 版本取被提取字体的表目录版本（0x00010000 或 'OTTO'），不保留 'ttcf' 头；</li>
     *     <li>表记录按原顺序（偏移升序、标签升序）完整保留，签名作为表数据原样复制；</li>
     *     <li>共享表（多个字体共同引用的表）只复制一次，不做表合并与去重改写；</li>
     *     <li>复制后重新计算 checkSumAdjustment：先以 0 写入 head 表的 checkSumAdjustment，
     *     再按 “0xB1B0AFBA - 整个文件 checkSum” 回填。</li>
     * </ol>
     *
     * @param ttcData 字体集合文件的全部字节
     * @param index   字体集合内索引，单位：个；越界时取最后一个字体
     * @return 可嵌入字体数据
     * @throws IOException 字体集合结构损坏
     */
    private static EmbeddedFontData extractFromCollection(byte[] ttcData, int index) throws IOException {
        if (ttcData.length < 16) {
            throw new IOException("字体集合（TTC）文件头长度不足");
        }
        int numFonts = (int) Math.min(Integer.MAX_VALUE, FontDescriptor.readU32(ttcData, 8));
        if (numFonts <= 0) {
            throw new IOException("字体集合（TTC）中没有字体");
        }
        int selected = Math.max(0, Math.min(index, numFonts - 1));
        if (selected != index) {
            logger.warn("字体集合内索引 {} 越界（共 {} 个字体），改用索引 {}", index, numFonts, selected);
        }
        int directoryOffset = (int) FontDescriptor.readU32(ttcData, 12 + selected * 4);
        if (directoryOffset < 0 || directoryOffset + 12 > ttcData.length) {
            throw new IOException("字体集合（TTC）中被选字体的表目录偏移越界：" + directoryOffset);
        }
        int sfntVersion = FontDescriptor.readS32(ttcData, directoryOffset);
        int numTables = FontDescriptor.readU16(ttcData, directoryOffset + 4);

        // 收集表记录：标签、偏移、长度、是否为 head 表
        Map<String, long[]> tables = new LinkedHashMap<>();
        for (int i = 0; i < numTables; i++) {
            int recordOffset = directoryOffset + 12 + i * SFNT_RECORD_LENGTH;
            if (recordOffset + SFNT_RECORD_LENGTH > ttcData.length) {
                throw new IOException("字体集合（TTC）表目录被截断");
            }
            String tag = new String(ttcData, recordOffset, 4, StandardCharsets.ISO_8859_1);
            long tableOffset = FontDescriptor.readU32(ttcData, recordOffset + 8);
            long tableLength = FontDescriptor.readU32(ttcData, recordOffset + 12);
            if (tableOffset < 0 || tableLength < 0 || tableOffset + tableLength > ttcData.length) {
                throw new IOException("字体集合（TTC）中表 " + tag + " 的数据越界");
            }
            tables.put(tag, new long[]{tableOffset, tableLength});
        }

        // 表目录：sfnt 头 + numTables 条记录（searchRange 等按 OpenType 规范计算）
        int headerLength = 12 + numTables * SFNT_RECORD_LENGTH;
        int totalLength = headerLength;
        for (long[] table : tables.values()) {
            totalLength += (int) ((table[1] + SFNT_ALIGNMENT - 1) / SFNT_ALIGNMENT * SFNT_ALIGNMENT);
        }
        byte[] sfnt = new byte[totalLength];
        writeS32(sfnt, 0, sfntVersion);
        int entrySelector = (int) (Math.log(numTables) / Math.log(2));
        writeU16(sfnt, 4, numTables);
        writeU16(sfnt, 6, (1 << entrySelector) * SFNT_RECORD_LENGTH);
        writeU16(sfnt, 8, entrySelector);
        writeU16(sfnt, 10, numTables * SFNT_RECORD_LENGTH - (1 << entrySelector) * SFNT_RECORD_LENGTH);

        int recordOffset = 12;
        int dataOffset = headerLength;
        int headOffsetInFile = -1;
        for (Map.Entry<String, long[]> entry : tables.entrySet()) {
            byte[] tag = entry.getKey().getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(tag, 0, sfnt, recordOffset, 4);
            long tableOffset = entry.getValue()[0];
            int tableLength = (int) entry.getValue()[1];
            System.arraycopy(ttcData, (int) tableOffset, sfnt, dataOffset, tableLength);
            if ("head".equals(entry.getKey())) {
                headOffsetInFile = dataOffset;
                // 复制完成后才能计算校验和，先清零 checkSumAdjustment（head 表内偏移 8）
                writeS32(sfnt, dataOffset + 8, 0);
            }
            long checkSum = calcChecksum(sfnt, dataOffset, tableLength);
            writeU32(sfnt, recordOffset + 4, checkSum);
            writeU32(sfnt, recordOffset + 8, dataOffset);
            writeU32(sfnt, recordOffset + 12, tableLength);
            recordOffset += SFNT_RECORD_LENGTH;
            dataOffset += (tableLength + SFNT_ALIGNMENT - 1) / SFNT_ALIGNMENT * SFNT_ALIGNMENT;
        }
        if (headOffsetInFile >= 0) {
            // checkSumAdjustment = 0xB1B0AFBA - 整个字体文件的校验和
            long fileChecksum = calcChecksum(sfnt, 0, sfnt.length);
            writeU32(sfnt, headOffsetInFile + 8, (0xB1B0AFBAL - fileChecksum) & 0xFFFFFFFFL);
        }

        FontDescriptor.GlyphFormat format = tables.containsKey("CFF ")
                ? FontDescriptor.GlyphFormat.CFF : FontDescriptor.GlyphFormat.TRUE_TYPE;
        String cffName = null;
        if (format == FontDescriptor.GlyphFormat.CFF) {
            cffName = readCffName(sfnt, FontDescriptor.findSfntTable(sfnt, 0, "CFF "));
        }
        logger.debug("已从字体集合（TTC）提取索引 {} 的字体并重建 sfnt，原始大小 {} 字节，重建后 {} 字节",
                selected, ttcData.length, totalLength);
        return new EmbeddedFontData(format, sfnt, true, selected, cffName);
    }

    /**
     * 读取 CFF 的 Name INDEX 中的第一个字体名
     *
     * @param cffContainer 包含 CFF 表的数据
     * @param cffRecord    CFF 表记录 [偏移量, 长度]，单位为字节；不存在时为 null
     * @return 字体名；CFF 表不存在或数据结构异常时返回 null
     */
    private static String readCffName(byte[] cffContainer, long[] cffRecord) {
        if (cffRecord == null) {
            return null;
        }
        int cffOffset = (int) cffRecord[0];
        try {
            // CFF 头：major(1) minor(1) hdrSize(1) offSize(1)，Name INDEX 紧随其后
            int hdrSize = cffContainer[cffOffset + 2] & 0xFF;
            int indexOffset = cffOffset + hdrSize;
            int count = FontDescriptor.readU16(cffContainer, indexOffset);
            if (count == 0) {
                return null;
            }
            int offSize = cffContainer[indexOffset + 2] & 0xFF;
            // offSize 个字节表达的数据偏移（1 起始，指向数据区起点为 1）
            long firstOffset = readCffOffset(cffContainer, indexOffset + 3, offSize);
            long secondOffset = readCffOffset(cffContainer, indexOffset + 3 + offSize, offSize);
            int nameLength = (int) (secondOffset - firstOffset);
            int nameStart = indexOffset + 3 + (count + 1) * offSize + (int) (firstOffset - 1);
            if (nameLength <= 0 || nameStart + nameLength > cffContainer.length) {
                return null;
            }
            return new String(cffContainer, nameStart, nameLength, StandardCharsets.ISO_8859_1);
        } catch (Exception e) {
            logger.debug("读取 CFF 字体名失败", e);
            return null;
        }
    }

    /**
     * 读取 CFF INDEX 的偏移量字段
     *
     * @param data    数据
     * @param pos     起始位置，单位为字节
     * @param offSize 偏移量字节数，取值 1 ~ 4
     * @return 偏移量
     */
    private static long readCffOffset(byte[] data, int pos, int offSize) {
        long value = 0;
        for (int i = 0; i < offSize; i++) {
            value = (value << 8) | (data[pos + i] & 0xFF);
        }
        return value;
    }

    /**
     * 计算 sfnt 数据块的校验和
     * <p>
     * 按 4 字节大端字求和并取低 32 位，末尾不足 4 字节时补 0。
     *
     * @param data   数据
     * @param offset 起始位置，单位为字节
     * @param length 长度，单位为字节
     * @return 校验和
     */
    private static long calcChecksum(byte[] data, int offset, int length) {
        long sum = 0;
        for (int i = 0; i < length; i += 4) {
            long word = 0;
            for (int j = 0; j < 4; j++) {
                int pos = offset + i + j;
                word = (word << 8) | (pos < offset + length ? (data[pos] & 0xFF) : 0);
            }
            sum += word;
        }
        return sum & 0xFFFFFFFFL;
    }

    /**
     * 写入大端无符号 16 位整数
     *
     * @param data  目标数组
     * @param pos   起始位置，单位为字节
     * @param value 取值
     */
    private static void writeU16(byte[] data, int pos, int value) {
        data[pos] = (byte) ((value >>> 8) & 0xFF);
        data[pos + 1] = (byte) (value & 0xFF);
    }

    /**
     * 写入大端无符号 32 位整数
     *
     * @param data  目标数组
     * @param pos   起始位置，单位为字节
     * @param value 取值
     */
    private static void writeU32(byte[] data, int pos, long value) {
        data[pos] = (byte) ((value >>> 24) & 0xFF);
        data[pos + 1] = (byte) ((value >>> 16) & 0xFF);
        data[pos + 2] = (byte) ((value >>> 8) & 0xFF);
        data[pos + 3] = (byte) (value & 0xFF);
    }

    /**
     * 写入大端无符号 32 位整数
     *
     * @param data  目标数组
     * @param pos   起始位置，单位为字节
     * @param value 取值
     */
    private static void writeS32(byte[] data, int pos, int value) {
        writeU32(data, pos, value & 0xFFFFFFFFL);
    }

    /**
     * 字形轮廓格式
     *
     * @return TrueType / CFF / Type1
     */
    public FontDescriptor.GlyphFormat getGlyphFormat() {
        return glyphFormat;
    }

    /**
     * 可嵌入 PDF 的字体数据
     *
     * @return 字体数据副本（每次调用返回新数组，调用方可自由修改）
     */
    @Override
    public byte[] getData() {
        return data.clone();
    }

    /**
     * 字体数据长度
     *
     * @return 长度，单位为字节
     */
    public int getLength() {
        return data.length;
    }

    /**
     * 字体数据的读取流
     *
     * @return 新的读取流，由调用方关闭
     */
    @Override
    public InputStream openStream() {
        return new java.io.ByteArrayInputStream(data);
    }

    /**
     * 数据是否由字体集合（TTC）中的字体重建得到
     *
     * @return true - 已从字体集合提取并重建 sfnt；false - 字体数据原样导出
     */
    public boolean isExtractedFromCollection() {
        return extractedFromCollection;
    }

    /**
     * 原字体集合内被提取字体的索引
     *
     * @return 索引，单位：个；非字体集合时为 -1
     */
    public int getCollectionIndex() {
        return collectionIndex;
    }

    /**
     * CFF 字体名（CFF 的 Name INDEX）
     *
     * @return CFF 字体名；非 CFF 字体或读取失败时为 null
     */
    public String getCffFontName() {
        return cffFontName;
    }

    /**
     * PDF FontFile 流的 Length1 取值（未压缩的字体数据长度）
     * <p>
     * PDF 1.7 9.9：
     * <ul>
     *     <li>FontFile2（TrueType）：Length1 为字体文件的长度；</li>
     *     <li>FontFile3（Subtype=OpenType，嵌入 OTF/TTC 重建数据）：Length1 为 OpenType 文件的长度；</li>
     *     <li>FontFile3（Subtype=Type1C，嵌入 CFF 数据）：Length1 为 CFF 数据的长度；</li>
     *     <li>Type1 原始 PFB：Length1/Length2/Length3 分别对应明文段、加密段与结束段，使用
     *     {@link #getPfbSegmentLengths()} 获取。</li>
     * </ul>
     *
     * @return 长度，单位为字节
     */
    public int getLength1() {
        return data.length;
    }

    /**
     * Type1 PFB 数据各段的长度
     * <p>
     * 非 Type1 数据返回空映射。
     *
     * @return 段类型（1=ASCII 明文段、2=BINARY 加密段、3=END 结束段）到长度的映射，长度单位为字节
     */
    public Map<Integer, Integer> getPfbSegmentLengths() {
        Map<Integer, Integer> lengths = new LinkedHashMap<>();
        if (glyphFormat != FontDescriptor.GlyphFormat.TYPE_1) {
            return lengths;
        }
        int pos = 0;
        while (pos + 2 <= data.length) {
            int segmentType = data[pos] & 0xFF;
            if (segmentType != PFB_ASCII && segmentType != PFB_BINARY && segmentType != PFB_END) {
                break;
            }
            int segmentLength = (data[pos + 1] & 0xFF) | ((data[pos + 2] & 0xFF) << 8);
            lengths.put(segmentType, segmentLength);
            if (segmentType == PFB_END) {
                break;
            }
            pos += 2 + segmentLength;
        }
        return lengths;
    }

    /**
     * 判断字体数据是否为字体集合（TTC）
     *
     * @param data 字体数据
     * @return true - 首 4 字节为 'ttcf'
     */
    public static boolean isCollection(byte[] data) {
        return data != null && data.length >= 4 && FontDescriptor.readS32(data, 0) == TAG_TTCF;
    }

    /**
     * 判断字体数据是否为 Type1 字体
     *
     * @param data 字体数据
     * @return true - Type1 数据
     */
    public static boolean isType1(byte[] data) {
        return data != null && data.length >= 4
                && Type1SegSplitParser.isType1(Arrays.copyOf(data, 4));
    }

    @Override
    public String toString() {
        return "EmbeddedFontData{" + glyphFormat + ", " + data.length + " 字节"
                + (extractedFromCollection ? ", 提取自字体集合索引 " + collectionIndex : "")
                + (cffFontName == null ? "" : ", " + cffFontName) + "}";
    }
}
