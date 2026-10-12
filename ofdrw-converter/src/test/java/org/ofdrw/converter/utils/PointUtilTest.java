package org.ofdrw.converter.utils;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.point.PathPoint;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.text.TextCode;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.ofdrw.converter.utils.CommonUtil.converterDpi;

class PointUtilTest {

    private static final double EPSILON = 0.000001;
    private static final double PAGE_WIDTH = 210d;
    private static final double PAGE_HEIGHT = 297d;

    @Test
    void ctmTranslationShouldOnlyApplyToTextOrigin() {
        TextCode code = new TextCode()
                .setContent("AB")
                .setCoordinate(1d, 2d)
                .setDeltaX(4d)
                .setDeltaY(5d);

        ST_Array ctm = new ST_Array(
                2, 0,
                0, 3,
                100, 200
        );
        ST_Box boundary = new ST_Box(10, 20, 200, 300);

        List<TextCodePoint> points = PointUtil.calPdfTextCoordinate(
                1000,
                1000,
                boundary,
                1f,
                Collections.singletonList(code),
                true,
                ctm,
                true,
                1d
        );

        assertEquals(2, points.size());

        // CTM(1, 2) = (102, 206); adding Boundary gives (112, 226).
        assertEquals(converterDpi(112), points.get(0).getX(), EPSILON);
        assertEquals(converterDpi(1000 - 226), points.get(0).getY(), EPSILON);

        // Delta is a vector: only the CTM linear part applies, so (4, 5) -> (8, 15).
        assertEquals(converterDpi(120), points.get(1).getX(), EPSILON);
        assertEquals(converterDpi(1000 - 241), points.get(1).getY(), EPSILON);
    }

    /**
     * PR 新增测试 1：带 CGTransform 参数的重载（ItextMaker 实际调用的路径）
     * 必须与 9 参重载保持相同的语义——平移分量只作用于原点，delta 只经线性部分变换。
     */
    @Test
    void ctmTranslationShouldOnlyApplyToTextOriginInCgTransformOverload() {
        TextCode code = new TextCode()
                .setContent("AB")
                .setCoordinate(1d, 2d)
                .setDeltaX(4d)
                .setDeltaY(5d);

        ST_Array ctm = new ST_Array(
                2, 0,
                0, 3,
                100, 200
        );
        ST_Box boundary = new ST_Box(10, 20, 200, 300);

        List<TextCodePoint> points = PointUtil.calPdfTextCoordinate(
                1000,
                1000,
                boundary,
                1f,
                Collections.singletonList(code),
                Collections.emptyList(),
                null,
                null,
                true,
                ctm,
                true,
                1d
        );

        assertEquals(2, points.size());

        // 与 ctmTranslationShouldOnlyApplyToTextOrigin 的期望完全一致
        assertEquals(converterDpi(112), points.get(0).getX(), EPSILON);
        assertEquals(converterDpi(1000 - 226), points.get(0).getY(), EPSILON);
        assertEquals(converterDpi(120), points.get(1).getX(), EPSILON);
        assertEquals(converterDpi(1000 - 241), points.get(1).getY(), EPSILON);
    }

