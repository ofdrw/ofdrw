package org.ofdrw.converter.pdf.model;

/**
 * 恢复图形状态，对应 PDF 操作符 {@code Q}
 * <p>
 * 必须与 {@link SaveState} 成对出现（设计约束 1）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class RestoreState extends AbstractCommand {

    /**
     * 指令实例（无状态）
     */
    public static final RestoreState INSTANCE = new RestoreState();

    private RestoreState() {
        super(CoordSpace.PAGE);
    }

    @Override
    public String toString() {
        return "Q";
    }
}
