package org.ofdrw.converter;

import org.apache.pdfbox.util.Matrix;

import java.util.ArrayList;
import java.util.List;

/**
 * 矩阵分解
 *
 * @author 权观宇
 * @since 2025-7-18 09:57:29
 */
public class MatrixDecomposer {


    /**
     * 将矩阵分解为平移（T）、旋转（Q）、缩放（S）、斜切（K）四个矩阵的乘积
     *
     * @param matrix 矩阵
     * @return 分解后的矩阵列表 [T, Q, S, K]
     */
    public static List<Matrix> decompose(Matrix matrix) {
        // 1. 提取平移分量
        double e = matrix.getTranslateX();
        double f = matrix.getTranslateY();
        Matrix T = new Matrix(1, 0, 0, 1, (float) e, (float) f); // 平移矩阵
        // 2. 提取线性部分 (a, b, c, d)
        double a = matrix.getScaleX();
        double b = matrix.getShearY();
        double c = matrix.getShearX();
        double d = matrix.getScaleY();
        return decompose(a, b, c, d, e, f);
    }

    /**
     * 将矩阵分解为平移（T）、旋转（Q）、缩放（S）、斜切（K）四个矩阵的乘积
     *
     * @param a 矩阵元素 a
     * @param b 矩阵元素 b
     * @param c 矩阵元素 c
     * @param d 矩阵元素 d
     * @param e 矩阵元素 e
     * @param f 矩阵元素 f
     * @return 分解后的矩阵列表 [T, Q, S, K]
     */
    public static List<Matrix> decompose(double a, double b, double c, double d, double e, double f) {
        // 1. 提取平移分量
        Matrix T = new Matrix(1, 0, 0, 1, (float) e, (float) f); // 平移矩阵

        // 3. QR分解: 对2x2矩阵 [a, b; c, d] 进行分解
        double r11 = Math.sqrt(a * a + b * b);
        if (Math.abs(r11) < 1e-10) {
            throw new IllegalArgumentException("矩阵奇异，无法分解");
        }
        double cosTheta = a / r11;
        double sinTheta = b / r11;
        // 计算R矩阵的元素
        double r12 = (a * c + b * d) / r11;
        double r22 = (a * d - b * c) / r11;
        // 4. 构建旋转矩阵Q (注意PDFBox的矩阵参数顺序)
        Matrix Q = new Matrix( (float)cosTheta, (float) sinTheta,  (float)-sinTheta,  (float)cosTheta, 0, 0);
        // 5. 缩放矩阵S (sx = r11, sy = r22)
        double sx = r11;
        double sy = r22;
        Matrix S = new Matrix( (float)sx, 0, 0, (float) sy, 0, 0);
        // 6. 斜切矩阵K (k = r12 / r11)
        double k = r12 / r11;
        Matrix K = new Matrix(1, 0,  (float)k, 1, 0, 0); // 水平斜切
        // 7. 按正确应用顺序返回矩阵: K(斜切) -> S(缩放) -> Q(旋转) -> T(平移)
        List<Matrix> result = new ArrayList<>();
        result.add(K);  // 最先应用
        result.add(S);
        result.add(Q);
        result.add(T);  // 最后应用

        return result;
    }
}
