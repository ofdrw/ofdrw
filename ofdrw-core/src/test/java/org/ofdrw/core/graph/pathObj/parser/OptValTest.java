package org.ofdrw.core.graph.pathObj.parser;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.graph.pathObj.OptVal;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-04-24 18:29:09
 */
class OptValTest {

    @Test
    void arrFilling() {

        double[] arr = {1, 2, 3};
        double[] newArr = OptVal.filling(arr, 2);
        assertEquals("[1.0, 2.0, 3.0]",Arrays.toString(newArr) );
       newArr = OptVal.filling(arr, 4);

        assertEquals("[1.0, 2.0, 3.0, 0.0]",Arrays.toString(newArr) );
    }
}