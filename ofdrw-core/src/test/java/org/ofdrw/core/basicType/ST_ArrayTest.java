package org.ofdrw.core.basicType;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ST_ArrayTest {

    @Test
    void toInt() {
        ST_Array arr = new ST_Array("7", "7", "7", "7", "7", "7", "7");
        assertArrayEquals(arr.toInt(), new Integer[]{7, 7, 7, 7, 7, 7, 7});
        System.out.println(arr);

        
        arr = ST_Array.getInstance("7 7 7 7 7 7 7");
        assertArrayEquals(arr.toInt(), new Integer[]{7, 7, 7, 7, 7, 7, 7});
    }
}