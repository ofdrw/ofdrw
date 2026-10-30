package org.ofdrw.converter.pdf.model;

/**
 * 叠加当前变换矩阵，对应 PDF 操作符 {@code cm}
 * <p>
 * 注意：本指令是显式写入的 CTM 叠加，设计 3.1 节禁止用页面级 Y 翻转矩阵代替逐点换算；
 * 仅在图元需要保留局部对象空间（例如表单 XObject）时使用。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ConcatMatrix extends AbstractCommand {

    /**
     * 待叠加的变换矩阵
     */
    private final AffineMatrix matrix;

    /**
     * 构造 CTM 叠加指令
     *
     * @param matrix 待叠加的变换矩阵，不能为空
     */
    public ConcatMatrix(AffineMatrix matrix) {
        super(CoordSpace.PAGE);
        if (matrix == null) {
            throw new IllegalArgumentException("变换矩阵不能为空");
        }
        this.matrix = matrix;
    }

    /**
     * @return 待叠加的变换矩阵
     */
    public AffineMatrix getMatrix() {
        return matrix;
    }

    @Override
    public String toString() {
        return "cm " + matrix;
    }
}
