package org.ofdrw.converter.pdf.model;

/**
 * 保存图形状态，对应 PDF 操作符 {@code q}
 * <p>
 * 设计约束 1：所有 q/Q 必须成对，图形状态必须显式写入，不依赖内容流的隐式状态。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class SaveState extends AbstractCommand {

    /**
     * 指令实例（无状态）
     */
    public static final SaveState INSTANCE = new SaveState();

    private SaveState() {
        super(CoordSpace.PAGE);
    }

    @Override
    public String toString() {
        return "q";
    }
}
