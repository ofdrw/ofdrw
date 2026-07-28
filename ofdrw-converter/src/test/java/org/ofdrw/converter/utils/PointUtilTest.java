package org.ofdrw.converter.utils;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.point.TextCodePoint;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.text.TextCode;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.ofdrw.converter.utils.CommonUtil.converterDpi;

class PointUtilTest {

    private static final double EPSILON = 0.000001;

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
}
