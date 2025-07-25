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
        Matrix Q = new Matrix((float) cosTheta, (float) sinTheta, (float) -sinTheta, (float) cosTheta, 0, 0);
        // 5. 缩放矩阵S (sx = r11, sy = r22)
        double sx = r11;
        double sy = r22;
        Matrix S = new Matrix((float) sx, 0, 0, (float) sy, 0, 0);
        // 6. 斜切矩阵K (k = r12 / r11)
        double k = r12 / r11;
        Matrix K = new Matrix(1, 0, (float) k, 1, 0, 0); // 水平斜切
        // 7. 按正确应用顺序返回矩阵: K(斜切) -> S(缩放) -> Q(旋转) -> T(平移)
        List<Matrix> result = new ArrayList<>();
        result.add(K);  // 最先应用
        result.add(S);
        result.add(Q);
        result.add(T);  // 最后应用

        return result;
    }

    /**
     * 将OFD的矩阵转换为PDF的矩阵
     *
     * @param ofdPageHeight OFD页面高度
     * @param a             OFD矩阵元素 a
     * @param b             OFD矩阵元素 b
     * @param c             OFD矩阵元素 c
     * @param d             OFD矩阵元素 d
     * @param e             OFD矩阵元素 e
     * @param f             OFD矩阵元素 f
     * @return PDF的矩阵
     */
    public static List<Matrix> ofd2pdfDecompose(double ofdPageHeight, double a, double b, double c, double d, double e, double f) {
        // 拆分OFD矩阵
        List<Matrix> matrixs = decompose(a, b, c, d, e, f);
        // 斜切矩阵
        Matrix k = matrixs.get(0);
        // 缩放矩阵
        Matrix s = matrixs.get(1);
        // 旋转矩阵
        Matrix q = matrixs.get(2);
        // 平移矩阵
        Matrix t = matrixs.get(3);

        System.out.println("分解结果:");
        System.out.println("K (斜切): " + k);
        System.out.println("S (缩放): " + s);
        System.out.println("Q (旋转): " + q);
        System.out.println("T (平移): " + t);

        // 处理平移变换：
        // OFD与PDF坐标Y轴方向相反，平移时相对于页面进行的因此需要通过页面高度反向计算在PDF中的位置
        t = new Matrix(
                1f, 0f, 0f, 1f, t.getTranslateX(), ((float) ofdPageHeight) - t.getTranslateY()
        );


        // 处理旋转变换：
        // 创建反向旋转矩阵 (转置矩阵，因为旋转矩阵是正交的)
        q = new Matrix(q.getScaleX(),
                -q.getShearY(),
                -q.getShearX(),
                q.getScaleY(), 0, 0);

        // 处理缩放变换：不需要处理

        // 处理斜切变换：
        // y方向的斜切需要反向处理
        k = new Matrix(1f, k.getShearX(), -k.getScaleY(), 1f, 0f, 0f);


        System.out.println("变换结果:");
        System.out.println("K (斜切): " + k);
        System.out.println("S (缩放): " + s);
        System.out.println("Q (旋转): " + q);
        System.out.println("T (平移): " + t);

        matrixs.set(0, k);
        matrixs.set(1, s);
        matrixs.set(2, q);
        matrixs.set(3, t);

        return matrixs;
    }

//
//    public static Matrix conv(double h) {
//        // OFD到PDF的缩放系数 (1 OFD单位 = 72 PDF点)
//        final float scale =1 * 72f / 25.4f;
//
//        // 计算PDF页面尺寸（点单位）
//        float pdfHeight = (float) h * scale;
//
//        /* 变换矩阵分解:
//         * 1. [a, b, c] = [scale, 0, 0]         → X轴缩放
//         * 2. [d, e, f] = [0, -scale, pdfHeight] → Y轴翻转并下移页面高度
//         *
//         * 完整矩阵:
//         * [ scale    0       0    ]
//         * [ 0      -scale   h*72 ]   (其中 h*72 = pdfHeight)
//         * [ 0       0       1    ]
//         */
//        return new Matrix(
//                scale,  // a: X轴缩放
//                0,      // b: 无X-Y剪切
//                0,      // c: 无X轴平移
//                -scale, // d: Y轴翻转缩放
//                pdfHeight, // e: Y轴平移补偿 (将内容下移页面高度)
//                0       // f: 预留参数(实际未使用)
//        );
//    }
}
