package org.ofdrw.converter.renderer;

import org.ofdrw.converter.renderer.command.DrawCommand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 绘制命令列表，是录制渲染器的产物，也是后续回放渲染器的输入。
 * <p>
 * 包围盒用于快速判断列表内容是否落在可视区域内，随命令追加而累积。
 * 本类非线程安全。
 */
public final class DisplayList {

    /**
     * 绘制命令序列
     */
    private final List<DrawCommand> commands;

    /**
     * 包围盒左边界，单位：像素
     */
    private double minX = Double.POSITIVE_INFINITY;

    /**
     * 包围盒上边界，单位：像素
     */
    private double minY = Double.POSITIVE_INFINITY;

    /**
     * 包围盒右边界，单位：像素
     */
    private double maxX = Double.NEGATIVE_INFINITY;

    /**
     * 包围盒下边界，单位：像素
     */
    private double maxY = Double.NEGATIVE_INFINITY;

    /**
     * 构造空的命令列表。
     */
    public DisplayList() {
        this(32);
    }

    /**
     * 按预期容量构造命令列表，避免大量命令时反复扩容。
     *
     * @param expectedCommands 预期命令数量，必须大于等于 0
     */
    public DisplayList(int expectedCommands) {
        this.commands = new ArrayList<>(Math.max(0, expectedCommands));
    }

    /**
     * 追加一条绘制命令，并扩展包围盒。
     *
     * @param command 绘制命令，不能为空
     */
    public void add(DrawCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("绘制命令不能为空");
        }
        commands.add(command);
        for (double[] point : command.getBounds()) {
            if (point.length < 2) {
                continue;
            }
            if (Double.isNaN(point[0]) || Double.isNaN(point[1])) {
                continue;
            }
            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
    }

    /**
     * 获取绘制命令列表（只读视图）。
     *
     * @return 绘制命令列表
     */
    public List<DrawCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    /**
     * 获取命令数量。
     *
     * @return 命令数量
     */
    public int size() {
        return commands.size();
    }

    /**
     * 判断包围盒是否有效（至少有一条命令贡献了坐标）。
     *
     * @return true 表示包围盒有效
     */
    public boolean hasBounds() {
        return !commands.isEmpty() && minX <= maxX && minY <= maxY;
    }

    /**
     * 获取包围盒。
     *
     * @return 长度为 4 的数组 {minX, minY, maxX, maxY}，单位：像素；无有效包围盒时返回 null
     */
    public double[] getBounds() {
        if (!hasBounds()) {
            return null;
        }
        return new double[]{minX, minY, maxX, maxY};
    }

    /**
     * 清空所有命令与包围盒。
     */
    public void clear() {
        commands.clear();
        minX = Double.POSITIVE_INFINITY;
        minY = Double.POSITIVE_INFINITY;
        maxX = Double.NEGATIVE_INFINITY;
        maxY = Double.NEGATIVE_INFINITY;
    }

    @Override
    public String toString() {
        return "DisplayList{commands=" + commands.size() + ", bounds=" + java.util.Arrays.toString(getBounds()) + "}";
    }
}
