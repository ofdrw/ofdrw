package org.ofdrw.converter.pdf.textobj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * sfnt（TrueType / OpenType）表目录读写工具
 * <p>
 * 只处理“表目录 + 表数据”这一层，不解析任何具体表的内容；用途有两个：
 * <ul>
 *     <li>{@link #read(byte[])}：读取 sfnt 的表目录，取得 {@code head.unitsPerEm}、{@code maxp.numGlyphs} 与表标签集合；</li>
 *     <li>{@link #rebuild(byte[], List)}：按白名单重写 sfnt（字体子集裁剪路径使用）。</li>
 * </ul>
 * 字体集合（TTC，首 4 字节 {@code 'ttcf'}）不是单个 sfnt，{@link #read(byte[])} 抛出 {@link IOException}，
 * 由调用方先用 {@link FontFileFormat#toSfnt(byte[], int)} 提取后再读。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class SfntTable {

    /**
     * sfnt 表目录每条表记录的长度，单位：字节
     */
    private static final int RECORD_LENGTH = 16;

    /**
     * 表数恒为 0 时用于计算 entrySelector 的兜底值
     */
    private static final int MIN_ENTRY_SELECTOR = 4;

    /**
     * 整个字体文件校验和的魔常数（OpenType head 表 checkSumAdjustment 定义）
     */
    private static final long CHECKSUM_MAGIC = 0xB1B0AFBAL;

    /**
     * 表标签到表数据的映射（按表标签字典序排列，便于稳定输出）
     */
    private final Map<String, byte[]> tables;

    /**
     * sfnt 版本号（0x00010000 / 'true' / 'OTTO'）
     */
    private final int sfntVersion;

    private SfntTable(int sfntVersion, Map<String, byte[]> tables) {
        this.sfntVersion = sfntVersion;
        this.tables = tables;
    }

    /**
     * 读取 sfnt 的表目录与表数据
     *
     * @param data sfnt 数据（不含 TTC 头，首 4 字节为 sfnt 版本）
     * @return 表标签到表数据的只读映射（表数据为副本）
     * @throws IOException 数据长度不足、表目录越界或 TTC 数据
     */
    static SfntTable read(byte[] data) throws IOException {
        if (data == null || data.length < 12) {
            throw new IOException("sfnt 数据长度不足 12 字节，无法读取表目录");
        }
        int sfntVersion = readS32(data, 0);
        if (sfntVersion == 0x74746366) {
            throw new IOException("字体集合（TTC）不是单个 sfnt，需先提取后再读取表目录");
        }
        int numTables = readU16(data, 4);
        if (numTables <= 0 || 12 + numTables * RECORD_LENGTH > data.length) {
            throw new IOException("sfnt 表目录被截断：表数量 " + numTables + " 个");
        }
        Map<String, byte[]> tables = new LinkedHashMap<>();
        for (int i = 0; i < numTables; i++) {
            int recordOffset = 12 + i * RECORD_LENGTH;
            String tag = new String(data, recordOffset, 4, StandardCharsets.ISO_8859_1);
            long tableOffset = readU32(data, recordOffset + 8);
            long tableLength = readU32(data, recordOffset + 12);
            if (tableOffset < 0 || tableLength < 0 || tableOffset + tableLength > data.length) {
                throw new IOException("sfnt 表 " + tag + " 的数据越界：" + tableOffset + " + " + tableLength
                        + " > " + data.length + " 字节");
            }
            tables.put(tag, Arrays.copyOfRange(data, (int) tableOffset, (int) (tableOffset + tableLength)));
        }
        return new SfntTable(sfntVersion, Collections.unmodifiableMap(tables));
    }

    /**
     * 按白名单重写 sfnt
     * <p>
     * 处理内容：
     * <ol>
     *     <li>只保留 {@code keepTags} 中出现的表（标签区分大小写，{@code OS/2}、{@code cvt } 含空格）；</li>
     *     <li>表记录按标签升序排列，表数据 4 字节对齐，重算每张表的 checkSum；</li>
     *     <li>把 {@code head.checkSumAdjustment}（表内偏移 8 字节处）清零后计算全体校验和，
     *     回填为 {@code 0xB1B0AFBA - 全体校验和}。</li>
     * </ol>
     *
     * @param original 原始 sfnt 表
     * @param keepTags 保留的表标签列表
     * @return 重写后的 sfnt 数据，单位为字节
     * @throws IOException 原始字体缺少必要表或表数据损坏
     */
    static byte[] rebuild(SfntTable original, List<String> keepTags) throws IOException {
        if (!original.tables.containsKey("head")) {
            throw new IOException("字体缺少 head 表，无法重写 sfnt");
        }
        Map<String, byte[]> kept = new java.util.TreeMap<>();
        for (String tag : keepTags) {
            byte[] table = original.tables.get(tag);
            if (table != null) {
                kept.put(tag, table.clone());
            }
        }
        for (String required : new String[]{"head", "hhea", "hmtx", "maxp"}) {
            if (!kept.containsKey(required)) {
                throw new IOException("字体缺少必要表 " + required + "，无法重写 sfnt");
            }
        }
        // 轮廓数据：TrueType 为 glyf+loca，CFF 为 CFF 表
        boolean trueTypeOutline = kept.containsKey("glyf") && kept.containsKey("loca");
        if (!trueTypeOutline && !kept.containsKey("CFF ")) {
            throw new IOException("字体既无 glyf/loca 也无 CFF 表，无法重写 sfnt");
        }
        if (trueTypeOutline && !kept.containsKey("cmap")) {
            throw new IOException("TrueType 字体缺少 cmap 表，无法重写 sfnt");
        }

        int numTables = kept.size();
        int entrySelector = Math.max(MIN_ENTRY_SELECTOR, 31 - Integer.numberOfLeadingZeros(numTables));
        int searchRange = (1 << entrySelector) * RECORD_LENGTH;
        int rangeShift = numTables * RECORD_LENGTH - searchRange;

        int headerLength = 12 + numTables * RECORD_LENGTH;
        int totalLength = headerLength;
        for (byte[] table : kept.values()) {
            totalLength += align4(table.length);
        }
        byte[] sfnt = new byte[totalLength];
        writeS32(sfnt, 0, original.sfntVersion);
        writeU16(sfnt, 4, numTables);
        writeU16(sfnt, 6, searchRange);
        writeU16(sfnt, 8, entrySelector);
        writeU16(sfnt, 10, rangeShift);

        int recordOffset = 12;
        int dataOffset = headerLength;
        int headOffset = -1;
        for (Map.Entry<String, byte[]> entry : kept.entrySet()) {
            String tag = entry.getKey();
            byte[] table = entry.getValue();
            System.arraycopy(tag.getBytes(StandardCharsets.ISO_8859_1), 0, sfnt, recordOffset, 4);
            System.arraycopy(table, 0, sfnt, dataOffset, table.length);
            if ("head".equals(tag)) {
                headOffset = dataOffset;
                // head 表的 checkSumAdjustment 位于表内偏移 8 字节，先清零再整体计算校验和
                writeU32(sfnt, dataOffset + 8, 0);
            }
            writeU32(sfnt, recordOffset + 4, checksum(sfnt, dataOffset, table.length));
            writeU32(sfnt, recordOffset + 8, dataOffset);
            writeU32(sfnt, recordOffset + 12, table.length);
            recordOffset += RECORD_LENGTH;
            dataOffset += align4(table.length);
        }
        long fileChecksum = checksum(sfnt, 0, sfnt.length);
        writeU32(sfnt, headOffset + 8, (CHECKSUM_MAGIC - fileChecksum) & 0xFFFFFFFFL);
        return sfnt;
    }

    /**
     * 生成表的文本列表，按标签升序
     *
     * @return 表标签列表
     */
    List<String> getTags() {
        List<String> tags = new ArrayList<>(tables.keySet());
        Collections.sort(tags);
        return tags;
    }

    /**
     * 获取指定表的数据
     *
     * @param tag 表标签
     * @return 表数据；表不存在时返回 null
     */
    byte[] getTable(String tag) {
        return tables.get(tag);
    }

    /**
     * 读取 head.unitsPerEm（每 em 的字体设计单位数）
     *
     * @return 每 em 的设计单位数，例如 1000、2048、256；head 表缺失时返回 1000
     */
    int getUnitsPerEm() {
        byte[] head = tables.get("head");
        if (head == null || head.length < 20) {
            return 1000;
        }
        int unitsPerEm = readU16(head, 18);
        return unitsPerEm > 0 ? unitsPerEm : 1000;
    }

    /**
     * 读取 maxp.numGlyphs（字形总数）
     *
     * @return 字形总数，单位为个；maxp 表缺失时返回 0
     */
    int getNumGlyphs() {
        byte[] maxp = tables.get("maxp");
        if (maxp == null || maxp.length < 6) {
            return 0;
        }
        return readU16(maxp, 4);
    }

    /**
     * 计算数据块校验和（按 4 字节大端无符号字求和后取低 32 位，末尾补 0）
     *
     * @param data   数据
     * @param offset 起始位置，单位为字节
     * @param length 长度，单位为字节
     * @return 校验和
     */
    private static long checksum(byte[] data, int offset, int length) {
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
     * 把长度向上对齐到 4 字节边界
     *
     * @param length 原始长度，单位为字节
     * @return 对齐后的长度，单位为字节
     */
    private static int align4(int length) {
        return (length + 3) & ~3;
    }

    /**
     * 读取大端无符号 16 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 取值
     */
    static int readU16(byte[] data, int pos) {
        return ((data[pos] & 0xFF) << 8) | (data[pos + 1] & 0xFF);
    }

    /**
     * 读取大端无符号 32 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 取值
     */
    static long readU32(byte[] data, int pos) {
        return ((long) (data[pos] & 0xFF) << 24) | ((data[pos + 1] & 0xFF) << 16)
                | ((data[pos + 2] & 0xFF) << 8) | (data[pos + 3] & 0xFF);
    }

    /**
     * 读取大端有符号 32 位整数
     *
     * @param data 数据
     * @param pos  起始位置，单位为字节
     * @return 取值
     */
    static int readS32(byte[] data, int pos) {
        return (int) readU32(data, pos);
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
     * 写入大端有符号 32 位整数
     *
     * @param data  目标数组
     * @param pos   起始位置，单位为字节
     * @param value 取值
     */
    private static void writeS32(byte[] data, int pos, int value) {
        writeU32(data, pos, value & 0xFFFFFFFFL);
    }

    /**
     * 拼接字节数组
     *
     * @param parts 待拼接的字节数组
     * @return 拼接结果，单位为字节
     */
    static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            if (part != null) {
                out.write(part, 0, part.length);
            }
        }
        return out.toByteArray();
    }
}
