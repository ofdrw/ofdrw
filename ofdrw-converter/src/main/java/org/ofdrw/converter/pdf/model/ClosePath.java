package org.ofdrw.converter.pdf.model;

/**
 * 闭合子路径，对应 PDF 操作符 {@code h}
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ClosePath implements PathSegment {

    /**
     * 指令实例（无状态）
     */
    public static final ClosePath INSTANCE = new ClosePath();

    private ClosePath() {
    }

    /**
     * 闭合路径后当前点回到子路径起点，此处返回起点坐标由路径构造方维护，故返回 {@code NaN}
     *
     * @return {@code NaN}，调用方不应依赖闭合指令的终点
     */
    @Override
    public double getEndX() {
        return Double.NaN;
    }

    /**
     * @return {@code NaN}，调用方不应依赖闭合指令的终点
     */
    @Override
    public double getEndY() {
        return Double.NaN;
    }

    @Override
    public String toString() {
        return "h";
    }
}
