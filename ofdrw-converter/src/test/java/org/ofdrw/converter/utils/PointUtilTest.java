package org.ofdrw.converter.utils;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicType.ST_Box;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class PointUtilTest {

    @Test
    void combineBoundaryShouldIncludeNestedClipPathOffset() {
        ST_Box objectBoundary = new ST_Box(89.5, 3.5, 31, 21);
        ST_Box clipPathBoundary = new ST_Box(0.5, 0.5, 30, 20);

        ST_Box actual = PointUtil.combineBoundary(objectBoundary, clipPathBoundary);

        assertEquals(90, actual.getTopLeftX());
        assertEquals(4, actual.getTopLeftY());
        assertEquals(30, actual.getWidth());
        assertEquals(20, actual.getHeight());
        assertEquals(89.5, objectBoundary.getTopLeftX());
        assertEquals(3.5, objectBoundary.getTopLeftY());
    }

    @Test
    void combineBoundaryShouldKeepParentWhenNestedBoundaryIsMissing() {
        ST_Box objectBoundary = new ST_Box(10, 20, 30, 40);

        assertSame(objectBoundary, PointUtil.combineBoundary(objectBoundary, null));
    }
}
