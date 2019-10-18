package org.ofdrw.core.text.font;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CharsetTest {

    @Test
    void getInstance() {
        assertTrue("shift-jis".equals(Charset.shift_jis.toString()));
        assertTrue(Charset.getInstance("shift-jis") == Charset.shift_jis);
    }
}