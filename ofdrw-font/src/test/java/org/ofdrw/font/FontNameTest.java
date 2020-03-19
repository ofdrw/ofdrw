package org.ofdrw.font;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-18 20:44:44
 */
class FontNameTest {
    @Test
    public void getFilename(){
        assertEquals(FontName.NotoSerif.getFilename(), "NotoSerifCJKsc-Medium.otf");
    }

}