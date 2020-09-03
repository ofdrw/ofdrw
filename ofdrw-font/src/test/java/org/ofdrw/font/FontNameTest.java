package org.ofdrw.font;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-18 20:44:44
 */
class FontNameTest {

    @Test
    void font() {

        Font font = FontName.SimSun.font();

        assertEquals(font.getFamilyName(), "宋体");
        assertEquals(font.getName(), "宋体");
        assertNull(font.getFontFile());
    }
}