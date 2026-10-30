package org.ofdrw.converter.pdf.model;

/**
 * 内容指令基类：仅携带坐标作用域
 * <p>
 * 所有具体指令都默认使用 {@link CoordSpace#PAGE}，需要以对象空间坐标描述时通过构造器显式指定。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public abstract class AbstractCommand implements ContentCommand {

    /**
     * 坐标作用域
     */
    private final CoordSpace coordSpace;

    /**
     * 构造指令
     *
     * @param coordSpace 坐标作用域，为空时按 {@link CoordSpace#PAGE} 处理
     */
    protected AbstractCommand(CoordSpace coordSpace) {
        this.coordSpace = coordSpace == null ? CoordSpace.PAGE : coordSpace;
    }

    @Override
    public CoordSpace getCoordSpace() {
        return coordSpace;
    }
}
