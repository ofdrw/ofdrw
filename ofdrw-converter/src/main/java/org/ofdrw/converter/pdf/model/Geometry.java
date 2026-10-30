package org.ofdrw.converter.pdf.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 椭圆弧几何换算（端点参数 → 圆心参数 → 单位圆弧）
 * <p>
 * 与 {@link PathSegment} 一样，本类只做纯数学运算，不涉及任何渲染库类型。
 * <ul>
 *     <li>{@link #arcToCenter(EllipseArc)}：端点参数（半径、旋转角、大弧标志、方向标志、终点）→ 圆心参数；</li>
 *     <li>{@link #splitIntoUnitArcs(CenterArc)}：把圆心参数圆弧拆成圆心角不超过 90 度的单位圆弧，
 *     之后由渲染器对每段单位圆弧计算三次贝塞尔控制点。</li>
 * </ul>
 * 角度单位一律为弧度；方向约定：X 轴向右、Y 轴向下（OFD / SVG 屏幕坐标系）时圆心角增大即顺时针。
 * 变换到 Y 轴向上的 PDF 页面坐标后圆心角随 Y 翻转自动反向，因此算法无需区分坐标系。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class Geometry {

    /**
     * 判断角度/长度是否相等的默认容差
     */
    public static final double EPSILON = 1e-9;

    /**
     * 单位圆弧的最大圆心角，单位弧度
     */
    public static final double MAX_UNIT_ARC_ANGLE = Math.PI / 2;

    /**
     * 椭圆圆心求解时，分母小于该值判定为退化（按圆心在弦中点处理）
     */
    private static final double DEGENERATE_DENOMINATOR = 1e-12;

    private Geometry() {
    }

    /**
     * 端点参数椭圆弧（与 OFD {@code A} 操作符、SVG {@code A} 命令语义一致）
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class EllipseArc {

        /**
         * 起点横坐标，与半径处于同一坐标空间（渲染器统一使用 pt）
         */
        private final double startX;

        /**
         * 起点纵坐标，与半径处于同一坐标空间
         */
        private final double startY;

        /**
         * 椭圆横半径，与坐标同一单位（可能为负，计算时取绝对值）
         */
        private final double rx;

        /**
         * 椭圆纵半径，与坐标同一单位（可能为负，计算时取绝对值）
         */
        private final double ry;

        /**
         * 椭圆旋转角，单位弧度
         */
        private final double rotation;

        /**
         * 是否为大弧（圆心角大于 π）
         */
        private final boolean largeArc;

        /**
         * 方向标志：{@code true} 表示沿圆心角增大方向（X 向右 Y 向下时为顺时针）从起点走向终点
         */
        private final boolean sweep;

        /**
         * 终点横坐标
         */
        private final double endX;

        /**
         * 终点纵坐标
         */
        private final double endY;

        /**
         * 构造端点参数椭圆弧
         *
         * @param startX   起点横坐标，与半径同一坐标空间
         * @param startY   起点纵坐标，与半径同一坐标空间
         * @param rx       椭圆横半径，与坐标同一单位
         * @param ry       椭圆纵半径，与坐标同一单位
         * @param rotation 椭圆旋转角，单位弧度
         * @param largeArc 是否为大弧
         * @param sweep    方向标志，{@code true} 表示圆心角增大方向
         * @param endX     终点横坐标，与半径同一坐标空间
         * @param endY     终点纵坐标，与半径同一坐标空间
         */
        public EllipseArc(double startX, double startY, double rx, double ry, double rotation,
                          boolean largeArc, boolean sweep, double endX, double endY) {
            this.startX = startX;
            this.startY = startY;
            this.rx = rx;
            this.ry = ry;
            this.rotation = rotation;
            this.largeArc = largeArc;
            this.sweep = sweep;
            this.endX = endX;
            this.endY = endY;
        }

        /**
         * @return 起点横坐标
         */
        public double getStartX() {
            return startX;
        }

        /**
         * @return 起点纵坐标
         */
        public double getStartY() {
            return startY;
        }

        /**
         * @return 椭圆横半径（原始值，可能为负）
         */
        public double getRx() {
            return rx;
        }

        /**
         * @return 椭圆纵半径（原始值，可能为负）
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
         * @return 是否为大弧
         */
        public boolean isLargeArc() {
            return largeArc;
        }

        /**
         * @return 方向标志
         */
        public boolean isSweep() {
            return sweep;
        }

        /**
         * @return 终点横坐标
         */
        public double getEndX() {
            return endX;
        }

        /**
         * @return 终点纵坐标
         */
        public double getEndY() {
            return endY;
        }
    }

    /**
     * 圆心参数圆弧
     * <p>
     * 椭圆以圆心为原点、先绕原点旋转 {@link #getRotation()}、再按 {@code (rx·cosθ, ry·sinθ)} 取点。
     * 全部字段与输入处于同一坐标空间。
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class CenterArc {

        /**
         * 圆心横坐标
         */
        private final double cx;

        /**
         * 圆心纵坐标
         */
        private final double cy;

        /**
         * 椭圆横半径，正数
         */
        private final double rx;

        /**
         * 椭圆纵半径，正数
         */
        private final double ry;

        /**
         * 椭圆旋转角，单位弧度
         */
        private final double rotation;

        /**
         * 起始角（圆心参数角），单位弧度
         */
        private final double startAngle;

        /**
         * 圆心角，单位弧度；正数表示沿圆心角增大方向，取值 (-2π, 2π)
         */
        private final double sweepAngle;

        /**
         * 该段终点横坐标
         */
        private final double endX;

        /**
         * 该段终点纵坐标
         */
        private final double endY;

        /**
         * 构造圆心参数圆弧
         *
         * @param cx         圆心横坐标
         * @param cy         圆心纵坐标
         * @param rx         椭圆横半径，正数
         * @param ry         椭圆纵半径，正数
         * @param rotation   椭圆旋转角，单位弧度
         * @param startAngle 起始角，单位弧度
         * @param sweepAngle 圆心角，单位弧度，正数为圆心角增大方向
         * @param endX       该段终点横坐标
         * @param endY       该段终点纵坐标
         */
        public CenterArc(double cx, double cy, double rx, double ry, double rotation,
                         double startAngle, double sweepAngle, double endX, double endY) {
            this.cx = cx;
            this.cy = cy;
            this.rx = rx;
            this.ry = ry;
            this.rotation = rotation;
            this.startAngle = startAngle;
            this.sweepAngle = sweepAngle;
            this.endX = endX;
            this.endY = endY;
        }

        /**
         * @return 圆心横坐标
         */
        public double getCx() {
            return cx;
        }

        /**
         * @return 圆心纵坐标
         */
        public double getCy() {
            return cy;
        }

        /**
         * @return 椭圆横半径，正数
         */
        public double getRx() {
            return rx;
        }

        /**
         * @return 椭圆纵半径，正数
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

        /**
         * @return 该段终点横坐标
         */
        public double getEndX() {
            return endX;
        }

        /**
         * @return 该段终点纵坐标
         */
        public double getEndY() {
            return endY;
        }
    }

    /**
     * 端点参数 → 圆心参数（依据 SVG 1.1 附录 F.6.5，OFD 表 36/42 的 {@code A} 语义相同）
     * <p>
     * 处理规则：
     * <ol>
     *     <li>半径取绝对值；半径为 0 或起点与终点重合时返回 {@code null}，调用方应退化为直线或忽略；</li>
     *     <li>半径过小时按比例放大到刚好容纳起点与终点（规范要求的 λ 修正）；</li>
     *     <li>方向标志与大弧标志共同决定圆心角符号与大小。</li>
     * </ol>
     *
     * @param arc 端点参数圆弧，不能为空
     * @return 圆心参数圆弧；退化为直线（半径为 0、起点终点重合）时返回 {@code null}
     */
    public static CenterArc arcToCenter(EllipseArc arc) {
        if (arc == null) {
            throw new IllegalArgumentException("椭圆弧参数不能为空");
        }
        double rx = Math.abs(arc.rx);
        double ry = Math.abs(arc.ry);
        double x1 = arc.startX;
        double y1 = arc.startY;
        double x2 = arc.endX;
        double y2 = arc.endY;
        if (rx < EPSILON || ry < EPSILON) {
            return null;
        }
        if (Math.abs(x2 - x1) < EPSILON && Math.abs(y2 - y1) < EPSILON) {
            return null;
        }
        double cos = Math.cos(arc.rotation);
        double sin = Math.sin(arc.rotation);
        // 1) 起点、终点换算到椭圆未旋转坐标系
        double dx2 = (x1 - x2) / 2;
        double dy2 = (y1 - y2) / 2;
        double x1p = cos * dx2 + sin * dy2;
        double y1p = -sin * dx2 + cos * dy2;
        // 2) 半径过小修正（λ 放大）
        double lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
        if (lambda > 1) {
            double scale = Math.sqrt(lambda);
            rx *= scale;
            ry *= scale;
        }
        double rxq = rx * rx;
        double ryq = ry * ry;
        double num = rxq * ryq - rxq * y1p * y1p - ryq * x1p * x1p;
        double den = rxq * y1p * y1p + ryq * x1p * x1p;
        double coef = den < DEGENERATE_DENOMINATOR ? 0 : Math.sqrt(Math.max(0, num / den));
        if (arc.largeArc == arc.sweep) {
            coef = -coef;
        }
        // 3) 椭圆未旋转坐标系下的圆心，再旋转回原坐标系
        double cxp = coef * rx * y1p / ry;
        double cyp = -coef * ry * x1p / rx;
        double cx = cos * cxp - sin * cyp + (x1 + x2) / 2;
        double cy = sin * cxp + cos * cyp + (y1 + y2) / 2;
        // 4) 起始角与圆心角
        double ux = (x1p - cxp) / rx;
        double uy = (y1p - cyp) / ry;
        double vx = (-x1p - cxp) / rx;
        double vy = (-y1p - cyp) / ry;
        double start = Math.atan2(uy, ux);
        double delta = Math.atan2(ux * vy - uy * vx, ux * vx + uy * vy);
        if (!arc.sweep && delta > 0) {
            delta -= 2 * Math.PI;
        } else if (arc.sweep && delta < 0) {
            delta += 2 * Math.PI;
        }
        return new CenterArc(cx, cy, rx, ry, arc.rotation, start, delta, x2, y2);
    }

    /**
     * 把圆心参数圆弧拆成圆心角不超过 {@link #MAX_UNIT_ARC_ANGLE} 的单位圆弧
     * <p>
     * 每段不超过 90 度是三次贝塞尔拟合圆弧的常规精度要求。最后一段的终点强制取输入终点，
     * 避免累计误差导致后续路径不能闭合。
     *
     * @param arc 圆心参数圆弧，不能为空
     * @return 单位圆弧列表（按绘制顺序）；圆心角为 0 时返回空列表
     */
    public static List<CenterArc> splitIntoUnitArcs(CenterArc arc) {
        if (arc == null) {
            throw new IllegalArgumentException("圆弧参数不能为空");
        }
        List<CenterArc> result = new ArrayList<>();
        double sweep = arc.sweepAngle;
        if (Math.abs(sweep) < EPSILON) {
            return result;
        }
        int count = (int) Math.ceil(Math.abs(sweep) / MAX_UNIT_ARC_ANGLE - 1e-12);
        count = Math.max(1, count);
        double step = sweep / count;
        for (int i = 0; i < count; i++) {
            double startAngle = arc.startAngle + step * i;
            boolean last = i == count - 1;
            double endX;
            double endY;
            if (last) {
                endX = arc.endX;
                endY = arc.endY;
            } else {
                double endAngle = arc.startAngle + step * (i + 1);
                endX = pointOnEllipse(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation, endAngle, 0);
                endY = pointOnEllipse(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation, endAngle, 1);
            }
            result.add(new CenterArc(arc.cx, arc.cy, arc.rx, arc.ry, arc.rotation, startAngle, step, endX, endY));
        }
        return result;
    }

    /**
     * 计算椭圆上的一点（按圆心参数）
     *
     * @param cx       圆心横坐标
     * @param cy       圆心纵坐标
     * @param rx       椭圆横半径，正数
     * @param ry       椭圆纵半径，正数
     * @param rotation 椭圆旋转角，单位弧度
     * @param angle    圆心参数角，单位弧度
     * @param axis     0 取横坐标，1 取纵坐标
     * @return 指定轴的坐标值
     */
    public static double pointOnEllipse(double cx, double cy, double rx, double ry, double rotation,
                                        double angle, int axis) {
        double cos = Math.cos(rotation);
        double sin = Math.sin(rotation);
        double ex = rx * Math.cos(angle);
        double ey = ry * Math.sin(angle);
        return axis == 0 ? cx + cos * ex - sin * ey : cy + sin * ex + cos * ey;
    }
}
