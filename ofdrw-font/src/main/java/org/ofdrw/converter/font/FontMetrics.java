package org.ofdrw.converter.font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 统一的字体度量
 * <p>
 * 把 sfnt 的 head / hhea / hmtx / glyf 与 CFF 的 FontMatrix 汇总为一组“1/1000 em”度量，
 * 供 PDF 输出后端写入字体字典、计算文字步进（TJ 位移）与基准线：
 * <ul>
 *     <li>{@link #getUnitsPerEm()}：字体设计单位数；</li>
 *     <li>{@link #getAdvanceWidth(int)}：字形水平宽度，单位为 1/1000 文本空间单位；</li>
 *     <li>{@link #getAscent()} / {@link #getDescent()}：上行 / 下行高度，单位为 1/1000 文本空间单位；</li>
 *     <li>{@link #getBBox()}：全部字形包围盒的并集，单位为 1/1000 文本空间单位。</li>
 * </ul>
 * 换算关系：1/1000 文本空间单位 = 字体设计单位 × 1000 / unitsPerEm。
 * <p>
 * 设计决策 16：本模块整字体嵌入、不做子集裁剪；嵌入后字体的字形编号（GID）保持不变，
 * 因此本类以 GID 表达度量，可直接与文档中的 CGTransform 字形索引对应。
 *
 * @see FontDescriptor
 * @since 2.5.1
 */
public class FontMetrics {

    private static final Logger logger = LoggerFactory.getLogger(FontMetrics.class);

    /**
     * 字体缺少有效 unitsPerEm 时采用的默认值（单位：设计单位/em）
     */
    private static final int DEFAULT_UNITS_PER_EM = 1000;

    /**
     * sfnt 表目录中 head 表的 unitsPerEm 字段偏移（单位：字节）
     */
    private static final int HEAD_UNITS_PER_EM_OFFSET = 18;

    /**
     * 字体度量汇总的字形数量上限（单位：个）
     * <p>
     * 字体文件开头的字形包含拉丁字母与常用符号，已能代表字体主体范围；限制采样数量避免数万字形时开销过大。
     */
    private static final int BBOX_SAMPLE_LIMIT = 4096;

    /**
     * 字体设计单位数（1 em 对应的设计单位）
     */
    private final int unitsPerEm;

    /**
     * 设计单位到 1/1000 文本空间单位的换算比例
     */
    private final float scale;

    /**
     * 上行高度，单位为 1/1000 文本空间单位
     */
    private final float ascent;

    /**
     * 下行高度（负数），单位为 1/1000 文本空间单位
     */
    private final float descent;

    /**
     * 行间距，单位为 1/1000 文本空间单位
     */
    private final float lineGap;

    /**
     * 全部字形包围盒的并集 [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    private final float[] bbox;

    /**
     * 字形宽度表（hmtx），可能为 null（没有 hmtx 表）
     */
    private final HorizontalMetricsTable hmtx;

    /**
     * 字形总数
     */
    private final int numGlyphs;

    /**
     * 由字体文件汇总度量
     * <p>
     * 字体格式按内容识别，字体集合（TTC）按索引提取指定字体后汇总。
     *
     * @param fontPath 字体文件路径
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return 字体度量
     * @throws IOException 文件读取或字体解析失败
     */
    public static FontMetrics of(Path fontPath, int ttcIndex) throws IOException {
        return of(Files.readAllBytes(fontPath), ttcIndex);
    }

    /**
     * 由字体字节数据汇总度量
     *
     * @param fontData 字体文件的全部字节（TTF / OTF / TTC）
     * @param ttcIndex 字体集合内索引，单位：个；非字体集合时忽略
     * @return 字体度量
     * @throws IOException 字体解析失败
     */
    public static FontMetrics of(byte[] fontData, int ttcIndex) throws IOException {
        EmbeddedFontData embedded = EmbeddedFontData.of(fontData, ttcIndex);
        TrueTypeFont font = new TrueTypeFont().parse(embedded.getData());
        return of(font);
    }

    /**
     * 由已解析字体汇总度量
     * <p>
     * 供已完成解析的调用方复用解析结果，避免重复解析字体文件。
     *
     * @param font 已解析的字体
     * @return 字体度量
     */
    public static FontMetrics of(TrueTypeFont font) {
        if (font == null) {
            throw new IllegalArgumentException("字体（font）不能为空");
        }
        return new FontMetrics(font);
    }

    /**
     * 汇总已解析字体的度量
     *
     * @param font 已解析的字体
     */
    private FontMetrics(TrueTypeFont font) {
        TTFDataStream stream = font.getDataStream();
        byte[] raw;
        try {
            raw = stream == null ? null
                    : org.apache.commons.io.IOUtils.toByteArray(stream.getOriginalData());
        } catch (IOException e) {
            logger.warn("读取字体原始数据失败，字体度量按默认值汇总", e);
            raw = null;
        }

        // =========> unitsPerEm
        // 换算比例与 unitsPerEm 互为倒数关系（比例 = 1000 / unitsPerEm），TrueType 与 CFF 均从换算比例反推，
        // 避免两处取值来源不一致（CFF 的 unitsPerEm 由 FontMatrix 决定，head 表未必存在）
        this.scale = FontDescriptor.emScale(font);
        int units = 0;
        if (raw != null && !font.isCffOutline()) {
            long[] head = FontDescriptor.findSfntTable(raw, 0, "head");
            if (head != null && head[0] + HEAD_UNITS_PER_EM_OFFSET + 2 <= raw.length) {
                units = FontDescriptor.readU16(raw, (int) head[0] + HEAD_UNITS_PER_EM_OFFSET);
            }
        }
        if (units <= 0) {
            units = Math.round(1000f / scale);
        }
        this.unitsPerEm = units > 0 ? units : DEFAULT_UNITS_PER_EM;

        // =========> hhea（ascender / descender / lineGap）
        float ascenderUnits = DEFAULT_UNITS_PER_EM * 0.75f;
        float descenderUnits = -DEFAULT_UNITS_PER_EM * 0.25f;
        float lineGapUnits = 0f;
        if (raw != null) {
            long[] hhea = FontDescriptor.findSfntTable(raw, 0, "hhea");
            if (hhea != null && hhea[0] + 8 <= raw.length) {
                try {
                    // hhea 表：version(4) + ascender(2) + descender(2) + lineGap(2)
                    int offset = (int) hhea[0];
                    ascenderUnits = FontDescriptor.readS16(raw, offset + 4);
                    descenderUnits = FontDescriptor.readS16(raw, offset + 6);
                    lineGapUnits = FontDescriptor.readS16(raw, offset + 8);
                } catch (Exception e) {
                    logger.debug("读取 hhea 表失败，上行/下行高度按 3/4 em 与 -1/4 em 估计", e);
                }
            }
        }
        this.ascent = ascenderUnits * scale;
        this.descent = descenderUnits * scale;
        this.lineGap = lineGapUnits * scale;

        // =========> hmtx（字形宽度）
        this.hmtx = readHmtx(font, raw);
        this.numGlyphs = countGlyphs(font, raw);
        this.bbox = computeBBox(font);
    }

    /**
     * 读取 hmtx 字宽表
     *
     * @param font 字体
     * @param raw  字体原始数据，可能为 null
     * @return 字宽表；不存在或解析失败时返回 null
     */
    private static HorizontalMetricsTable readHmtx(TrueTypeFont font, byte[] raw) {
        TTFDataStream stream = font.getDataStream();
        if (raw == null || stream == null) {
            return null;
        }
        long[] hhea = FontDescriptor.findSfntTable(raw, 0, "hhea");
        long[] hmtx = FontDescriptor.findSfntTable(raw, 0, "hmtx");
        if (hhea == null || hmtx == null) {
            return null;
        }
        int numGlyphs = countGlyphs(font, raw);
        try {
            // hhea 表末尾 2 字节为 numberOfHMetrics
            int metricsOffset = (int) hhea[0] + 34;
            if (metricsOffset + 2 > raw.length) {
                return null;
            }
            int numberOfHMetrics = FontDescriptor.readU16(raw, metricsOffset);
            return new HorizontalMetricsTable(hmtx).parse(numberOfHMetrics, numGlyphs, stream);
        } catch (Exception e) {
            logger.warn("解析 hmtx 字宽表失败，字形宽度将按 0 返回", e);
            return null;
        }
    }

    /**
     * 统计字形数量
     *
     * @param font 字体
     * @param raw  字体原始数据，可能为 null
     * @return 字形数量，单位为个；无法确定时返回 -1
     */
    private static int countGlyphs(TrueTypeFont font, byte[] raw) {
        if (raw == null) {
            return -1;
        }
        long[] maxp = FontDescriptor.findSfntTable(raw, 0, "maxp");
        if (maxp == null || maxp[0] + 6 > raw.length) {
            return -1;
        }
        // maxp 表：version(4) + numGlyphs(2)
        return FontDescriptor.readU16(raw, (int) maxp[0] + 4);
    }

    /**
     * 计算全部字形包围盒的并集
     *
     * @param font 字体
     * @return [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    private static float[] computeBBox(TrueTypeFont font) {
        float scale = FontDescriptor.emScale(font);
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
                break;
            }
            org.apache.fontbox.util.BoundingBox box = glyph.getBoundingBox();
            if (box == null || box.getWidth() == 0f && box.getHeight() == 0f) {
                continue;
            }
            if (bbox == null) {
                bbox = new float[]{box.getLowerLeftX(), box.getLowerLeftY(), box.getUpperRightX(), box.getUpperRightY()};
            } else {
                bbox[0] = Math.min(bbox[0], box.getLowerLeftX());
                bbox[1] = Math.min(bbox[1], box.getLowerLeftY());
                bbox[2] = Math.max(bbox[2], box.getUpperRightX());
                bbox[3] = Math.max(bbox[3], box.getUpperRightY());
            }
        }
        if (bbox == null) {
            bbox = new float[]{0f, 0f, 1000f, 1000f};
            return bbox;
        }
        return new float[]{bbox[0] * scale, bbox[1] * scale, bbox[2] * scale, bbox[3] * scale};
    }

    /**
     * 字体设计单位数
     *
     * @return 1 em 对应的设计单位数，例如 1000、2048、256
     */
    public int getUnitsPerEm() {
        return unitsPerEm;
    }

    /**
     * 设计单位到 1/1000 文本空间单位的换算比例
     *
     * @return 换算比例：1/1000 文本空间单位 = 设计单位 × 比例
     */
    public float getScale() {
        return scale;
    }

    /**
     * 字形水平宽度
     * <p>
     * 字形索引非法或字体没有 hmtx 表时返回 0（与 PDF 默认宽度 0 一致），并记录 debug 日志。
     *
     * @param gid 字形索引
     * @return 水平宽度，单位为 1/1000 文本空间单位
     */
    public float getAdvanceWidth(int gid) {
        if (hmtx == null) {
            return 0f;
        }
        if (gid < 0 || (numGlyphs > 0 && gid >= numGlyphs)) {
            logger.debug("字形索引越界，字宽按 0 返回，gid={}", gid);
            return 0f;
        }
        try {
            int width = hmtx.getAdvanceWidth(gid);
            if (width < 0) {
                return 0f;
            }
            return width * scale;
        } catch (Exception e) {
            logger.debug("读取字形宽度失败，gid=" + gid, e);
            return 0f;
        }
    }

    /**
     * 上行高度
     *
     * @return 上行高度，单位为 1/1000 文本空间单位（通常为正数）
     */
    public float getAscent() {
        return ascent;
    }

    /**
     * 下行高度
     *
     * @return 下行高度，单位为 1/1000 文本空间单位（通常为负数）
     */
    public float getDescent() {
        return descent;
    }

    /**
     * 行间距
     *
     * @return 行间距，单位为 1/1000 文本空间单位（可能为 0）
     */
    public float getLineGap() {
        return lineGap;
    }

    /**
     * 全部字形包围盒的并集
     *
     * @return 新数组 [llx, lly, urx, ury]，单位为 1/1000 文本空间单位
     */
    public float[] getBBox() {
        return bbox.clone();
    }

    /**
     * 是否有可用的字宽表
     *
     * @return true - 字体包含 hmtx 表且解析成功
     */
    public boolean hasAdvanceWidth() {
        return hmtx != null;
    }

    @Override
    public String toString() {
        return "FontMetrics{unitsPerEm=" + unitsPerEm + ", ascent=" + ascent + ", descent=" + descent
                + ", bbox=[" + bbox[0] + ", " + bbox[1] + ", " + bbox[2] + ", " + bbox[3] + "]}";
    }
}
