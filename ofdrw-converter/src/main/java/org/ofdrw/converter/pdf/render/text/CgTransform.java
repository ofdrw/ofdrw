package org.ofdrw.converter.pdf.render.text;

import java.util.Collections;
import java.util.List;

/**
 * 字符到字形的变换关系（GB/T 33190-2016 表 48 CGTransform）
 * <p>
 * 语义：TextCode 中从 {@link #getCodePosition()} 开始、连续 {@link #getCodeCount()} 个字符，
 * 依次对应 {@link #getGlyphs()} 中的字形索引；由此可表达四种映射：
 * <table border="1">
 *     <caption>CGTransform 映射类型</caption>
 *     <tr><th>类型</th><th>CodeCount</th><th>GlyphCount</th></tr>
 *     <tr><td>一对一</td><td>1</td><td>1</td></tr>
 *     <tr><td>多对一（连字）</td><td>n &gt; 1</td><td>1</td></tr>
 *     <tr><td>一对多</td><td>1</td><td>m &gt; 1</td></tr>
 *     <tr><td>多对多</td><td>n &gt; 1</td><td>m &gt; 1</td></tr>
 * </table>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class CgTransform {

    /**
     * TextCode 中字符编码的起始位置，从 0 开始
     */
    private final int codePosition;

    /**
     * 变换关系中字符数量，至少为 1
     */
    private final int codeCount;

    /**
     * 变换后对应的字形索引列表
     */
    private final List<Integer> glyphs;

    /**
     * 构造字形变换
     *
     * @param codePosition 字符编码起始位置，从 0 开始
     * @param codeCount    字符数量，至少 1（小于 1 时按 1 处理）
     * @param glyphs       字形索引列表，可为空
     */
    public CgTransform(int codePosition, int codeCount, List<Integer> glyphs) {
        this.codePosition = Math.max(0, codePosition);
        this.codeCount = Math.max(1, codeCount);
        this.glyphs = glyphs == null ? Collections.emptyList() : Collections.unmodifiableList(glyphs);
    }

    /**
     * 字符编码起始位置
     *
     * @return 起始位置，从 0 开始
     */
    public int getCodePosition() {
        return codePosition;
    }

    /**
     * 字符数量
     *
     * @return 字符数量，至少 1
     */
    public int getCodeCount() {
        return codeCount;
    }

    /**
     * 字形索引个数
     *
     * @return 字形数量，单位：个
     */
    public int getGlyphCount() {
        return glyphs.size();
    }

    /**
     * 获取字形索引列表
     *
     * @return 只读列表
     */
    public List<Integer> getGlyphs() {
        return glyphs;
    }

    /**
     * 判断指定字符位置是否落在本变换的字符区间内
     *
     * @param codeIndex 字符位置，从 0 开始
     * @return true 表示属于本变换
     */
    public boolean contains(int codeIndex) {
        return codeIndex >= codePosition && codeIndex < codePosition + codeCount;
    }

    /**
     * 判断指定位置是否为多对一（连字）的后续字符
     * <p>
     * 多对一时，除首个字符外其余字符不再单独产生字形与位移，因此需要跳过其 Delta 偏移。
     *
     * @param codeIndex 字符位置，从 0 开始
     * @return true 表示该字符与前一字符共享同一字形
     */
    public boolean isContinuationOfManyToOne(int codeIndex) {
        return contains(codeIndex) && codeIndex > codePosition && glyphs.size() == 1;
    }

    @Override
    public String toString() {
        return "CG[pos=" + codePosition + ", codes=" + codeCount + ", glyphs=" + glyphs + "]";
    }
}
