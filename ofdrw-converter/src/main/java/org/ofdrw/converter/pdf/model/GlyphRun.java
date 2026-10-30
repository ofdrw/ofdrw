package org.ofdrw.converter.pdf.model;

/**
 * 字形条目：文字绘制中的一个字形及其在 PDF 中的定位信息
 * <p>
 * 设计决策 3（字形优先 + ToUnicode）：条目同时携带<b>字形索引</b>（保证视觉一致）与
 * <b>Unicode 文本</b>（保证可提取、可搜索）。
 * <p>
 * 坐标语义：{@code x}/{@code y} 是字形原点在<b>文本空间</b>中的位置，即尚未应用 {@link TextCommand#getTextMatrix()}
 * 与页面坐标换算之前的值；{@link #getAdvancePt()} 是该字形在文本空间中的前进量。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class GlyphRun {

    /**
     * 字形在字体中的索引（GID），未知时取 -1
     */
    private final int glyphId;

    /**
     * 对应的 Unicode 文本，无法确定时为空字符串
     */
    private final String unicode;

    /**
     * 字形原点横坐标，单位 pt（文本空间）
     */
    private final double x;

    /**
     * 字形原点纵坐标，单位 pt（文本空间）
     */
    private final double y;

    /**
     * 字形前进量，单位 pt（文本空间）
     */
    private final double advancePt;

    /**
     * 构造字形条目
     *
     * @param glyphId   字形索引，未知时取 -1
     * @param unicode   对应 Unicode 文本，可为空
     * @param x         字形原点横坐标，单位 pt（文本空间）
     * @param y         字形原点纵坐标，单位 pt（文本空间）
     * @param advancePt 字形前进量，单位 pt（文本空间）
     */
    public GlyphRun(int glyphId, String unicode, double x, double y, double advancePt) {
        this.glyphId = glyphId;
        this.unicode = unicode == null ? "" : unicode;
        this.x = x;
        this.y = y;
        this.advancePt = advancePt;
    }

    /**
     * @return 字形索引（GID），未知时为 -1
     */
    public int getGlyphId() {
        return glyphId;
    }

    /**
     * @return 是否为已知字形索引
     */
    public boolean hasGlyphId() {
        return glyphId >= 0;
    }

    /**
     * @return 对应 Unicode 文本，可能为空字符串
     */
    public String getUnicode() {
        return unicode;
    }

    /**
     * @return 字形原点横坐标，单位 pt（文本空间）
     */
    public double getX() {
        return x;
    }

    /**
     * @return 字形原点纵坐标，单位 pt（文本空间）
     */
    public double getY() {
        return y;
    }

    /**
     * @return 字形前进量，单位 pt（文本空间）
     */
    public double getAdvancePt() {
        return advancePt;
    }

    @Override
    public String toString() {
        return "Glyph(gid=" + glyphId + ", '" + unicode + "', " + x + "," + y + ", adv=" + advancePt + ")";
    }
}
