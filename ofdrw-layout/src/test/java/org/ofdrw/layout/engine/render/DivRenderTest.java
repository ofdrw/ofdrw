package org.ofdrw.layout.engine.render;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DivRenderTest {

    @Test
    void eq() {
        assertTrue(DivRender.eq(0, 0));
        assertTrue(DivRender.eq(1.1, 1.1));
        assertTrue(DivRender.eq(0.0000001, 0.0000001));
        assertTrue(DivRender.eq(0.000000001, 0.0000000012));
    }
}