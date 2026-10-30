package org.ofdrw.converter.pdf.model;

import java.util.Arrays;

/**
 * 中间模型仿射矩阵 {@code [a b c d e f]}
 * <p>
 * 采用与 OFD（8.1.5）和 PDF 一致的行向量写法：{@code [x' y' 1] = [x y 1] · M}，即
 * <pre>
 *     x' = a·x + c·y + e
 *     y' = b·x + d·y + f
 * </pre>
 * <b>本类不包含任何 Y 轴翻转</b>：所有与页面方向相关的换算必须通过 {@link org.ofdrw.converter.pdf.CoordSystem}，
 * 否则旋转与镜像方向会相反（设计 3.1 节）。
 * <p>
 * 本类为不可变对象。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class AffineMatrix {

    /**
     * 单位矩阵
     */
    public static final AffineMatrix IDENTITY = new AffineMatrix(1, 0, 0, 1, 0, 0);

    /**
     * 矩阵分量 a
     */
    private final double a;

    /**
     * 矩阵分量 b
     */
    private final double b;

    /**
     * 矩阵分量 c
     */
    private final double c;

    /**
     * 矩阵分量 d
     */
    private final double d;

    /**
     * 横向平移量 e，单位 pt
     */
    private final double e;

    /**
     * 纵向平移量 f，单位 pt
     */
    private final double f;

    /**
     * 构造矩阵
     *
     * @param a 矩阵分量 a
     * @param b 矩阵分量 b
     * @param c 矩阵分量 c
     * @param d 矩阵分量 d
     * @param e 横向平移量，单位 pt
     * @param f 纵向平移量，单位 pt
     */
    public AffineMatrix(double a, double b, double c, double d, double e, double f) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
        this.e = e;
        this.f = f;
    }

    /**
     * @return 矩阵分量 a
     */
    public double getA() {
        return a;
    }

    /**
     * @return 矩阵分量 b
     */
    public double getB() {
        return b;
    }

    /**
     * @return 矩阵分量 c
     */
    public double getC() {
        return c;
    }

    /**
     * @return 矩阵分量 d
     */
    public double getD() {
        return d;
    }

    /**
     * @return 横向平移量，单位 pt
     */
    public double getE() {
        return e;
    }

    /**
     * @return 纵向平移量，单位 pt
     */
    public double getF() {
        return f;
    }

    /**
     * 平移矩阵
     *
     * @param tx 横向平移量，单位 pt
     * @param ty 纵向平移量，单位 pt
     * @return 平移矩阵
     */
    public static AffineMatrix translate(double tx, double ty) {
        return new AffineMatrix(1, 0, 0, 1, tx, ty);
    }

    /**
     * 缩放矩阵
     *
     * @param sx 横向缩放比例
     * @param sy 纵向缩放比例
     * @return 缩放矩阵
     */
    public static AffineMatrix scale(double sx, double sy) {
        return new AffineMatrix(sx, 0, 0, sy, 0, 0);
    }

    /**
     * 计算本矩阵左乘 {@code other} 的结果
     * <p>
     * 语义：先应用本矩阵，再应用 {@code other}，即 {@code (P · M) · N = P · (M · N)}。
     * 常用于 CTM 叠加：{@code ctm.multiply(childCtm)}。
     *
     * @param other 右乘矩阵，不能为空
     * @return 矩阵乘积
     */
    public AffineMatrix multiply(AffineMatrix other) {
        if (other == null) {
            throw new IllegalArgumentException("右乘矩阵不能为空");
        }
        return new AffineMatrix(
                a * other.a + b * other.c,
                a * other.b + b * other.d,
                c * other.a + d * other.c,
                c * other.b + d * other.d,
                e * other.a + f * other.c + other.e,
                e * other.b + f * other.d + other.f);
    }

    /**
     * 变换一个点
     *
     * @param x 输入横坐标
     * @param y 输入纵坐标
     * @return 变换后的坐标 {@code [x', y']}
     */
    public double[] transform(double x, double y) {
        return new double[]{
                a * x + c * y + e,
                b * x + d * y + f
        };
    }

    /**
     * 获取分量数组
     *
     * @return 分量数组 {@code [a, b, c, d, e, f]}
     */
    public double[] toArray() {
        return new double[]{a, b, c, d, e, f};
    }

    /**
     * 是否为仿射矩阵（行列式不为 0）
     *
     * @return true 表示矩阵可逆
     */
    public boolean isInvertible() {
        return Math.abs(a * d - b * c) > 1e-12;
    }

    /**
     * 求逆矩阵
     *
     * @return 逆矩阵
     * @throws IllegalStateException 矩阵不可逆时抛出
     */
    public AffineMatrix inverse() {
        double det = a * d - b * c;
        if (Math.abs(det) <= 1e-12) {
            throw new IllegalStateException("矩阵不可逆: " + this);
        }
        double ia = d / det;
        double ib = -b / det;
        double ic = -c / det;
        double id = a / det;
        double ie = -(e * ia + f * ic);
        double ifv = -(e * ib + f * id);
        return new AffineMatrix(ia, ib, ic, id, ie, ifv);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AffineMatrix)) {
            return false;
        }
        AffineMatrix other = (AffineMatrix) o;
        return Double.compare(a, other.a) == 0
                && Double.compare(b, other.b) == 0
                && Double.compare(c, other.c) == 0
                && Double.compare(d, other.d) == 0
                && Double.compare(e, other.e) == 0
                && Double.compare(f, other.f) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(toArray());
    }

    @Override
    public String toString() {
        return "[" + a + " " + b + " " + c + " " + d + " " + e + " " + f + "]";
    }
}
