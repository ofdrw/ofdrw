package org.ofdrw.converter.pdf.model;

/**
 * 裁剪指令：对应 PDF 的路径构造 + {@code W}/{@code W*} + {@code n}，由图形状态栈（q/Q）限定作用范围
 * <p>
 * 裁剪语义（设计第 2 节、OFD 8.4 节）：
 * <ul>
 *     <li>多个 Clip 之间取 <b>交集</b>：每个 Clip 生成一条独立的 {@link ClipCommand}，写入器在 q 内依次累积；</li>
 *     <li>同一 Clip 内的多个 Area 取 <b>并集</b>：由渲染器合并为一条裁剪路径后再生成指令（{@link #isUnionArea()} 标记）；</li>
 *     <li>裁剪区域随线宽等绘制参数影响（表 33 DrawParam 备注）：渲染器需在裁剪指令前写入 {@link StrokeStyle}。</li>
 * </ul>
 * 裁剪指令本身不改变绘制内容，写入器应使用 {@code n}（不绘制）结束路径。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ClipCommand extends AbstractCommand {

    /**
     * 裁剪路径（可包含多条子路径，子路径之间按并集处理）
     */
    private final java.util.List<PathSegment> segments;

    /**
     * 填充规则
     */
    private final FillRule fillRule;

    /**
     * 是否由多个 Area 合并而来（并集）
     */
    private final boolean unionArea;

    /**
     * 构造裁剪指令
     *
     * @param segments  裁剪路径，不能为空，首元素必须是 {@link MoveTo}
     * @param fillRule  填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param unionArea 是否由多个 Area 合并而来
     */
    public ClipCommand(java.util.List<PathSegment> segments, FillRule fillRule, boolean unionArea) {
        this(CoordSpace.PAGE, segments, fillRule, unionArea);
    }

    /**
     * 构造裁剪指令（指定坐标作用域）
     *
     * @param coordSpace 坐标作用域
     * @param segments   裁剪路径，不能为空，首元素必须是 {@link MoveTo}
     * @param fillRule   填充规则，为空按 {@link FillRule#NON_ZERO}
     * @param unionArea  是否由多个 Area 合并而来
     */
    public ClipCommand(CoordSpace coordSpace, java.util.List<PathSegment> segments, FillRule fillRule, boolean unionArea) {
        super(coordSpace);
        if (segments == null || segments.isEmpty()) {
            throw new IllegalArgumentException("裁剪路径不能为空");
        }
        if (!(segments.get(0) instanceof MoveTo)) {
            throw new IllegalArgumentException("裁剪路径必须以 MoveTo 开始");
        }
        this.segments = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(segments));
        this.fillRule = fillRule == null ? FillRule.NON_ZERO : fillRule;
        this.unionArea = unionArea;
    }

    /**
     * @return 裁剪路径（只读）
     */
    public java.util.List<PathSegment> getSegments() {
        return segments;
    }

    /**
     * @return 填充规则
     */
    public FillRule getFillRule() {
        return fillRule;
    }

    /**
     * @return true 表示由多个 Area 合并而来
     */
    public boolean isUnionArea() {
        return unionArea;
    }

    @Override
    public String toString() {
        return "Clip(" + segments.size() + " segments, " + fillRule + ", union=" + unionArea + ")";
    }
}
