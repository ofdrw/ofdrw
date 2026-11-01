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
     * 需要被底纹填充的区域路径，单位 pt（PDF 页面坐标）
     * <p>
     * 图案本身只描述“单元怎么画”，必须由调用方给出“填在哪里”，否则写入侧无法生成 {@code f} 着色操作。
     */
    private final java.util.List<PathSegment> area;

    /**
     * 平铺变换矩阵（底纹的 {@code Pattern/CTM}，六元组行向量写法），{@code null} 表示单位矩阵
     * <p>
     * 旋转/缩放的斜向水印全部依赖该矩阵；平铺时每个单元都要先应用它，否则单元会被正向排布。
     */
    private final double[] tilingMatrix;

    /**
     * 平铺步进横向距离，单位 pt（PDF 页面坐标）
     */
    private final double xStepPt;

    /**
     * 平铺步进纵向距离，单位 pt（PDF 页面坐标）
     */
    private final double yStepPt;

    /**
     * 构造底纹指令
     *
     * @param name         底纹名称，可为空
     * @param cell         单元格，单位 pt（PDF 页面坐标），不能为空
     * @param cellCommands 单元格内容指令，不能为空
     * @param approximate  是否使用近似实现
     */
    public PatternCommand(String name, Box cell, java.util.List<ContentCommand> cellCommands, boolean approximate) {
        this(name, cell, cellCommands, approximate, null);
    }

    /**
     * 构造底纹指令（带填充区域）
     *
     * @param name         底纹名称，可为空
     * @param cell         单元格，单位 pt（PDF 页面坐标），不能为空
     * @param cellCommands 单元格内容指令，不能为空
     * @param approximate  是否使用近似实现
     * @param area         需要被填充的区域路径，单位 pt；为空时写入侧跳过该底纹
     */
    public PatternCommand(String name, Box cell, java.util.List<ContentCommand> cellCommands, boolean approximate,
                          java.util.List<PathSegment> area) {
        this(name, cell, cellCommands, approximate, area, null,
                cell == null ? 0 : cell.getWidth(), cell == null ? 0 : cell.getHeight());
    }

    /**
     * 构造底纹指令（带填充区域、平铺矩阵与步进）
     *
     * @param name         底纹名称，可为空
     * @param cell         单元格，单位 pt（PDF 页面坐标），不能为空
     * @param cellCommands 单元格内容指令，不能为空
     * @param approximate  是否使用近似实现
     * @param area         需要被填充的区域路径，单位 pt
     * @param tilingMatrix 平铺变换矩阵（行向量 {a,b,c,d,e,f}），为空表示单位矩阵
     * @param xStepPt      平铺步进横向距离，单位 pt
     * @param yStepPt      平铺步进纵向距离，单位 pt
     */
    public PatternCommand(String name, Box cell, java.util.List<ContentCommand> cellCommands, boolean approximate,
                          java.util.List<PathSegment> area, double[] tilingMatrix,
                          double xStepPt, double yStepPt) {
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
        this.area = area == null
                ? java.util.Collections.<PathSegment>emptyList()
                : java.util.Collections.unmodifiableList(new java.util.ArrayList<>(area));
        this.tilingMatrix = tilingMatrix == null ? null : tilingMatrix.clone();
        this.xStepPt = xStepPt;
        this.yStepPt = yStepPt;
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
     * @return 需要被填充的区域路径，单位 pt（只读）；为空表示无法确定填充范围
     */
    public java.util.List<PathSegment> getArea() {
        return area;
    }

    /**
     * @return 平铺变换矩阵（行向量 {a,b,c,d,e,f}），为空表示单位矩阵
     */
    public double[] getTilingMatrix() {
        return tilingMatrix == null ? null : tilingMatrix.clone();
    }

    /**
     * @return 平铺步进横向距离，单位 pt
     */
    public double getXStepPt() {
        return xStepPt;
    }

    /**
     * @return 平铺步进纵向距离，单位 pt
     */
    public double getYStepPt() {
        return yStepPt;
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
