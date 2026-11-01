package org.ofdrw.converter.pdf.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 中间模型页面内容：描述一个 PDF 页面的内容指令序列
 * <p>
 * 设计 4.1 节：渲染管线把 OFD 页面转换为与库无关的指令序列，再由具体写入器（PDFBox 3 或测试用 DumpWriter）
 * 渲染为 PDF 内容流，从而支持“渲染器无关”的单元测试。
 * <p>
 * 语义约定：
 * <ul>
 *     <li>指令按加入顺序执行；</li>
 *     <li>指令序列必须 q/Q 自平衡——{@link SaveState} 与 {@link RestoreState} 成对出现，实现见 {@link #isBalanced()}；</li>
 *     <li>页面尺寸单位为 pt，见 {@link #getWidthPt()}、{@link #getHeightPt()}，页面框按决策 26 与 MediaBox 一致；</li>
 *     <li>内容指令统一使用 {@link CoordSpace#PAGE} 作用域的坐标。</li>
 * </ul>
 * 本类可变（按顺序追加指令），但对外暴露的指令列表只读。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfPageContent {

    /**
     * 页面宽度，单位 pt
     */
    private final double widthPt;

    /**
     * 页面高度，单位 pt
     */
    private final double heightPt;

    /**
     * 内容指令序列
     */
    private final List<ContentCommand> commands = new ArrayList<>();

    /**
     * 是否存在无法解析的图像资源
     * <p>
     * 图像引用缺失（资源 ID 为空、资源不存在或文件不可读）时置位，供导出侧按参考实现做整页栅格回退。
     */
    private boolean imageResourceMissing;

    /**
     * 构造页面内容
     *
     * @param widthPt  页面宽度，单位 pt，必须大于 0
     * @param heightPt 页面高度，单位 pt，必须大于 0
     */
    public PdfPageContent(double widthPt, double heightPt) {
        if (!(widthPt > 0) || !(heightPt > 0)) {
            throw new IllegalArgumentException("页面尺寸必须大于 0: " + widthPt + " x " + heightPt);
        }
        this.widthPt = widthPt;
        this.heightPt = heightPt;
    }

    /**
     * 追加一条内容指令
     *
     * @param command 内容指令，不能为空
     * @return this
     */
    public PdfPageContent add(ContentCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("内容指令不能为空");
        }
        commands.add(command);
        return this;
    }

    /**
     * 追加多条内容指令
     *
     * @param newCommands 内容指令序列，null 元素将被忽略
     * @return this
     */
    public PdfPageContent addAll(List<? extends ContentCommand> newCommands) {
        if (newCommands == null) {
            return this;
        }
        for (ContentCommand command : newCommands) {
            add(command);
        }
        return this;
    }

    /**
     * 保存图形状态（对应 PDF 的 {@code q}）
     *
     * @return this
     */
    public PdfPageContent saveState() {
        return add(SaveState.INSTANCE);
    }

    /**
     * 恢复图形状态（对应 PDF 的 {@code Q}）
     * <p>
     * 当前图形状态栈为空时不追加指令，避免生成不合法的内容流。
     *
     * @return this
     */
    public PdfPageContent restoreState() {
        if (stateDepth() <= 0) {
            return this;
        }
        return add(RestoreState.INSTANCE);
    }

    /**
     * 标记本页存在无法解析的图像资源
     */
    public void markImageResourceMissing() {
        imageResourceMissing = true;
    }

    /**
     * 判断本页是否存在无法解析的图像资源
     *
     * @return true 表示有图像引用无法解析
     */
    public boolean isImageResourceMissing() {
        return imageResourceMissing;
    }

    /**
     * 获取内容指令序列
     *
     * @return 只读的指令序列
     */
    public List<ContentCommand> getCommands() {
        return Collections.unmodifiableList(commands);
    }

    /**
     * 获取指令数量
     *
     * @return 指令数量
     */
    public int size() {
        return commands.size();
    }

    /**
     * 获取当前图形状态栈深度
     *
     * @return 深度，0 表示未保存过状态
     */
    public int stateDepth() {
        int depth = 0;
        for (ContentCommand command : commands) {
            if (command == SaveState.INSTANCE) {
                depth++;
            } else if (command == RestoreState.INSTANCE && depth > 0) {
                depth--;
            }
        }
        return depth;
    }

    /**
     * 判断 q/Q 是否自平衡
     *
     * @return true 表示成对
     */
    public boolean isBalanced() {
        return stateDepth() == 0;
    }

    /**
     * 获取页面宽度
     *
     * @return 页面宽度，单位 pt
     */
    public double getWidthPt() {
        return widthPt;
    }

    /**
     * 获取页面高度
     *
     * @return 页面高度，单位 pt
     */
    public double getHeightPt() {
        return heightPt;
    }
}
