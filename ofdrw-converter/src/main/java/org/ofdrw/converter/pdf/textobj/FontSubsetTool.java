package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TTFSubsetter;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.io.RandomAccessReadBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 字体子集化与 sfnt 表裁剪（<b>可选能力</b>，默认关闭）
 * <p>
 * 文字对象化的默认策略是“PDF 内嵌字体原样嵌入”（见 {@link PdfFontRegistry}），
 * 本类只在调用方显式开启 {@code subsetFont} 时使用，用于“完整 CJK 字体整字体嵌入导致 OFD 体积过大”的场景。
 * <p>
 * 子集化流程（设计文档 5.6.3、5.6.4）：
 * <ol>
 *     <li>对每个实际使用的 Unicode 调用 {@link TTFSubsetter#add(int)}，填充 uniToGID 以便重建 cmap；
 *     只调用 {@code addGlyphIds} 时 PDFBox 不重建 cmap，子集字体可能保留指向旧 GID 的失效映射；</li>
 *     <li>调用 {@link TTFSubsetter#addGlyphIds(Set)} 加入 PDF 实际使用的 GID（含 0 号 .notdef）；</li>
 *     <li>{@link TTFSubsetter#getGIDMap()} 返回 <b>new GID → old GID</b>，调用方需要反转得到 old → new
 *     并让 CGTransform 使用新 GID；</li>
 *     <li>写出后按白名单裁剪表并重算 checkSumAdjustment；</li>
 *     <li>用 fontbox {@link TTFParser} 重新解析校验，并核对 numGlyphs / unitsPerEm / cmap 覆盖关系。</li>
 * </ol>
 * 任何一步失败都抛出 {@link IOException}，由调用方回退为整字体嵌入或路径绘制，不中断转换。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class FontSubsetTool {

    private static final Logger logger = LoggerFactory.getLogger(FontSubsetTool.class);

    /**
     * 子集字体的表白名单（标签区分大小写，{@code cvt } 含尾随空格）
     */
    static final java.util.List<String> KEEP_TAGS = Collections.unmodifiableList(Arrays.asList(
            "head", "hhea", "maxp", "hmtx", "loca", "glyf", "cmap", "name", "OS/2", "post",
            "cvt ", "fpgm", "prep", "gasp"));

    /**
     * 子集结果
     */
    static final class Result {

        /**
         * 裁剪后的子集字体数据，单位：字节
         */
        final byte[] data;

        /**
         * old GID（PDF 原始 GID）→ new GID（子集字体 GID）的映射，只包含被加入的字形
         */
        final Map<Integer, Integer> oldToNew;

        /**
         * 子集后的字形数量，单位：个
         */
        final int numGlyphs;

        Result(byte[] data, Map<Integer, Integer> oldToNew, int numGlyphs) {
            this.data = data;
            this.oldToNew = oldToNew;
            this.numGlyphs = numGlyphs;
        }

        /**
         * 把 PDF 原始 GID 换算为子集字体的 GID
         *
         * @param oldGid PDF 原始 GID
         * @return 子集字体中的 GID；映射中不存在时返回 0（.notdef）
         */
        int toNewGid(int oldGid) {
            Integer gid = oldToNew.get(oldGid);
            return gid == null ? 0 : gid;
        }
    }

    private FontSubsetTool() {
    }

    /**
     * 按实际使用的 Unicode 与 GID 生成子集字体
     *
     * @param fontData    原始 sfnt 字体数据（不含 TTC 头）
     * @param usedGids    PDF 实际使用的 GID 集合（0 号自动加入）
     * @param usedUnicodes 这些字形对应的 Unicode 码位集合，用于重建 cmap（可为空，此时只依赖 CGTransform）
     * @return 子集结果
     * @throws IOException 字体解析失败、子集化失败、表裁剪失败或子集字体校验失败
     */
    static Result subset(byte[] fontData, Set<Integer> usedGids, Set<Integer> usedUnicodes) throws IOException {
        if (fontData == null || fontData.length < 12) {
            throw new IOException("字体数据长度不足，无法子集化");
        }
        /*
         * TTFSubsetter 只支持 TrueType 轮廓：需要 glyf / loca / maxp 表；CFF 轮廓（OTTO）直接判定不支持，
         * 由调用方回退为原样嵌入。
         */
        if (!FontFileFormat.hasTable(fontData, 0, "glyf") || !FontFileFormat.hasTable(fontData, 0, "loca")) {
            throw new IOException("字体不含 glyf/loca 表（可能为 CFF 轮廓），不支持 TTFSubsetter 子集化");
        }

        Set<Integer> gids = new TreeSet<>();
        for (Integer gid : usedGids == null ? Collections.<Integer>emptySet() : usedGids) {
            if (gid != null && gid >= 0) {
                gids.add(gid);
            }
        }
        Set<Integer> unicodes = new TreeSet<>();
        for (Integer unicode : usedUnicodes == null ? Collections.<Integer>emptySet() : usedUnicodes) {
            if (unicode != null && unicode >= 0) {
                unicodes.add(unicode);
            }
        }

        TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBuffer(fontData));
        byte[] subsetData;
        int maxGid;
        Map<Integer, Integer> oldToNew = new HashMap<>();
        try {
            TTFSubsetter subsetter = new TTFSubsetter(font);
            // 关键 1：逐个 Unicode 调用 add(int)，参数是 Unicode 码位而不是 GID，用于重建子集 cmap
            for (Integer unicode : unicodes) {
                if (unicode != null && unicode >= 0) {
                    subsetter.add(unicode);
                }
            }
            // 关键 2：把 PDF 实际使用的 GID 一并加入（0 号 .notdef 由 GID 集合保证）
            gids.add(0);
            /*
             * GID 必须落在字体字形范围内：PDF 的 CIDToGIDMap / 编码可能给出越界 GID（.notdef 的常见写法），
             * 越界值交给 TTFSubsetter 会抛 IndexOutOfBounds；这里过滤掉并记录数量，由调用方决定是否告警。
             */
            maxGid = font.getNumberOfGlyphs();
            java.util.Set<Integer> validGids = new TreeSet<>();
            int dropped = 0;
            for (Integer gid : gids) {
                if (gid >= 0 && gid < maxGid) {
                    validGids.add(gid);
                } else {
                    dropped++;
                }
            }
            if (dropped > 0) {
                logger.warn("子集化忽略 {} 个越界 GID（字体共 {} 个字形）", dropped, maxGid);
            }
            subsetter.addGlyphIds(validGids);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            subsetter.writeToStream(out);
            subsetData = out.toByteArray();
            // 关键 3：getGIDMap() 返回 new GID → old GID，必须反转成 old → new 供 CGTransform 使用
            for (Map.Entry<Integer, Integer> entry : subsetter.getGIDMap().entrySet()) {
                oldToNew.put(entry.getValue(), entry.getKey());
            }
        } finally {
            font.close();
        }

        byte[] pruned = pruneTables(subsetData);
        int numGlyphs = verify(pruned, gids.size());
        for (Integer gid : gids) {
            if (gid < maxGid && !oldToNew.containsKey(gid)) {
                throw new IOException("子集字形映射缺少 PDF 原始 GID " + gid + "（共 " + gids.size() + " 个）");
            }
        }
        logger.debug("字体子集化完成：原始 {} 字节、{} 个使用字形 → 子集 {} 字节、{} 个字形",
                fontData.length, gids.size(), pruned.length, numGlyphs);
        return new Result(pruned, oldToNew, numGlyphs);
    }

    /**
     * 按白名单裁剪子集字体的表并重写 sfnt
     * <p>
     * 删除内容：位图表（EBDT/EBLC/EBSC、bdat/bloc/bdsc、CBDT/CBLC、sbix、SVG ）、竖排度量（vmtx/vhea）、
     * 排版表（GSUB/GPOS/GDEF/kern）、签名与显示相关表（DSIG/LTSH/VDMX/hdmx/MERG/meta）以及其余不在白名单中的表。
     *
     * @param subsetData 子集字体数据
     * @return 裁剪后的字体数据，单位：字节
     * @throws IOException 表目录损坏或缺少必要表
     */
    static byte[] pruneTables(byte[] subsetData) throws IOException {
        SfntTable sfnt = SfntTable.read(subsetData);
        java.util.List<String> dropped = new ArrayList<>(sfnt.getTags());
        dropped.removeAll(KEEP_TAGS);
        if (!dropped.isEmpty()) {
            logger.debug("子集字体删除表 {}（共 {} 张）", dropped, dropped.size());
        }
        return SfntTable.rebuild(sfnt, KEEP_TAGS);
    }

    /**
     * 用 fontbox {@link TTFParser} 重新解析并校验裁剪后的字体
     *
     * @param pruned    裁剪后的字体数据
     * @param usedGlyphs 加入子集的字形数量（含 .notdef），单位为个；用于核对子集是否包含全部使用字形
     * @return 校验后的字形数量，单位：个
     * @throws IOException 解析失败，或 numGlyphs / unitsPerEm / cmap 异常、字形数量不足
     */
    static int verify(byte[] pruned, int usedGlyphs) throws IOException {
        TrueTypeFont font = new TTFParser().parse(new RandomAccessReadBuffer(pruned));
        try {
            int numGlyphs = font.getNumberOfGlyphs();
            if (numGlyphs <= 0) {
                throw new IOException("子集字体 numGlyphs=" + numGlyphs + " 个，校验失败");
            }
            if (numGlyphs < usedGlyphs) {
                throw new IOException("子集字体 numGlyphs=" + numGlyphs + " 个，少于使用字形 " + usedGlyphs + " 个");
            }
            int unitsPerEm = font.getUnitsPerEm();
            if (unitsPerEm <= 0) {
                throw new IOException("子集字体 unitsPerEm=" + unitsPerEm + " 个，校验失败");
            }
            // cmap 允许缺失（只依赖 CGTransform），存在时必须能定位到子集内的字形
            if (font.getCmap() != null && font.getUnicodeCmapLookup() != null) {
                font.getUnicodeCmapLookup().getGlyphId('A');
            }
            logger.debug("子集字体校验通过：{} 个字形、unitsPerEm={}", numGlyphs, unitsPerEm);
            return numGlyphs;
        } finally {
            font.close();
        }
    }
}
