package org.ofdrw.converter.pdf.render.text;

import java.util.Collections;
import java.util.List;

/**
 * Delta 偏移队列（表 46 的 DeltaX / DeltaY）
 * <p>
 * 语义（GB/T 33190-2016 表 46 备注）：
 * <ul>
 *     <li>第 i 个值表示“第 i 个文字与前一个文字”之间的偏移；首个文字不产生偏移；</li>
 *     <li>队列长度小于文字数量时，<b>取最后一个值延续</b>（末位尾部增量），现有实现忽略该规则导致竖排、旋转文本错位；</li>
 *     <li>{@code g} 重复语法（{@code <重复次数> <值>}）在解析阶段已展开，队列中只保留数值。</li>
 * </ul>
 * 本类为不可变对象，{@link #deltaAt(int)} 即时计算延续值，不复制队列。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class DeltaQueue {

    /**
     * 空队列：任何位置上偏移均为 0
     */
    public static final DeltaQueue EMPTY = new DeltaQueue(Collections.emptyList());

    /**
     * 偏移值序列，单位与被定位对象一致（文字定位中为 mm）
     */
    private final List<Double> values;

    /**
     * 构造偏移队列
     *
     * @param values 偏移值序列，可为空
     */
    public DeltaQueue(List<Double> values) {
        this.values = values == null ? Collections.emptyList() : Collections.unmodifiableList(values);
    }

    /**
     * 获取指定字符位置对应的偏移
     * <p>
     * 下标 {@code index} 表示“第 index 个文字（从 0 开始）与前一个文字”的偏移：
     * {@code index = 0} 恒为 0；{@code index} 超出队列长度时取队列最后一个值（末位尾部增量）。
     *
     * @param index 字符下标，从 0 开始
     * @return 偏移值；队列为空或 {@code index <= 0} 时返回 0
     */
    public double deltaAt(int index) {
        if (index <= 0 || values.isEmpty()) {
            return 0d;
        }
        int last = values.size() - 1;
        int pos = index - 1;
        return values.get(Math.min(pos, last));
    }

    /**
     * 队列是否为空
     *
     * @return true 表示未声明偏移
     */
    public boolean isEmpty() {
        return values.isEmpty();
    }

    /**
     * 队列长度
     *
     * @return 显式声明的偏移值数量，单位：个
     */
    public int size() {
        return values.size();
    }

    /**
     * 获取队列中的原始值
     *
     * @param index 下标，从 0 开始
     * @return 偏移值
     */
    public double get(int index) {
        return values.get(index);
    }

    /**
     * 获取只读的值列表
     *
     * @return 值列表
     */
    public List<Double> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return values.toString();
    }
}
