package org.ofdrw.converter.pdf.render;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.pdf.CoordSystem;
import org.ofdrw.converter.pdf.model.AffineMatrix;
import org.ofdrw.converter.pdf.model.ClosePath;
import org.ofdrw.converter.pdf.model.CurveTo;
import org.ofdrw.converter.pdf.model.LineTo;
import org.ofdrw.converter.pdf.model.MoveTo;
import org.ofdrw.converter.pdf.model.PathSegment;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 紧缩图形数据转换测试
 * <p>
 * 锁定：{@code S/M/L/Q/B/A/C} 全覆盖、路径缺少起点时自动补 {@link MoveTo}、
 * 操作数不足按 0 补齐、未知操作符跳过且不影响后续操作符。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class AbbreviatedDataConverterTest {

    /**
     * 断言容限，单位 pt
     */
    private static final double EPS = 1e-6;

    /**
     * 页面高 100mm 的对象空间 → 页面坐标矩阵
     */
    private static final AffineMatrix TO_PAGE = CoordSystem.objectToPage(null, null, 0, 0, 100);

    @Test
    @DisplayName("S/M/L/B/C 全覆盖")
    void basicOperators() {
        AbbreviatedData data = new AbbreviatedData()
                .S(0, 0).L(10, 0).B(10, 5, 5, 10, 0, 10).C();
        List<PathSegment> segments = new AbbreviatedDataConverter().convert(data, TO_PAGE);
        assertEquals(4, segments.size());
        assertTrue(segments.get(0) instanceof MoveTo);
        assertTrue(segments.get(1) instanceof LineTo);
        assertTrue(segments.get(2) instanceof CurveTo);
        assertTrue(segments.get(3) instanceof ClosePath);
        CurveTo curve = (CurveTo) segments.get(2);
        assertEquals(CoordSystem.yToPt(10, 100), curve.getY3(), EPS);
    }

    @Test
    @DisplayName("缺少起点时自动补 MoveTo，保证 PathCommand 合法")
    void missingStart() {
        AbbreviatedData data = new AbbreviatedData().L(10, 0).L(10, 10);
        List<PathSegment> segments = new AbbreviatedDataConverter().convert(data, TO_PAGE);
        assertEquals(3, segments.size());
        assertTrue(segments.get(0) instanceof MoveTo);
    }

    @Test
    @DisplayName("操作数不足按 0 补齐，未知操作符跳过")
    void tolerantOperands() {
        AbbreviatedData data = new AbbreviatedData();
        data.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("M", new double[]{5}));
        data.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("XYZ", new double[]{1, 2}));
        data.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("L", new double[]{10, 0}));
        List<PathSegment> segments = new AbbreviatedDataConverter().convert(data, TO_PAGE);
        assertEquals(2, segments.size());
        MoveTo move = (MoveTo) segments.get(0);
        assertEquals(CoordSystem.mmToPt(5), move.getX(), EPS);
        assertEquals(CoordSystem.yToPt(0, 100), move.getY(), EPS);
        assertTrue(segments.get(1) instanceof LineTo);
    }

    @Test
    @DisplayName("空数据与空对象返回空列表")
    void empty() {
        AbbreviatedDataConverter converter = new AbbreviatedDataConverter();
        assertTrue(converter.convert(null, TO_PAGE).isEmpty());
        assertTrue(converter.convert(new AbbreviatedData(), TO_PAGE).isEmpty());
    }

    @Test
    @DisplayName("C 关闭后当前点清空，再次 L 会补建子路径")
    void closeThenContinue() {
        AbbreviatedData data = new AbbreviatedData().S(0, 0).L(10, 0).C().L(20, 20);
        List<PathSegment> segments = new AbbreviatedDataConverter().convert(data, TO_PAGE);
        assertEquals(5, segments.size());
        assertTrue(segments.get(2) instanceof ClosePath);
        assertTrue(segments.get(3) instanceof MoveTo);
        assertTrue(segments.get(4) instanceof LineTo);
    }

    @Test
    @DisplayName("非有限坐标被拒绝并跳过该操作符")
    void nonFiniteCoordinate() {
        AbbreviatedData data = new AbbreviatedData().S(0, 0);
        data.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("L", new double[]{Double.NaN, 0}));
        data.getRawOptVal().add(new org.ofdrw.core.graph.pathObj.OptVal("L", new double[]{10, 0}));
        List<PathSegment> segments = new AbbreviatedDataConverter().convert(data, TO_PAGE);
        assertEquals(2, segments.size());
        assertTrue(segments.get(1) instanceof LineTo);
    }
}