    /**
     * PR 新增测试 2：竖排文字（真实数电发票复现用例）。
     *
     * CTM="0 1 -1 0 0.50 -2"（90° 旋转 + 非零平移 f=-2），Size=3.175mm，
     * DeltaX=3.18mm。修复前步距 = |3.18×1 + (−2)| = 1.18mm，字符互相重叠
     * 约 63%（"下载次数：1" 渲染成图片后无法辨认）；修复后步距 = 3.18mm。
     *
     * 说明：该重载在变换后坐标系中把步距累积到 x 轴，由 ItextMaker 随后
     * 的画布旋转（rotate(angel)）映射为页面竖直方向，因此这里断言 x 步距。
     */
    @Test
    void verticalTextStepShouldExcludeCtmTranslation() {
        TextCode code = new TextCode()
                .setContent("下载次数：1")
                .setCoordinate(1.5d, 0d)
                .setDeltaX(3.18d, 3.18d, 3.18d, 3.18d, 3.18d);

        ST_Array ctm = new ST_Array(
                0, 1,
                -1, 0,
                0.5, -2
        );
        ST_Box boundary = new ST_Box(207.5, 30, 20, 20);

        List<TextCodePoint> points = PointUtil.calPdfTextCoordinate(
                211.5,
                140,
                boundary,
                3.175f,
                Collections.singletonList(code),
                Collections.emptyList(),
                null,
                null,
                true,
                ctm,
                true,
                1d
        );

        assertEquals(6, points.size());

        // 首字符原点经全量 CTM 变换：(1.5, 0) -> (0.5, -0.5)，
        // 加 Boundary(207.5, 30) 得 (208, 29.5)
        assertEquals(converterDpi(208), points.get(0).getX(), EPSILON);
        assertEquals(converterDpi(140 - 29.5), points.get(0).getY(), EPSILON);

        // 后续字符步距 = |DeltaX × b| = 3.18（不含平移分量 f）
        for (int i = 1; i < points.size(); i++) {
            assertEquals(converterDpi(208 + 3.18 * i), points.get(i).getX(), EPSILON);
            assertEquals(converterDpi(140 - 29.5), points.get(i).getY(), EPSILON);
        }
    }

    @Test
    void legacyFoxitPathUsesAbsolute300DpiCoordinates() {
        ST_Box boundary = new ST_Box(38.438663, 146.473328, 2.963333, 0.423333);
        ST_Array ctm = new ST_Array(0.010948, 0, 0, 0.010925, -6.688669, -5.418684);
        List<PathPoint> source = PointUtil.convertPathAbbreviatedDatatoPoint("M 456 1732 L 486 1732");

        assertTrue(PointUtil.isLegacyAbsolutePath(PAGE_WIDTH, PAGE_HEIGHT, boundary,
                source, true, ctm));

        List<PathPoint> result = PointUtil.calPdfPathPoint(PAGE_WIDTH, PAGE_HEIGHT, boundary,
                source, true, ctm, null, null, true);
        assertEquals(456d * 72d / 300d, result.get(0).x1, 0.001d);
        assertEquals(486d * 72d / 300d, result.get(1).x1, 0.001d);
        assertEquals((PAGE_HEIGHT - 1732d * 25.4d / 300d) * 72d / 25.4d,
                result.get(0).y1, 0.001d);
    }

    @Test
    void standardObjectSpacePathStillUsesBoundaryAndCtm() {
        ST_Box boundary = new ST_Box(38, 146, 3, 1);
        ST_Array ctm = new ST_Array(1, 0, 0, 1, 0, 0);
        List<PathPoint> source = PointUtil.convertPathAbbreviatedDatatoPoint("M 0 0.5 L 3 0.5");

        assertFalse(PointUtil.isLegacyAbsolutePath(PAGE_WIDTH, PAGE_HEIGHT, boundary,
                source, true, ctm));

        List<PathPoint> result = PointUtil.calPdfPathPoint(PAGE_WIDTH, PAGE_HEIGHT, boundary,
                source, true, ctm, null, null, true);
        assertEquals(38d * 72d / 25.4d, result.get(0).x1, 0.001d);
        assertEquals((PAGE_HEIGHT - 146.5d) * 72d / 25.4d, result.get(0).y1, 0.001d);
    }

    @Test
    void legacyFoxitLineWidthIsAlreadyInPdfPoints() {
        ST_Array ctm = new ST_Array(0.010948, 0, 0, 0.010925, -6.688669, -5.418684);

        assertEquals(1.35467d,
                PointUtil.calPdfPathLineWidth(1.35467d, 1d, true, ctm), 0.00001d);
        assertEquals(1.35467d * 72d / 25.4d * 0.010948d,
                PointUtil.calPdfPathLineWidth(1.35467d, 1d, false, ctm), 0.00001d);
    }
}
