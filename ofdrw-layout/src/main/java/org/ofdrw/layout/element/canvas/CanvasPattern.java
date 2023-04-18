package org.ofdrw.layout.element.canvas;

import org.ofdrw.core.basicStructure.pageObj.layer.block.ImageObject;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.pageDescription.color.pattern.CT_Pattern;
import org.ofdrw.core.pageDescription.color.pattern.CellContent;
import org.ofdrw.core.pageDescription.color.pattern.ReflectMethod;

import java.awt.geom.AffineTransform;
import java.nio.file.Path;

/**
 * Canvas填充模式
 *
 * @author 权观宇
 * @since 2023-4-12 19:01:01
 */
public class CanvasPattern {

    /**
     * OFD底纹
     */
    public final CT_Pattern pattern;
    /**
     * 图片路径
     */
    public final Path img;

    /**
     * 创建一个Canvas底纹
     *
     * @param img        底纹图片路径
     * @param repetition 重复样式，支持 repeat == normal、column、row、row-column
     * @param imgObj     OFD图片对象
     */
    public CanvasPattern(Path img, String repetition, ImageObject imgObj) {
        this.pattern = new CT_Pattern();
        this.img = img.toAbsolutePath();

        /*
         * "repeat" == "normal" 支持
         * "repeat-x" 不支持
         * "repeat-y" 不支持
         * "no-repeat" 不支持
         */
        switch (repetition.toLowerCase()) {
            case "repeat":
            case "":
                // 默认值就是 normal所以不需要设置
                break;
            case "column":
                pattern.setReflectMethod(ReflectMethod.Column);
                break;
            case "row":
                pattern.setReflectMethod(ReflectMethod.Row);
                break;
            case "row-column":
                pattern.setReflectMethod(ReflectMethod.RowAndColumn);
                break;
            default:
                throw new IllegalArgumentException("不支持的重复样式：" + repetition);
        }
        CellContent cellContent = new CellContent();
        cellContent.addPageBlock(imgObj);
        this.pattern.setCellContent(cellContent);

        ST_Box boundary = imgObj.getBoundary();
        this.pattern.setWidth(boundary.getWidth());
        this.pattern.setHeight(boundary.getHeight());
    }


    /**
     * 设置底纹单元变换矩阵
     * 改变换矩阵将应用于底纹单元，以便将其映射到目标区域。
     *
     * @param matrix 变换矩阵，按照 [a, b, c, d, e, f] 顺序6个参数。
     */
    public void setTransform(double[] matrix) {
        if (matrix == null || matrix.length < 6) {
            throw new IllegalArgumentException("矩阵参数错误");
        }
        ST_Array ctm = new ST_Array(matrix[0], matrix[1], matrix[2], matrix[3], matrix[4], matrix[5]);
        pattern.setCTM(ctm);
    }

    /**
     * 设置底纹单元变换矩阵
     * 改变换矩阵将应用于底纹单元，以便将其映射到目标区域。
     *
     * @param tx 变换矩阵
     */
    public void setTransform(AffineTransform tx) {
        if (tx == null) {
            throw new IllegalArgumentException("矩阵参数错误");
        }
        /*
             m00 m10 0    a b 0
             m01 m11 0  = c d 0
             m02 m12 1    e f 1
        */
        ST_Array ctm = new ST_Array(tx.getScaleX(), tx.getShearY(), tx.getShearX(), tx.getScaleY(), tx.getTranslateX(), tx.getTranslateY());
        pattern.setCTM(ctm);
    }

}
