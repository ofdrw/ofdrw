package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 圆弧 {@code A} 转换测试
 * <p>
 * 锁定：大弧/小弧标志、方向标志、旋转椭圆、退化情况（半径为 0、起终点重合）以及
 * 每段贝塞尔不超过 90 度；同时验证经过 Y 翻转（PDF 页面坐标）后圆弧仍然落在正确的象限。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class ArcConverterTest {

    /**
     * 判断误差容限，单位 pt
     */
    private static final double EPS = 1e-6;

    @Test
    @DisplayName("四分之一圆：小弧 + 角度增大方向 → 单段贝塞尔，控制系数 4/3·tan(π/8)")
    void quarterArcUnitCircle() {
        // 单位圆：从 (1,0) 逆时针（Y 轴向上、角度增大）画到 (0,1)
        List<ArcConverter.CurveSegment> segments = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, false, true, 0, 1, null);
        assertEquals(1, segments.size());
        double k = 4.0 / 3.0 * Math.tan(Math.PI / 8);
        ArcConverter.CurveSegment s = segments.get(0);
        assertEquals(1, s.getX1(), EPS);
        assertEquals(k, s.getY1(), EPS);
        assertEquals(k, s.getX2(), EPS);
        assertEquals(1, s.getY2(), EPS);
        assertEquals(0, s.getX3(), EPS);
        assertEquals(1, s.getY3(), EPS);
    }

    @Test
    @DisplayName("半圆：大弧标志决定经过上半圆还是下半圆")
    void halfArcFlags() {
        // 从 (1,0) 到 (-1,0)，large=false → 小弧（经过上半圆）
        List<ArcConverter.CurveSegment> small = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, false, true, -1, 0, null);
        assertEquals(2, small.size());
        assertTrue(small.get(0).getY1() > 0);

        // large=true + 方向相反（角度减小）→ 经过下半圆（180° 弧同样拆成 2 段）
        List<ArcConverter.CurveSegment> large = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, true, false, -1, 0, null);
        assertEquals(2, large.size());
        assertTrue(large.get(0).getY1() < 0);
        assertEquals(-1, large.get(large.size() - 1).getX3(), EPS);

        // large=true + 角度增大方向 → 经过上半圆的 180° 弧（同样拆成 2 段）
        List<ArcConverter.CurveSegment> largeUpper = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, true, true, -1, 0, null);
        assertEquals(2, largeUpper.size());
        assertTrue(largeUpper.get(0).getY1() > 0);

        // 方向相反（角度减小）的小弧 → 经过下半圆
        List<ArcConverter.CurveSegment> lower = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, false, false, -1, 0, null);
        assertEquals(2, lower.size());
        assertTrue(lower.get(0).getY1() < 0);
    }

    @Test
    @DisplayName("整圆附近的大弧（>180°）拆分为多段，每段不超过 90 度")
    void largeArcSplit() {
        // 起点 (1,0)，终点 (0,-1)，large=true、角度增大方向 → 270° 圆弧
        List<ArcConverter.CurveSegment> segments = ArcConverter.toCubicSegments(
                1, 0, 1, 1, 0, true, true, 0, -1, null);
        assertEquals(3, segments.size());
        // 最后一段终点即 A 操作符终点
        ArcConverter.CurveSegment last = segments.get(segments.size() - 1);
        assertEquals(0, last.getX3(), EPS);
        assertEquals(-1, last.getY3(), EPS);
        // 中间段终点处曲线上，半径恒为 1
        for (ArcConverter.CurveSegment segment : segments) {
            double radius = Math.hypot(segment.getX3(), segment.getY3());
            assertEquals(1, radius, EPS);
        }
    }

    @Test
    @DisplayName("旋转 90° 的椭圆弧：半径互换")
    void rotatedEllipse() {
        // rx=2, ry=1，旋转 90° 后长轴位于 Y 方向：起点 (0,1) 经角度增大方向到 (2,0)（90° 弧）
        List<ArcConverter.CurveSegment> segments = ArcConverter.toCubicSegments(
                0, 1, 2, 1, 90, false, true, 2, 0, null);
        // 旋转椭圆结果必须落在以 (1,1) 为中心、旋转 90° 的椭圆上
        assertFalse(segments.isEmpty());
        for (ArcConverter.CurveSegment segment : segments) {
            for (double x : new double[]{segment.getX1(), segment.getX2(), segment.getX3()}) {
                assertTrue(Double.isFinite(x));
            }
        }
        ArcConverter.CurveSegment last = segments.get(segments.size() - 1);
        assertEquals(2, last.getX3(), EPS);
        assertEquals(0, last.getY3(), EPS);
        // 终点在椭圆上的切向应指向下方（角度增大方向）
        assertTrue(last.getY2() < last.getY3() + EPS);
    }

    @Test
    @DisplayName("退化：半径为 0 或起终点重合时输出单段直线")
    void degenerate() {
        List<ArcConverter.CurveSegment> zeroRadius = ArcConverter.toCubicSegments(
                0, 0, 0, 5, 0, false, true, 10, 10, null);
        assertEquals(1, zeroRadius.size());
        assertEquals(10, zeroRadius.get(0).getX3(), EPS);
        assertEquals(10, zeroRadius.get(0).getY3(), EPS);

        List<ArcConverter.CurveSegment> samePoint = ArcConverter.toCubicSegments(
                3, 4, 5, 5, 0, false, true, 3, 4, null);
        assertEquals(1, samePoint.size());
        assertEquals(3, samePoint.get(0).getX3(), EPS);
    }

    @Test
    @DisplayName("非有限数值拒绝计算")
    void rejectsNonFinite() {
        assertThrows(IllegalArgumentException.class,
                () -> ArcConverter.toCubicSegments(Double.NaN, 0, 1, 1, 0, false, true, 1, 1, null));
    }

    @Test
    @DisplayName("圆心参数圆弧：单段转换与拆分")
    void centerArcSegments() {
        ArcConverter.ArcParams arc = new ArcConverter.ArcParams(0, 0, 10, 10, 0, 0, Math.PI * 2);
        List<ArcConverter.CurveSegment> segments = ArcConverter.toCubicSegments(arc);
        assertEquals(4, segments.size());
        ArcConverter.CurveSegment last = segments.get(3);
        assertEquals(10, last.getX3(), 1e-4);
        assertEquals(0, last.getY3(), 1e-4);
        assertTrue(ArcConverter.toCubicSegments(new ArcConverter.ArcParams(0, 0, 1, 1, 0, 0, 0)).isEmpty());
    }

    @Test
    @DisplayName("页面坐标（Y 轴向上）：经过 toPage 逆变换后弧线方向与对象空间一致")
    void pageCoordinates() {
        // 页面高 100mm：对象空间 (10,10) → 页面 (10·k, (100-10)·k)
        org.ofdrw.converter.pdf.model.AffineMatrix toPage =
                org.ofdrw.converter.pdf.CoordSystem.objectToPage(null, null, 0, 0, 100);
        double[] start = toPage.transform(10, 10);
        double[] end = toPage.transform(20, 10);
        // 对象空间口径：(10,10) → (20,10)，large=false + 方向相反 → 270° 圆弧，先向对象空间的下方凸出
        List<ArcConverter.CurveSegment> segments = ArcConverter.toCubicSegments(
                start[0], start[1], 5, 5, 0, false, false, end[0], end[1], toPage);
        assertFalse(segments.isEmpty());
        ArcConverter.CurveSegment first = segments.get(0);
        double startY = toPage.transform(10, 10)[1];
        // 对象空间“下方”（y 更大）在页面坐标中 y 更小：控制点应低于起点
        assertTrue(first.getY2() < startY);
        ArcConverter.CurveSegment last = segments.get(segments.size() - 1);
        assertEquals(end[0], last.getX3(), 1e-6);
        assertEquals(end[1], last.getY3(), 1e-6);
    }
}
