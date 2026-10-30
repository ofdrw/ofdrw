package org.ofdrw.converter.pdf.textobj;

import org.ofdrw.converter.font.EmbeddedFontData;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 字体程序格式识别与 OFD 字体文件扩展名
 * <p>
 * OFD 字体资源要求使用 OpenType 格式（GB/T 33190-2016 11.1），TrueType 轮廓的 sfnt（{@code .ttf}）
 * 是 OpenType 的 TrueType 轮廓分支。本类按字体程序字节内容识别格式，不依赖文件名与 PDF 的 Subtype；
 * 识别只读 sfnt 表目录的标签，不解析表内容。
 *
 * @author 权观宇
 * @since 2.5.1
 */
enum FontFileFormat {

    /**
     * TrueType 轮廓的 sfnt（PDF {@code FontFile2}，或 {@code FontFile3} 中轮廓为 glyf 的 OpenType）
     */
    TrueType(".ttf"),

    /**
     * CFF 轮廓的 OpenType（{@code OTTO}）或裸 CFF（PDF {@code FontFile3} Subtype=Type1C）
     */
    OpenTypeCff(".otf"),

    /**
     * Type1（PFB/PFA）：不是 OpenType，只能原样作为普通资源写入，无法被 OFD 查看器用于文字绘制
     */
    Type1(".pfb"),

    /**
     * 字体集合（TTC）：需先提取指定字体重建 sfnt 才能作为 OFD 字体文件
     */
    Collection(".ttc");

    /**
     * 文件扩展名（含点号），单位：字符
     */
    private final String suffix;

    FontFileFormat(String suffix) {
        this.suffix = suffix;
    }

    /**
     * 文件扩展名
     *
     * @return 扩展名，例如 {@code .ttf}、{@code .otf}
     */
    String getSuffix() {
        return suffix;
    }

    /**
     * 是否为 OpenType 兼容格式（可写入 {@code CT_Font.FontFile} 供查看器解析字形）
     *
     * @return true 表示 TrueType sfnt 或 OpenType/CFF
     */
    boolean isOpenType() {
        return this == TrueType || this == OpenTypeCff;
    }

    /**
     * 按字体程序字节识别格式
     *
     * @param data 字体程序字节
     * @return 字体格式
     * @throws IOException 数据长度不足 4 字节，无法识别
     */
    static FontFileFormat detect(byte[] data) throws IOException {
        if (data == null || data.length < 4) {
            throw new IOException("字体数据长度不足 4 字节，无法识别格式");
        }
        int tag = SfntTable.readS32(data, 0);
        // 'ttcf'：字体集合
        if (tag == 0x74746366) {
            return Collection;
        }
        // 裸 CFF：版本号 0x01000402 / 0x01000403
        if (CffFontData.isBareCff(data)) {
            return OpenTypeCff;
        }
        if (isType1(data)) {
            return Type1;
        }
        // 'OTTO' 为 CFF 轮廓的 OpenType；'true' / 0x00010000 的 sfnt 也可能带 CFF 表
        if (tag == 0x4F54544F || hasTable(data, 0, "CFF ")) {
            return OpenTypeCff;
        }
        // 其余 sfnt（0x00010000 / 'true'）按 TrueType 轮廓处理
        return TrueType;
    }

    /**
     * 把字体集合（TTC）数据提取为只含指定字体的 sfnt
     *
     * @param data     字体数据
     * @param ttcIndex 字体集合内索引，单位为个（从 0 开始）；越界时取最后一个字体
     * @return 重建后的 sfnt 数据，单位为字节；非 TTC 数据时原样返回
     * @throws IOException 字体集合结构损坏
     */
    static byte[] toSfnt(byte[] data, int ttcIndex) throws IOException {
        if (data == null || data.length < 4 || SfntTable.readS32(data, 0) != 0x74746366) {
            return data;
        }
        return EmbeddedFontData.of(data, ttcIndex).getData();
    }

    /**
     * 再次识别 sfnt 数据格式（避免单次识别结果的偶然性），供单元测试交叉校验
     *
     * @param data sfnt 数据
     * @return 字体格式
     * @throws IOException 数据长度不足
     */
    static FontFileFormat toSfntFormatForTest(byte[] data) throws IOException {
        return detect(data);
    }

    /**
     * 判断数据是否为 Type1 字体（PFB 段头 0x80 0x01，或 PFA 的 '%!' 明文头）
     *
     * @param data 字体数据
     * @return true 表示 Type1 数据
     */
    static boolean isType1(byte[] data) {
        if (data == null || data.length < 4) {
            return false;
        }
        return EmbeddedFontData.isType1(data);
    }

    /**
     * 判断 sfnt 数据中是否存在指定表
     *
     * @param data       数据（数据开头即表目录，不含 TTC 头）
     * @param sfntOffset 表目录偏移，单位为字节
     * @param tag        表标签，例如 {@code CFF }（4 个字节，区分大小写）
     * @return true 表示存在
     */
    static boolean hasTable(byte[] data, long sfntOffset, String tag) {
        if (data == null || sfntOffset < 0 || sfntOffset + 12 > data.length) {
            return false;
        }
        int numTables = SfntTable.readU16(data, (int) sfntOffset + 4);
        byte[] wanted = tag.getBytes(StandardCharsets.ISO_8859_1);
        if (wanted.length != 4) {
            return false;
        }
        for (int i = 0; i < numTables; i++) {
            int recordOffset = (int) sfntOffset + 12 + i * 16;
            if (recordOffset + 4 > data.length) {
                return false;
            }
            boolean same = true;
            for (int j = 0; j < 4; j++) {
                if (data[recordOffset + j] != wanted[j]) {
                    same = false;
                    break;
                }
            }
            if (same) {
                return true;
            }
        }
        return false;
    }
}
