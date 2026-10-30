package org.ofdrw.converter.pdf.model;

/**
 * 底纹（Pattern）填充指令
 * <p>
 * 对应 PDF 的 Tiling Pattern（{@code /PatternType 1}）。设计决策 17：首版按 {@code ReflectMethod=Normal} 近似平铺，
 * 其余翻转与页面相对定位在结果中标记为“近似”，并在渲染时输出 warning 日志。
 * <p>
 * 坐标系约定：{@link #getCell()} 是单元格在 PDF 页面坐标（单位 pt）中的显示区域，单元格内容
 * {@link #getCellCommands()} 使用页面坐标，由写入器负责平铺时用 {@code cm} 偏移复制。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PatternCommand extends AbstractCommand {

    /**
     * 底纹名称（用于调试与日志）
     */
    private final String name;

    /**
     * 底纹单元格，单位 pt（PDF 页面坐标）
     */
    private final Box cell;

    /**
     * 单元格内容的指令序列（可包含路径、文字、图像）
     */
    private final java.util.List<ContentCommand> cellCommands;

    /**
     * 是否使用了近似实现（非 Normal 翻转或页面相对定位）
     */
    private final boolean approximate;

    /**
     * 构造底纹指令
     *
     * @param name         底纹名称，可为空
     * @param cell         单元格，单位 pt（PDF 页面坐标），不能为空
     * @param cellCommands 单元格内容指令，不能为空
     * @param approximate  是否使用近似实现
     */
    public PatternCommand(String name, Box cell, java.util.List<ContentCommand> cellCommands, boolean approximate) {
        super(CoordSpace.PAGE);
        if (cell == null) {
            throw new IllegalArgumentException("底纹单元格不能为空");
        }
        if (cell.getWidth() <= 0 || cell.getHeight() <= 0) {
            throw new IllegalArgumentException("底纹单元格尺寸必须大于 0: " + cell);
        }
        if (cellCommands == null || cellCommands.isEmpty()) {
            throw new IllegalArgumentException("底纹单元格内容不能为空");
        }
        this.name = name == null ? "" : name;
        this.cell = cell;
        this.cellCommands = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(cellCommands));
        this.approximate = approximate;
    }

    /**
     * @return 底纹名称
     */
    public String getName() {
        return name;
    }

    /**
     * @return 底纹单元格，单位 pt（PDF 页面坐标）
     */
    public Box getCell() {
        return cell;
    }

    /**
     * @return 单元格内容指令（只读）
     */
    public java.util.List<ContentCommand> getCellCommands() {
        return cellCommands;
    }

    /**
     * @return true 表示使用了近似实现（设计决策 17）
     */
    public boolean isApproximate() {
        return approximate;
    }

    @Override
    public String toString() {
        return "Pattern(" + name + ", cell=" + cell + ", approximate=" + approximate + ")";
    }
}
