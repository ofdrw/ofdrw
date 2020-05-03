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

    @Test
    void mtxMul() {
        ST_Array a = new ST_Array(0, 1, -1, 0, 0, 0);
        ST_Array b = new ST_Array(2, 0, 0, 1, 0, 0);
        ST_Array c = new ST_Array(1, 0, 0, 1, 0, 3);

        ST_Array d = a.mtxMul(b).mtxMul(c);
        d.printMtx();
    }
}