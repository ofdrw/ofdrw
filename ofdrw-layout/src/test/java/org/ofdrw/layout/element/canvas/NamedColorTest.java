package org.ofdrw.layout.element.canvas;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class NamedColorTest {

    @Test
    void rgb() {
        int[] rgb = NamedColor.rgb("blue");
        assertArrayEquals(new int[]{0, 0, 255}, rgb);
        rgb = NamedColor.rgb("#FF00FF");
        assertArrayEquals(new int[]{255, 0, 255}, rgb);
        rgb = NamedColor.rgb("#FFF");
        assertArrayEquals(new int[]{255, 255, 255}, rgb);
    }
}