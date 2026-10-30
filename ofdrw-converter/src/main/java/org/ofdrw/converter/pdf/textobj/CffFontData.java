package org.ofdrw.converter.pdf.textobj;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 裸 CFF 字体数据包装为 OpenType（sfnt）字体
 * <p>
 * PDF 的 {@code FontFile3 /Subtype /Type1C} 中存放的是裸 CFF 数据，没有 sfnt 表目录，
 * 不符合 GB/T 33190-2016 对 {@code FontFile} “使用 OpenType 格式”的要求。本类给裸 CFF 补一个最小 sfnt 外壳：
 * <table border="1">
 *     <caption>补表规则</caption>
 *     <tr><th>表</th><th>来源</th></tr>
 *     <tr><td>{@code CFF }</td><td>原始裸 CFF 数据</td></tr>
 *     <tr><td>{@code head}</td><td>按 CFF FontBBox 与 FontMatrix 合成，unitsPerEm 取 1000</td></tr>
 *     <tr><td>{@code hhea} / {@code hmtx} / {@code loca} / {@code maxp}</td><td>对应 PDF 字体的宽度与字形数</td></tr>
 *     <tr><td>{@code OS/2}</td><td>最小版本 0 表，用于查看器识别为 OpenType</td></tr>
 * </table>
 * <b>不合成 {@code cmap}</b>：CFF 的字符编码信息在 CFF 内部的 Encoding/Charset 中，
 * 使用方必须通过 CGTransform 显式给出字形索引（文字对象化对 CID 字体正是这么写的）。
 * <p>
 * 说明：本类是“仅用于排版时无内嵌字体”的补充能力（无 cmap 的包装字体只能配合 CGTransform 使用），
 * 文字对象化主路径（PDF 内嵌字体原样嵌入）不经过这里。
 * 合成失败时抛出 {@link IOException}，由 {@link PdfFontRegistry} 回退路径绘制。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class CffFontData {

    /**
     * OpenType 的 sfnt 版本号 'OTTO'
     */
    private static final int TAG_OTTO = 0x4F54544F;

    /**
     * 每 em 的设计单位数，单位：个（OpenType/CFF 常规取值）
     */
    private static final int UNITS_PER_EM = 1000;

    private CffFontData() {
    }

    /**
     * 判断数据是否为裸 CFF（首 4 字节为 CFF 版本号 0x01000402 或 0x01000403）
     *
     * @param data 数据
     * @return true 表示裸 CFF 数据
     */
    static boolean isBareCff(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        int tag = SfntTable.readS32(data, 0);
        return tag == 0x01000402 || tag == 0x01000403;
    }

    /**
     * 把裸 CFF 包装为 OpenType 字体
     *
     * @param cff             裸 CFF 数据
     * @param numGlyphs       字形数量，单位为个（取自 PDF 的 CIDFont / CFF CharStrings）
     * @param widths          每个字形的前进宽度，单位为 1/1000 em，长度应不小于 numGlyphs（不足部分按 0 处理）；
     *                        调用方负责把字体设计单位换算到 1/1000 em
     * @param fontBBox        字体包围盒 [llx, lly, urx, ury]，单位为 1/1000 em；为 null 时取 0
     * @param postScriptName  字体名，写入 name 表的 PostScript 名称记录（nameID=6）
     * @return OpenType（CFF 轮廓）字体数据，单位为字节
     * @throws IOException 裸 CFF 数据过短或无法合成
     */
    static byte[] wrap(byte[] cff, int numGlyphs, int[] widths, float[] fontBBox, String postScriptName)
            throws IOException {
        if (!isBareCff(cff)) {
            throw new IOException("不是裸 CFF 数据（首 4 字节应为 0x01000402 / 0x01000403），无法包装为 OpenType");
        }
        if (numGlyphs <= 0) {
            throw new IOException("字形数量 " + numGlyphs + " 个不合法，无法包装裸 CFF 为 OpenType");
        }
        // loca 为占位表：CFF 轮廓不使用 glyf/loca，但部分查看器要求两者存在其一，这里提供全 0 索引
        byte[] head = buildHead(fontBBox);
        byte[] hhea = buildHhea(numGlyphs, widths);
        byte[] maxp = buildMaxp(numGlyphs);
        byte[] hmtx = buildHmtx(numGlyphs, widths);
        byte[] loca = new byte[(numGlyphs + 1) * 4];
        byte[] os2 = buildOS2();
        byte[] name = buildName(postScriptName);
        java.util.Map<String, byte[]> tableMap = new java.util.TreeMap<>();
        tableMap.put("CFF ", cff);
        tableMap.put("OS/2", os2);
        tableMap.put("head", head);
        tableMap.put("hhea", hhea);
        tableMap.put("hmtx", hmtx);
        tableMap.put("loca", loca);
        tableMap.put("maxp", maxp);
        tableMap.put("name", name);
        // 写出表目录后回读一次：重算每张表的 checkSum 与 head.checkSumAdjustment
        String[] keep = {"CFF ", "OS/2", "head", "hhea", "hmtx", "loca", "maxp", "name"};
        return SfntTable.rebuild(SfntTable.read(writeSfnt(tableMap)), java.util.Arrays.asList(keep));
    }

    /**
     * 构造 head 表（54 字节）
     *
     * @param fontBBox 字体包围盒，单位为 1/1000 em；为 null 时取 0
     * @return head 表数据
     */
    private static byte[] buildHead(float[] fontBBox) {
        byte[] head = new byte[54];
        writeU32(head, 0, 0x00010000L);          // version 1.0
        writeU32(head, 4, 0x00010000L);          // fontRevision 1.0
        writeU32(head, 8, 0);                    // checkSumAdjustment（rebuild 时重算）
        writeU32(head, 12, 0x5F0F3CF5L);         // magicNumber
        writeU16(head, 16, 0x0000);              // flags
        writeU16(head, 18, UNITS_PER_EM);        // unitsPerEm
        writeU32(head, 20, 0);                   // created（长整型时间，0 表示未记录）
        writeU32(head, 24, 0);                   // modified
        int[] box = fontBBox == null || fontBBox.length < 4
                ? new int[]{0, 0, 1000, 1000}
                : new int[]{Math.round(fontBBox[0]), Math.round(fontBBox[1]),
                Math.round(fontBBox[2]), Math.round(fontBBox[3])};
        writeS16(head, 36, box[0]);
        writeS16(head, 38, box[1]);
        writeS16(head, 40, box[2]);
        writeS16(head, 42, box[3]);
        writeU16(head, 44, 0);                   // macStyle
        writeU16(head, 46, 8);                   // lowestRecPPEM
        writeS16(head, 48, 2);                   // fontDirectionHint
        writeS16(head, 50, 0);                   // indexToLocFormat：CFF 不使用 loca，取短格式
        writeS16(head, 52, 0);                   // glyphDataFormat
        return head;
    }

    /**
     * 构造 hhea 表（36 字节）
     *
     * @param numGlyphs 字形数量，单位为个
     * @param widths    字形宽度，单位为 1/1000 em
     * @return hhea 表数据
     */
    private static byte[] buildHhea(int numGlyphs, int[] widths) {
        byte[] hhea = new byte[36];
        writeU32(hhea, 0, 0x00010000L);          // version 1.0
        writeS16(hhea, 4, 800);                  // ascender
        writeS16(hhea, 6, -200);                 // descender
        writeS16(hhea, 8, 0);                    // lineGap
        int maxAdvance = 0;
        for (int i = 0; i < numGlyphs && widths != null && i < widths.length; i++) {
            maxAdvance = Math.max(maxAdvance, widths[i]);
        }
        writeU16(hhea, 10, Math.max(0, Math.min(0xFFFF, maxAdvance)));
        writeS16(hhea, 12, 0);                   // minLeftSideBearing
        writeS16(hhea, 14, 0);                   // minRightSideBearing
        writeS16(hhea, 16, 0);                   // xMaxExtent
        writeS16(hhea, 18, 1);                   // caretSlopeRise
        writeS16(hhea, 20, 0);                   // caretSlopeRun
        writeS16(hhea, 22, 0);                   // caretOffset
        writeU16(hhea, 32, 0);                   // metricDataFormat
        writeU16(hhea, 34, numGlyphs);           // numberOfHMetrics
        return hhea;
    }

    /**
     * 构造 hmtx 表
     *
     * @param numGlyphs 字形数量，单位为个
     * @param widths    字形宽度，单位为 1/1000 em
     * @return hmtx 表数据
     */
    private static byte[] buildHmtx(int numGlyphs, int[] widths) {
        byte[] hmtx = new byte[numGlyphs * 4];
        for (int gid = 0; gid < numGlyphs; gid++) {
            int advance = widths != null && gid < widths.length ? Math.max(0, Math.min(0xFFFF, widths[gid])) : 0;
            writeU16(hmtx, gid * 4, advance);
            writeS16(hmtx, gid * 4 + 2, 0);      // leftSideBearing
        }
        return hmtx;
    }

    /**
     * 构造 maxp 表（版本 0.5，32 字节）
     *
     * @param numGlyphs 字形数量，单位为个
     * @return maxp 表数据
     */
    private static byte[] buildMaxp(int numGlyphs) {
        byte[] maxp = new byte[32];
        writeU32(maxp, 0, 0x00005000L);          // version 0.5（CFF 轮廓）
        writeU16(maxp, 4, numGlyphs);
        writeU16(maxp, 6, 0);
        writeU16(maxp, 8, 0);
        writeU16(maxp, 10, 0);
        writeU16(maxp, 12, 0);
        writeU16(maxp, 14, 0);
        writeU16(maxp, 16, 0);
        writeU16(maxp, 18, 0);
        writeU16(maxp, 20, 0);
        writeU16(maxp, 22, 0);
        writeU16(maxp, 24, 0);
        writeU16(maxp, 26, 0);
        writeU16(maxp, 28, 0);
        writeU16(maxp, 30, 1);
        return maxp;
    }

    /**
     * 构造最小 OS/2 表（版本 0，78 字节）
     *
     * @return OS/2 表数据
     */
    private static byte[] buildOS2() {
        byte[] os2 = new byte[78];
        writeU16(os2, 0, 0);                     // version 0
        writeS16(os2, 2, 500);                   // xAvgCharWidth
        writeU16(os2, 4, 400);                   // usWeightClass
        writeU16(os2, 6, 5);                     // usWidthClass
        writeU16(os2, 8, 0);                     // fsType：0 表示无授权限制标记
        // 其余字段（字体度量、字符区间）保持 0，排版信息由字体软件按实际字形使用
        return os2;
    }

    /**
     * 构造最小 name 表（只写一个 PostScript 名记录）
     *
     * @param postScriptName 字体名，单位：字符；为 null 时使用 "CFFFont"
     * @return name 表数据
     */
    private static byte[] buildName(String postScriptName) {
        String name = postScriptName == null || postScriptName.isEmpty() ? "CFFFont" : postScriptName;
        byte[] utf16 = name.getBytes(StandardCharsets.UTF_16BE);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        // name 表头：format(2) count(2) stringOffset(2)
        writeU16(out, 0);
        writeU16(out, 1);
        writeU16(out, 6 + 12);
        // 名称记录：platformID(2) encodingID(2) languageID(2) nameID(2) length(2) offset(2)
        writeU16(out, 3);          // platformID：Windows
        writeU16(out, 1);          // encodingID：Unicode BMP
        writeU16(out, 0x0409);     // languageID：英文（美国）
        writeU16(out, 6);          // nameID：PostScript 名
        writeU16(out, utf16.length);
        writeU16(out, 0);
        out.write(utf16, 0, utf16.length);
        return out.toByteArray();
    }

    /**
     * 写出带表目录的 sfnt（表数据 4 字节对齐）
     *
     * @param tables 表标签到表数据的映射（按标签升序迭代）
     * @return sfnt 数据，单位为字节
     */
    private static byte[] writeSfnt(java.util.Map<String, byte[]> tables) {
        int numTables = tables.size();
        int headerLength = 12 + numTables * 16;
        int totalLength = headerLength;
        for (byte[] table : tables.values()) {
            totalLength += (table.length + 3) & ~3;
        }
        byte[] sfnt = new byte[totalLength];
        writeU32(sfnt, 0, TAG_OTTO);
        writeU16(sfnt, 4, numTables);
        writeU16(sfnt, 6, 0);
        writeU16(sfnt, 8, 0);
        writeU16(sfnt, 10, 0);
        int recordOffset = 12;
        int dataOffset = headerLength;
        for (java.util.Map.Entry<String, byte[]> entry : tables.entrySet()) {
            byte[] tag = entry.getKey().getBytes(StandardCharsets.ISO_8859_1);
            System.arraycopy(tag, 0, sfnt, recordOffset, 4);
            byte[] table = entry.getValue();
            System.arraycopy(table, 0, sfnt, dataOffset, table.length);
            writeU32(sfnt, recordOffset + 8, dataOffset);
            writeU32(sfnt, recordOffset + 12, table.length);
            recordOffset += 16;
            dataOffset += (table.length + 3) & ~3;
        }
        return sfnt;
    }

    /**
     * 写入大端无符号 16 位整数
     *
     * @param out   输出流
     * @param value 取值
     */
    private static void writeU16(ByteArrayOutputStream out, int value) {
        out.write((value >>> 8) & 0xFF);
        out.write(value & 0xFF);
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
     * 写入大端有符号 16 位整数
     *
     * @param data  目标数组
     * @param pos   起始位置，单位为字节
     * @param value 取值
     */
    private static void writeS16(byte[] data, int pos, int value) {
        writeU16(data, pos, value & 0xFFFF);
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
}
