package org.ofdrw.converter.font;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一的“字符 → 字形”访问入口（OFD 文字转换的字形选择策略）
 * <p>
 * OFD 文字通过 <em>CGTransform</em>（GB/T 33190-2016 11.4）描述字符与字形的对应关系，
 * 支持一对一、多对一、一对多与多对多；PDF 没有等价结构，只能把字符写成字形编号（CID）。
 * 本类按下述顺序确定字形编号（设计决策 3“字形优先 + ToUnicode”）：
 * <ol>
 *     <li><strong>显式字形编号</strong>：CGTransform 给出 GID 时直接采用（视觉一致优先）；</li>
 *     <li><strong>cmap 查表</strong>：没有显式 GID 时按 Unicode 查字体的 cmap 表；</li>
 *     <li><strong>回退字形 0</strong>：前两者都失败时使用字形 0（.notdef），并把 {@link GlyphSelection#isSubstituted()}
 *     标记为 true、记录 warning 日志（设计决策 7/8：降级不阻断）。</li>
 * </ol>
 * 多字符映射到同一字形（多对一）时保留首字符的位置信息，其余字符的步进记 0；
 * 一个字符映射到多个字形（一对多）时，首个字形挂原字符，后续字形挂空（由 ToUnicode 的 bfchar 表达多码位）。
 *
 * @since 2.5.1
 */
public class GlyphIndexProvider {

    private static final Logger logger = LoggerFactory.getLogger(GlyphIndexProvider.class);

    /**
     * 回退字形编号（.notdef）
     */
    public static final int NOTDEF_GLYPH = 0;

    /**
     * 字体
     */
    private final TrueTypeFont font;

    /**
     * Unicode cmap 查表；字体没有可用 cmap 表时为 null
     */
    private final CmapSubtable unicodeCmap;

    /**
     * 字体字形数量；无法确定时为 -1（不做越界校验）
     */
    private final int numGlyphs;

    /**
     * 显式字形编号的越界处理方式：true - 越界时回退 cmap 查表；false - 越界时直接回退字形 0
     */
    private boolean fallbackOnExplicitOutOfRange = true;

    /**
     * 创建字形索引提供器
     *
     * @param font 已解析的字体
     */
    public GlyphIndexProvider(TrueTypeFont font) {
        if (font == null) {
            throw new IllegalArgumentException("字体（font）不能为空");
        }
        this.font = font;
        this.numGlyphs = font.getNumGlyphs();
        CmapSubtable cmap;
        try {
            cmap = (CmapSubtable) font.getUnicodeCmapLookup();
        } catch (IllegalArgumentException e) {
            logger.warn("字体 {} 没有可用的 Unicode cmap 表，字符到字形的映射只能依赖显式字形编号",
                    font.psName == null ? "UnknownFont" : font.psName);
            cmap = null;
        }
        this.unicodeCmap = cmap;
    }

    /**
     * 设置显式字形编号越界时的处理方式
     *
     * @param fallbackOnExplicitOutOfRange true - 越界时回退 cmap 查表（默认）；false - 直接回退字形 0
     * @return this
     */
    public GlyphIndexProvider fallbackOnExplicitOutOfRange(boolean fallbackOnExplicitOutOfRange) {
        this.fallbackOnExplicitOutOfRange = fallbackOnExplicitOutOfRange;
        return this;
    }

    /**
     * 解析单个字符对应的字形编号
     * <p>
     * 解析顺序：显式字形编号 → cmap 查表 → 回退字形 0。
     *
     * @param explicitGid CGTransform 给出的显式字形编号；没有显式字形编号时传负值（如 -1）
     * @param codePoint   字符的 Unicode 码位
     * @return 字形选择结果，至少包含一个字形；回退字形 0 时 {@link GlyphSelection#isSubstituted()} 为 true
     */
    public GlyphSelection resolve(int explicitGid, int codePoint) {
        GlyphSelection selection = new GlyphSelection(1);
        // =========> 1. 显式字形编号优先
        if (explicitGid >= 0) {
            if (isValidGid(explicitGid)) {
                selection.add(explicitGid, 0);
                return selection;
            }
            selection.markSubstituted();
            if (!fallbackOnExplicitOutOfRange) {
                logger.warn("显式字形编号 {} 超出字体 {} 的字形范围，回退字形 0", explicitGid, fontName());
                selection.add(NOTDEF_GLYPH, 0);
                return selection;
            }
            logger.warn("显式字形编号 {} 超出字体 {} 的字形范围，回退 cmap 查表", explicitGid, fontName());
        }
        // =========> 2. cmap 查表
        if (unicodeCmap != null) {
            int gid = lookup(codePoint);
            if (gid != NOTDEF_GLYPH && isValidGid(gid)) {
                selection.add(gid, 0);
                return selection;
            }
        }
        // =========> 3. 回退字形 0
        logger.warn("字体 {} 中没有码位 U+{} 对应的字形，回退字形 0",
                fontName(), String.format("%04X", codePoint));
        selection.add(NOTDEF_GLYPH, 0);
        selection.markSubstituted();
        return selection;
    }

    /**
     * 解析一组字符对应的字形序列（CGTransform 的一般形式）
     * <p>
     * 输出字形序列（outputGlyphIds）给出时按该序列输出，不再查 cmap：
     * 多对一时只有首个字形携带字符位置，一对多时多余的字形无对应字符；
     * 未给出时逐个字符按 {@link #resolve(int, int)} 解析（cmap → 回退字形 0）。
     *
     * @param codePoints     字符的 Unicode 码位序列
     * @param outputGlyphIds CGTransform 输出的字形编号序列；没有时传 null
     * @return 字形选择结果
     */
    public GlyphSelection select(int[] codePoints, int[] outputGlyphIds) {
        if (codePoints == null || codePoints.length == 0) {
            throw new IllegalArgumentException("字符序列（codePoints）不能为空");
        }
        if (outputGlyphIds == null || outputGlyphIds.length == 0) {
            // 无显式字形序列：逐个字符解析
            GlyphSelection selection = new GlyphSelection(codePoints.length);
            for (int codePoint : codePoints) {
                GlyphSelection single = resolve(-1, codePoint);
                for (int i = 0; i < single.glyphCount(); i++) {
                    selection.add(single.getGlyphId(i), i == 0 ? 0 : -1);
                    if (single.isSubstituted()) {
                        selection.markSubstituted();
                    }
                }
            }
            return selection;
        }
        // 显式给出输出字形序列：多对一 / 一对多 / 多对多
        GlyphSelection selection = new GlyphSelection(codePoints.length);
        // 多对一时：每个字符都对应同一个字形，此时按 1 个字符位置承载全部字符（其余字符无独立字形）
        boolean manyToOne = outputGlyphIds.length < codePoints.length;
        for (int i = 0; i < outputGlyphIds.length; i++) {
            int gid = outputGlyphIds[i];
            // textPosition：前 codePoints 个输出字形依次对应各字符，多余字形（一对多）无对应字符
            int textPosition = manyToOne ? (i == 0 ? 0 : -1) : (i < codePoints.length ? 0 : -1);
            if (isValidGid(gid)) {
                selection.add(gid, textPosition);
                continue;
            }
            logger.warn("CGTransform 输出的字形编号 {} 超出字体 {} 的字形范围，回退字形 0", gid, fontName());
            selection.add(NOTDEF_GLYPH, textPosition);
            selection.markSubstituted();
        }
        return selection;
    }

    /**
     * 按 Unicode 字符查 cmap 表
     *
     * @param codePoint Unicode 码位
     * @return 字形编号；查不到时返回 {@link #NOTDEF_GLYPH}
     */
    private int lookup(int codePoint) {
        if (codePoint < 0 || codePoint > 0x10FFFF) {
            return NOTDEF_GLYPH;
        }
        try {
            return unicodeCmap.getGlyphId(codePoint);
        } catch (Exception e) {
            logger.warn("cmap 查表失败，码位 0x" + Integer.toHexString(codePoint), e);
            return NOTDEF_GLYPH;
        }
    }

    /**
     * 判断字形编号是否在字体范围内
     *
     * @param gid 字形编号
     * @return true - 合法
     */
    private boolean isValidGid(int gid) {
        if (gid < 0) {
            return false;
        }
        return numGlyphs <= 0 || gid < numGlyphs;
    }

    /**
     * 字体名称
     *
     * @return PostScript 名；缺失时返回 UnknownFont
     */
    private String fontName() {
        return font.psName == null ? "UnknownFont" : font.psName;
    }

    /**
     * 是否有可用的 Unicode cmap 表
     * <p>
     * OFD 内嵌字体常被裁剪到只保留字形轮廓（缺少 cmap），此时只能按显式字形索引取字形。
     *
     * @return true 表示可以按字符查表
     */
    public boolean hasUnicodeCmap() {
        return unicodeCmap != null;
    }

    /**
     * 字体
     *
     * @return 已解析的字体
     */
    public TrueTypeFont getFont() {
        return font;
    }

    /**
     * 字形选择结果
     * <p>
     * 依次记录输出的每个字形编号及其对应的字符位置（用于 ToUnicode 的 bfchar 与 TJ 步进）。
     */
    public static final class GlyphSelection {

        /**
         * 输出的字形编号
         */
        private final List<Integer> glyphIds = new ArrayList<>();

        /**
         * 每个字形对应的字符位置：0 表示该字形为本字符的首个字形，-1 表示无对应字符
         * <p>
         * 多对一时，多个字符映射到的同一字形位置为各自字符的索引；
         * 一对多时，首个字形为 0，后续字形为 -1。
         */
        private final List<Integer> textPositions = new ArrayList<>();

        /**
         * 输入字符数
         */
        private final int codePointCount;

        /**
         * 是否发生了替换（回退字形 0 或字形越界）
         */
        private boolean substituted;

        /**
         * 创建字形选择结果
         *
         * @param codePointCount 输入字符数，单位为个
         */
        private GlyphSelection(int codePointCount) {
            this.codePointCount = codePointCount;
        }

        /**
         * 追加一个字形编号
         *
         * @param gid          字形编号
         * @param textPosition 对应字符位置：0 表示本字符首个字形，-1 表示无对应字符
         */
        private void add(int gid, int textPosition) {
            glyphIds.add(gid);
            textPositions.add(textPosition);
        }

        /**
         * 标记发生了字形替换
         */
        private void markSubstituted() {
            this.substituted = true;
        }

        /**
         * 输出字形数量
         *
         * @return 数量，单位为个
         */
        public int glyphCount() {
            return glyphIds.size();
        }

        /**
         * 输入字符数
         *
         * @return 数量，单位为个
         */
        public int codePointCount() {
            return codePointCount;
        }

        /**
         * 获取指定位置的输出字形编号
         *
         * @param index 输出序号，从 0 开始
         * @return 字形编号
         */
        public int getGlyphId(int index) {
            return glyphIds.get(index);
        }

        /**
         * 获取指定输出字形对应的字符位置
         *
         * @param index 输出序号，从 0 开始
         * @return 0 表示该字形为本字符首个字形，-1 表示无对应字符
         */
        public int getTextPosition(int index) {
            return textPositions.get(index);
        }

        /**
         * 输出字形编号数组
         *
         * @return 字形编号数组
         */
        public int[] getGlyphIds() {
            int[] result = new int[glyphIds.size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = glyphIds.get(i);
            }
            return result;
        }

        /**
         * 是否发生了字形替换（回退为字形 0 或字形编号越界）
         *
         * @return true - 发生了替换，需要输出 warning 日志
         */
        public boolean isSubstituted() {
            return substituted;
        }

        @Override
        public String toString() {
            return "GlyphSelection{glyphs=" + glyphIds + ", positions=" + textPositions
                    + ", substituted=" + substituted + "}";
        }
    }
}
