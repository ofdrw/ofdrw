package org.ofdrw.converter.pdf.render.text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一个 TextCode 的库无关视图：文字内容 + 首个字形原点 + Delta 队列 + 字形变换
 * <p>
 * 字段语义严格对应 GB/T 33190-2016 表 46（文字定位）与表 48（CGTransform）：
 * <ul>
 *     <li>{@link #getX()}、{@link #getY()}：首个文字的字形原点（对象坐标），单位 mm；属性缺失时继承上一个 TextCode；</li>
 *     <li>{@link #getDeltaX()}、{@link #getDeltaY()}：相邻文字之间的偏移队列，单位 mm；
 *     队列已按 {@code g} 重复语法展开，长度不足时按最后一个值延续（含末位尾部增量），见
 *     {@link org.ofdrw.converter.pdf.render.text.DeltaQueue}；</li>
 *     <li>{@link #getCgTransforms()}：字符编码与字形索引的变换关系，见 {@link CgTransform}。</li>
 * </ul>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TextCodeRun {

    /**
     * 文字内容（已做 XML 实体还原）
     */
    private final String text;

    /**
     * 首个文字的字形原点横坐标，单位 mm
     */
    private final double x;

    /**
     * 首个文字的字形原点纵坐标，单位 mm
     */
    private final double y;

    /**
     * X 方向相邻文字偏移队列，单位 mm；未声明时为空队列
     */
    private final DeltaQueue deltaX;

    /**
     * Y 方向相邻文字偏移队列，单位 mm；未声明时为空队列
     */
    private final DeltaQueue deltaY;

    /**
     * 字符到字形的变换关系
     */
    private final List<CgTransform> cgTransforms;

    /**
     * 构造 TextCode 视图
     *
     * @param text         文字内容，可为 null
     * @param x            首个文字的字形原点横坐标，单位 mm
     * @param y            首个文字的字形原点纵坐标，单位 mm
     * @param deltaX       X 方向偏移队列，可为 null
     * @param deltaY       Y 方向偏移队列，可为 null
     * @param cgTransforms 字符到字形的变换关系，可为 null
     */
    public TextCodeRun(String text, double x, double y, DeltaQueue deltaX, DeltaQueue deltaY,
                       List<CgTransform> cgTransforms) {
        this.text = text == null ? "" : text;
        this.x = x;
        this.y = y;
        this.deltaX = deltaX == null ? DeltaQueue.EMPTY : deltaX;
        this.deltaY = deltaY == null ? DeltaQueue.EMPTY : deltaY;
        this.cgTransforms = cgTransforms == null ? Collections.emptyList()
                : Collections.unmodifiableList(new ArrayList<>(cgTransforms));
    }

    /**
     * 文字内容
     *
     * @return 文字内容，不会为 null
     */
    public String getText() {
        return text;
    }

    /**
     * 首个文字的字形原点横坐标
     *
     * @return 横坐标，单位 mm
     */
    public double getX() {
        return x;
    }

    /**
     * 首个文字的字形原点纵坐标
     *
     * @return 纵坐标，单位 mm
     */
    public double getY() {
        return y;
    }

    /**
     * X 方向偏移队列
     *
     * @return 偏移队列，未声明时为空队列
     */
    public DeltaQueue getDeltaX() {
        return deltaX;
    }

    /**
     * Y 方向偏移队列
     *
     * @return 偏移队列，未声明时为空队列
     */
    public DeltaQueue getDeltaY() {
        return deltaY;
    }

    /**
     * 字符到字形的变换关系
     *
     * @return 只读列表，未声明时为空列表
     */
    public List<CgTransform> getCgTransforms() {
        return cgTransforms;
    }

    @Override
    public String toString() {
        return "TextCodeRun('" + text + "' @" + x + "," + y + " mm, dx=" + deltaX.size()
                + ", dy=" + deltaY.size() + ", cg=" + cgTransforms.size() + ")";
    }
}
