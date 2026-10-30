package org.ofdrw.converter.pdf.model;

import java.util.Arrays;

/**
 * 虚线样式
 * <p>
 * 对应 PDF 的 {@code d} 操作符：{@code [dashArray] dashPhase d}。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class DashPattern {

    /**
     * 实线（无虚线）
     */
    public static final DashPattern SOLID = new DashPattern(new double[0], 0);

    /**
     * 虚线段长度序列，单位 pt，元素必须非负且不能全为 0
     */
    private final double[] dashArray;

    /**
     * 虚线起始偏移，单位 pt
     */
    private final double dashPhase;

    /**
     * 构造虚线样式
     *
     * @param dashArray 虚线段长度序列，单位 pt，可为空数组表示实线
     * @param dashPhase 虚线起始偏移，单位 pt
     */
    public DashPattern(double[] dashArray, double dashPhase) {
        double[] normalized = new double[0];
        double phase = 0;
        if (dashArray != null && dashArray.length > 0) {
            double[] copy = new double[dashArray.length];
            boolean allZero = true;
            for (int i = 0; i < dashArray.length; i++) {
                double len = dashArray[i];
                if (Double.isNaN(len) || Double.isInfinite(len) || len < 0) {
                    len = 0;
                }
                copy[i] = len;
                if (len > 0) {
                    allZero = false;
                }
            }
            if (!allZero) {
                normalized = copy;
                phase = (Double.isNaN(dashPhase) || Double.isInfinite(dashPhase)) ? 0 : dashPhase;
            }
        }
        // 空数组或全 0 虚线在 PDF 中非法，统一退化为实线
        this.dashArray = normalized;
        this.dashPhase = phase;
    }

    /**
     * @return 虚线段长度副本，单位 pt；空数组表示实线
     */
    public double[] getDashArray() {
        return dashArray.clone();
    }

    /**
     * @return 虚线起始偏移，单位 pt
     */
    public double getDashPhase() {
        return dashPhase;
    }

    /**
     * @return true 表示实线
     */
    public boolean isSolid() {
        return dashArray.length == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DashPattern)) {
            return false;
        }
        DashPattern other = (DashPattern) o;
        return Double.compare(dashPhase, other.dashPhase) == 0 && Arrays.equals(dashArray, other.dashArray);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(dashArray) * 31 + Double.hashCode(dashPhase);
    }

    @Override
    public String toString() {
        return "Dash" + Arrays.toString(dashArray) + " phase=" + dashPhase;
    }
}
