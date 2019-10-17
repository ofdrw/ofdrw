package org.ofdrw.core.basicType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ST_BoxTest {


    @Test
    public void con() throws Exception {
        ST_Box box = new ST_Box(0, 0, 255, 255);
        assertTrue("0 0 255 255".equals(box.toString()));
    }
}