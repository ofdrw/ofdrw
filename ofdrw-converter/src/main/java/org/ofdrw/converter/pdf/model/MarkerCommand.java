package org.ofdrw.converter.pdf.model;

/**
 * 图元 ID 标记指令
 * <p>
 * 不产生任何 PDF 操作符，仅用于在指令序列中标注“以下指令来自哪个 OFD 图元”，
 * 便于渲染器无关的单元测试定位断言位置，以及在告警日志中回溯出错图元（设计约束 3、4）。
 * <p>
 * 写入器必须忽略本指令，或仅输出注释行（以 {@code %} 开头）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class MarkerCommand extends AbstractCommand {

    /**
     * 标记文本，通常为 OFD 图元 ID 或说明
     */
    private final String label;

    /**
     * 构造标记指令
     *
     * @param label 标记文本，不能为空
     */
    public MarkerCommand(String label) {
        super(CoordSpace.PAGE);
        if (label == null || label.isEmpty()) {
            throw new IllegalArgumentException("标记文本不能为空");
        }
        this.label = label;
    }

    /**
     * @return 标记文本
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return "% " + label;
    }
}
