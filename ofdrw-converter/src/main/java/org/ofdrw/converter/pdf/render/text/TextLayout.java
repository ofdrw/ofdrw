package org.ofdrw.converter.pdf.render.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 文字布局结果：逐字形的定位与 Unicode 信息（渲染器无关）
 * <p>
 * 本类只做“OFD 文字语义 → 逐字布局”的计算，不涉及 PDF 字体、PDFBox 或字符方向矩阵，
 * 因此可以脱离渲染后端断言（设计 4.1 节“渲染器无关”）。
 * <p>
 * 坐标语义：
 * <ul>
 *     <li>{@link PlacedGlyph#getX()}、{@link PlacedGlyph#getY()}：字形原点在<b>对象空间</b>中的位置，单位 mm
 *     （已叠加图元自身 CTM，尚未叠加外接矩形平移与页面换算）；</li>
 *     <li>{@link PlacedGlyph#getAdvanceMm()}：字形沿阅读方向的步进，单位 mm；</li>
 *     <li>{@link PlacedGlyph#getText()}：该字形对应的文字，纯占位（无文字）时为空串。</li>
 * </ul>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextLayout {

    /**
     * 逐字布局结果
     */
    private final List<PlacedGlyph> glyphs;

    /**
     * 布局过程中发生的降级事件（供调用方输出 warning 日志）
     */
    private final List<String> warnings;

    /**
     * 构造布局结果
     *
     * @param glyphs   逐字布局结果
     * @param warnings 降级事件说明
     */
    public TextLayout(List<PlacedGlyph> glyphs, List<String> warnings) {
        this.glyphs = glyphs == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(glyphs));
        this.warnings = warnings == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(warnings));
    }

    /**
     * 逐字布局结果
     *
     * @return 只读列表，按绘制顺序排列
     */
    public List<PlacedGlyph> getGlyphs() {
        return glyphs;
    }

    /**
     * 降级事件说明
     *
     * @return 只读列表，可能为空
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * 是否没有可绘制的字形
     *
     * @return true 表示布局为空
     */
    public boolean isEmpty() {
        return glyphs.isEmpty();
    }

    /**
     * 拼接全部字形的文字
     *
     * @return 可提取文本
     */
    public String getText() {
        StringBuilder sb = new StringBuilder(glyphs.size());
        for (PlacedGlyph glyph : glyphs) {
            sb.append(glyph.getText());
        }
        return sb.toString();
    }

    /**
     * 单个字形的布局结果
     */
    public static final class PlacedGlyph {

        /**
         * 文字内容
         */
        private final String text;

        /**
         * Unicode 码位序列，空表示该字形不携带文字（多对一、一对多的附属字形）
         */
        private final int[] codePoints;

        /**
         * 显式字形索引（来自 CGTransform），-1 表示未给出
         */
        private final int explicitGlyphId;

        /**
         * 字形原点横坐标，单位 mm（对象空间）
         */
        private final double x;

        /**
         * 字形原点纵坐标，单位 mm（对象空间）
         */
        private final double y;

        /**
         * 沿阅读方向的步进，单位 mm
         */
        private final double advanceMm;

        /**
         * 是否因多对一映射而与前一字符共享字形（不产生独立位移）
         */
        private final boolean shared;

        /**
         * 构造字形布局
         *
         * @param text            文字内容
         * @param codePoints      Unicode 码位序列
         * @param explicitGlyphId 显式字形索引，-1 表示未给出
         * @param x               字形原点横坐标，单位 mm
         * @param y               字形原点纵坐标，单位 mm
         * @param advanceMm       沿阅读方向的步进，单位 mm
         * @param shared          是否与前一字符共享字形
         */
        public PlacedGlyph(String text, int[] codePoints, int explicitGlyphId,
                           double x, double y, double advanceMm, boolean shared) {
            this.text = text == null ? "" : text;
            this.codePoints = codePoints == null ? new int[0] : codePoints.clone();
            this.explicitGlyphId = explicitGlyphId;
            this.x = x;
            this.y = y;
            this.advanceMm = advanceMm;
            this.shared = shared;
        }

        /**
         * 文字内容
         *
         * @return 文字内容，可能为空串
         */
        public String getText() {
            return text;
        }

        /**
         * Unicode 码位序列
         *
         * @return 码位副本，可能为空
         */
        public int[] getCodePoints() {
            return codePoints.clone();
        }

        /**
         * 显式字形索引
         *
         * @return 字形索引；-1 表示 CGTransform 未给出，需要按 cmap 查表
         */
        public int getExplicitGlyphId() {
            return explicitGlyphId;
        }

        /**
         * @return 字形原点横坐标，单位 mm（对象空间）
         */
        public double getX() {
            return x;
        }

        /**
         * @return 字形原点纵坐标，单位 mm（对象空间）
         */
        public double getY() {
            return y;
        }

        /**
         * @return 沿阅读方向的步进，单位 mm
         */
        public double getAdvanceMm() {
            return advanceMm;
        }

        /**
         * @return true 表示该字形与前一字符共享（多对一映射）
         */
        public boolean isShared() {
            return shared;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PlacedGlyph)) {
                return false;
            }
            PlacedGlyph other = (PlacedGlyph) o;
            return explicitGlyphId == other.explicitGlyphId
                    && Double.compare(x, other.x) == 0
                    && Double.compare(y, other.y) == 0
                    && Double.compare(advanceMm, other.advanceMm) == 0
                    && shared == other.shared
                    && Objects.equals(text, other.text);
        }

        @Override
        public int hashCode() {
            return Objects.hash(text, explicitGlyphId, x, y, advanceMm, shared);
        }

        @Override
        public String toString() {
            return "Glyph('" + text + "', gid=" + explicitGlyphId + ", " + x + "," + y
                    + " mm, adv=" + advanceMm + (shared ? ", shared" : "") + ")";
        }
    }
}
