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
