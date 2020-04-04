package org.ofdrw.layout;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-21 10:36:22
 */
class PageLayoutTest {

    @Test
    void testEquals() {
        final PageLayout a4 = PageLayout.A4();
        PageLayout a4Copy = a4;
        assertTrue(a4.equals(a4Copy));
        PageLayout customA4 = new PageLayout(210d, 297d);
        assertTrue(a4.equals(customA4));
    }
}