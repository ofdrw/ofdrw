package org.ofdrw.converter.pdf.render;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.Geometry;

/**
 * 圆弧转换器：OFD 路径命令 {@code A}（椭圆弧）→ 三次贝塞尔曲线段
 * <p>
 * OFD 文档表 36/42 的 A 操作符参数为 {@code rx ry angle large-arc-flag sweep-flag x y}（与 SVG 相同），
 * 起点取路径当前点；{@code angle} 为椭圆旋转角（正值为顺时针，单位度），
 * {@code large-arc-flag} 为 1 表示大弧，{@code sweep-flag} 为 1 表示沿角度增大方向（X 向右 Y 向下时为顺时针）。
 * <p>
 * 中间模型只表达线段与三次贝塞尔曲线，因此圆弧必须拆分为圆心角不超过 {@link #MAX_SWEEP_PER_CURVE} 的贝塞尔段。
 * 几何换算由与库无关的 {@link Geometry} 完成，本类只负责“圆心参数 → 三次贝塞尔控制点”。
 * <p>
 * 坐标约定：输入点是<b>已换算为 PDF 页面坐标（pt，Y 轴向上）</b>的端点；Y 翻转会同时改变旋转角符号与方向语义，
 * 因此先把输入点逆变换回 OFD 方向，在 OFD 语义下讨论半径、旋转角与方向标志，再把结果正变换回 PDF 坐标。
 * 半径按 {@code toPage} 的线性缩放比换算为页面 pt 后参与计算。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class ArcConverter {

    /**
     * 单段贝塞尔拟合的最大圆心角，单位弧度
     */
    public static final double MAX_SWEEP_PER_CURVE = Math.PI / 2;

    /**
     * 三次贝塞尔拟合单位圆弧的控制点系数：{@code k = 4/3 · tan(θ/4)}，θ 为圆心角，单位弧度
     */
    private static final double KAPPA_FACTOR = 4.0 / 3.0;

    /**
     * 角度转弧度的换算系数
     */
    private static final double DEG_TO_RAD = Math.PI / 180.0;

    private ArcConverter() {
    }

    /**
     * 圆弧参数
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class ArcParams {

        /**
         * 圆心横坐标，单位 pt（PDF 页面坐标）
         */
        private final double cx;

        /**
         * 圆心纵坐标，单位 pt（PDF 页面坐标）
         */
        private final double cy;

        /**
         * 椭圆横半径，单位 pt
         */
        private final double rx;

        /**
         * 椭圆纵半径，单位 pt
         */
        private final double ry;

        /**
         * 椭圆旋转角，单位弧度
         */
        private final double rotation;

        /**
         * 起始角，单位弧度
         */
        private final double startAngle;

        /**
         * 圆心角，单位弧度，正数表示逆时针（PDF 坐标系）
         */
        private final double sweepAngle;

        /**
         * 构造圆弧参数
         *
         * @param cx         圆心横坐标，单位 pt（PDF 页面坐标）
         * @param cy         圆心纵坐标，单位 pt（PDF 页面坐标）
         * @param rx         椭圆横半径，单位 pt，必须大于 0
         * @param ry         椭圆纵半径，单位 pt，必须大于 0
         * @param rotation   椭圆旋转角，单位弧度
         * @param startAngle 起始角，单位弧度
         * @param sweepAngle 圆心角，单位弧度，正数表示逆时针（PDF 坐标系）
         */
        public ArcParams(double cx, double cy, double rx, double ry, double rotation, double startAngle, double sweepAngle) {
            if (!(rx > 0) || !(ry > 0)) {
                throw new IllegalArgumentException("圆弧半径必须大于 0: rx=" + rx + ", ry=" + ry);
            }
            this.cx = cx;
            this.cy = cy;
            this.rx = rx;
            this.ry = ry;
            this.rotation = rotation;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
        }

        /**
         * @return 圆心横坐标，单位 pt
         */
        public double getCx() {
            return cx;
        }

        /**
         * @return 圆心纵坐标，单位 pt
         */
        public double getCy() {
            return cy;
        }

        /**
         * @return 椭圆横半径，单位 pt
         */
        public double getRx() {
            return rx;
        }

        /**
         * @return 椭圆纵半径，单位 pt
         */
        public double getRy() {
            return ry;
        }

        /**
         * @return 椭圆旋转角，单位弧度
         */
        public double getRotation() {
            return rotation;
        }

        /**
         * @return 起始角，单位弧度
         */
        public double getStartAngle() {
            return startAngle;
        }

        /**
         * @return 圆心角，单位弧度
         */
        public double getSweepAngle() {
            return sweepAngle;
        }
    }

    /**
     * OFD {@code A} 操作符（端点参数）→ 三次贝塞尔曲线段
     * <p>
     * {@code start}/{@code end} 必须是 {@code toPage} 作用<b>之后</b>的 PDF 页面坐标（pt）；
     * {@code rx}/{@code ry} 使用 OFD 原始半径（单位与 {@code toPage} 输入坐标一致，通常为 mm），
     * 内部按 {@code toPage} 的线性缩放比换算为页面 pt。起点与终点重合或半径非正时退化为一段直线。
     *
     * @param startX 路径当前点横坐标，单位 pt（PDF 页面坐标）
     * @param startY 路径当前点纵坐标，单位 pt（PDF 页面坐标）
     * @param rx     椭圆横半径，与 {@code toPage} 输入坐标同单位（通常 mm），内部取绝对值
     * @param ry     椭圆纵半径，与 {@code toPage} 输入坐标同单位（通常 mm），内部取绝对值
     * @param angle  椭圆旋转角，单位度，正值为顺时针（OFD 语义）
     * @param large  大弧标志（{@code true} 表示圆心角大于 180°）
     * @param sweep  方向标志（{@code true} 表示由起点到终点为顺时针，OFD 语义）
     * @param endX   A 操作符终点横坐标，单位 pt（PDF 页面坐标）
     * @param endY   A 操作符终点纵坐标，单位 pt（PDF 页面坐标）
     * @param toPage 对象空间 → PDF 页面坐标的矩阵，用于把输入点还原到 OFD 方向并换算半径；为空按单位矩阵
     * @return 三次贝塞尔段，按绘制顺序排列；退化时返回单段直线（控制点与端点重合）
     * @throws IllegalArgumentException 坐标或半径包含非有限数值时
     */
    public static List<CurveSegment> toCubicSegments(double startX, double startY,
                                                     double rx, double ry, double angle,
                                                     boolean large, boolean sweep,
                                                     double endX, double endY,
                                                     AffineMatrix toPage) {
        assertFinite(startX, startY, rx, ry, angle, endX, endY);
        List<CurveSegment> result = new ArrayList<>(2);
        if (!(Math.abs(rx) > Geometry.EPSILON) || !(Math.abs(ry) > Geometry.EPSILON)
                || samePoint(startX, startY, endX, endY)) {
            result.add(new CurveSegment(startX, startY, endX, endY, endX, endY));
            return result;
        }
        AffineMatrix objectToPage = toPage == null ? AffineMatrix.IDENTITY : toPage;
        AffineMatrix pageToObject = objectToPage.isInvertible() ? objectToPage.inverse() : AffineMatrix.IDENTITY;
        boolean identity = AffineMatrix.IDENTITY.equals(objectToPage);
        double[] s = pageToObject.transform(startX, startY);
        double[] e = pageToObject.transform(endX, endY);
        // 对象空间 → 页面坐标的线性缩放比（mm→pt 的比例），用于把原始半径换算到页面 pt
        double scale = Math.hypot(objectToPage.getA(), objectToPage.getB());
        if (!(scale > 0)) {
            scale = 1;
        }
        Geometry.EllipseArc ellipse = new Geometry.EllipseArc(s[0], s[1], rx * scale, ry * scale,
                angle * DEG_TO_RAD, large, sweep, e[0], e[1]);
        Geometry.CenterArc center = Geometry.arcToCenter(ellipse);
        if (center == null) {
            result.add(new CurveSegment(startX, startY, endX, endY, endX, endY));
            return result;
        }
        for (Geometry.CenterArc unit : Geometry.splitIntoUnitArcs(center)) {
            CurveSegment segment = unitArcToCubic(unit);
            result.add(identity ? segment : segment.transform(objectToPage));
        }
        return result;
    }

    /**
     * 圆心参数圆弧 → 三次贝塞尔曲线段
     * <p>
     * 输入坐标必须处于<b>同一个坐标空间</b>（本实现统一为 PDF 页面坐标，单位 pt，Y 轴向上）。
     * 结果中的每个点同样处于该空间，可直接装入 {@link CurveTo}。
     *
     * @param arc 圆弧参数，不能为空
     * @return 三次贝塞尔段，按绘制顺序排列；圆心角为 0 时返回空列表
     */
    public static List<CurveSegment> toCubicSegments(ArcParams arc) {
        if (arc == null) {
            throw new IllegalArgumentException("圆弧参数不能为空");
        }
        if (Math.abs(arc.sweepAngle) < Geometry.EPSILON) {
            return Collections.emptyList();
        }
        double endAngle = arc.startAngle + arc.sweepAngle;
        Geometry.CenterArc center = new Geometry.CenterArc(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation,
                arc.startAngle, arc.sweepAngle,
                Geometry.pointOnEllipse(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation, endAngle, 0),
                Geometry.pointOnEllipse(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation, endAngle, 1));
        List<CurveSegment> result = new ArrayList<>(2);
        for (Geometry.CenterArc unit : Geometry.splitIntoUnitArcs(center)) {
            result.add(unitArcToCubic(unit));
        }
        return result;
    }

    /**
     * 把圆心角不超过 90 度的单位圆弧转换为单段三次贝塞尔曲线
     * <p>
     * 控制点位于端点切线方向上，距端点的距离为 {@code k = 4/3 · tan(θ/4)} 倍的对应半径，θ 为该段圆心角。
     *
     * @param arc 圆心参数圆弧，圆心角绝对值不得超过 {@link #MAX_SWEEP_PER_CURVE}
     * @return 三次贝塞尔曲线段
     */
    static CurveSegment unitArcToCubic(Geometry.CenterArc arc) {
        double theta = arc.getSweepAngle();
        if (Math.abs(theta) > MAX_SWEEP_PER_CURVE + 1e-6) {
            throw new IllegalArgumentException("单位圆弧的圆心角不得超过 90 度：" + theta);
        }
        double k = KAPPA_FACTOR * Math.tan(theta / 4);
        double cos = Math.cos(arc.getRotation());
        double sin = Math.sin(arc.getRotation());
        // 起点、终点在椭圆未旋转坐标系下的参数角
        double a0 = arc.getStartAngle();
        double a1 = a0 + theta;
        double x0 = arc.getRx() * Math.cos(a0);
        double y0 = arc.getRy() * Math.sin(a0);
        double x1 = arc.getRx() * Math.cos(a1);
        double y1 = arc.getRy() * Math.sin(a1);
        // 切线方向为参数方程的导数：(-rx·sinθ, ry·cosθ)
        double tx0 = -arc.getRx() * Math.sin(a0);
        double ty0 = arc.getRy() * Math.cos(a0);
        double tx1 = -arc.getRx() * Math.sin(a1);
        double ty1 = arc.getRy() * Math.cos(a1);
        double c1x = x0 + k * tx0;
        double c1y = y0 + k * ty0;
        double c2x = x1 - k * tx1;
        double c2y = y1 - k * ty1;
        return new CurveSegment(
                arc.getCx() + cos * c1x - sin * c1y,
                arc.getCy() + sin * c1x + cos * c1y,
                arc.getCx() + cos * c2x - sin * c2y,
                arc.getCy() + sin * c2x + cos * c2y,
                arc.getEndX(),
                arc.getEndY());
    }

    /**
     * 校验数值均为有限值
     *
     * @param values 待校验数值
     * @throws IllegalArgumentException 存在 NaN 或无穷时
     */
    private static void assertFinite(double... values) {
        for (double value : values) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                throw new IllegalArgumentException("圆弧参数包含非有限数值：" + value);
            }
        }
    }

    /**
     * 判断两点是否重合
     *
     * @param x1 第一个点横坐标
     * @param y1 第一个点纵坐标
     * @param x2 第二个点横坐标
     * @param y2 第二个点纵坐标
     * @return {@code true} 表示重合
     */
    private static boolean samePoint(double x1, double y1, double x2, double y2) {
        return Math.abs(x1 - x2) < Geometry.EPSILON && Math.abs(y1 - y2) < Geometry.EPSILON;
    }

    /**
     * 圆弧中的一段三次贝塞尔曲线
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class CurveSegment {

        /**
         * 第一个控制点横坐标，单位 pt
         */
        private final double x1;

        /**
         * 第一个控制点纵坐标，单位 pt
         */
        private final double y1;

        /**
         * 第二个控制点横坐标，单位 pt
         */
        private final double x2;

        /**
         * 第二个控制点纵坐标，单位 pt
         */
        private final double y2;

        /**
         * 终点横坐标，单位 pt
         */
        private final double x3;

        /**
         * 终点纵坐标，单位 pt
         */
        private final double y3;

        /**
         * 构造贝塞尔曲线段
         *
         * @param x1 第一个控制点横坐标，单位 pt
         * @param y1 第一个控制点纵坐标，单位 pt
         * @param x2 第二个控制点横坐标，单位 pt
         * @param y2 第二个控制点纵坐标，单位 pt
         * @param x3 终点横坐标，单位 pt
         * @param y3 终点纵坐标，单位 pt
         */
        public CurveSegment(double x1, double y1, double x2, double y2, double x3, double y3) {
            this.x1 = x1;
            this.y1 = y1;
            this.x2 = x2;
            this.y2 = y2;
            this.x3 = x3;
            this.y3 = y3;
        }

        /**
         * 把曲线段按矩阵变换后返回新段
         *
         * @param matrix 变换矩阵，不能为空
         * @return 变换后的曲线段
         */
        public CurveSegment transform(AffineMatrix matrix) {
            double[] p1 = matrix.transform(x1, y1);
            double[] p2 = matrix.transform(x2, y2);
            double[] p3 = matrix.transform(x3, y3);
            return new CurveSegment(p1[0], p1[1], p2[0], p2[1], p3[0], p3[1]);
        }

        /**
         * @return 该段对应的中间模型路径段 {@link CurveTo}
         */
        public CurveTo toPathSegment() {
            return new CurveTo(x1, y1, x2, y2, x3, y3);
        }

        /**
         * @return 第一个控制点横坐标，单位 pt
         */
        public double getX1() {
            return x1;
        }

        /**
         * @return 第一个控制点纵坐标，单位 pt
         */
        public double getY1() {
            return y1;
        }

        /**
         * @return 第二个控制点横坐标，单位 pt
         */
        public double getX2() {
            return x2;
        }

        /**
         * @return 第二个控制点纵坐标，单位 pt
         */
        public double getY2() {
            return y2;
        }

        /**
         * @return 终点横坐标，单位 pt
         */
        public double getX3() {
            return x3;
        }

        /**
         * @return 终点纵坐标，单位 pt
         */
        public double getY3() {
            return y3;
        }
    }
}
