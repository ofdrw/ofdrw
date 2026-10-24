package org.ofdrw.converter.renderer.command;

/**
 * 平移坐标系的命令。
 */
public final class TranslateCommand extends DrawCommand {

    /**
     * 水平平移量，单位：像素
     */
    private final double tx;

    /**
     * 垂直平移量，单位：像素
     */
    private final double ty;

    /**
     * 构造平移命令。
     *
     * @param tx 水平平移量，单位：像素
     * @param ty 垂直平移量，单位：像素
     */
    public TranslateCommand(double tx, double ty) {
        this.tx = tx;
        this.ty = ty;
    }

    /**
     * 获取水平平移量。
     *
     * @return 水平平移量，单位：像素
     */
    public double getTx() {
        return tx;
    }

    /**
     * 获取垂直平移量。
     *
     * @return 垂直平移量，单位：像素
     */
    public double getTy() {
        return ty;
    }

    /**
     * 获取命令对包围盒的坐标贡献。
     *
     * @return 空数组，变换命令不直接产生几何
     */
    @Override
    public double[][] getBounds() {
        return NO_BOUNDS;
    }

    @Override
    public String toString() {
        return "Translate(" + tx + ", " + ty + ")";
    }
}
