package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Div;

class SegmentTest {

    @Test
    void iterator() {
        Segment sgm = new Segment(900d);
        sgm.tryAdd(Div.placeholder(100, 300, AFloat.left));
        sgm.tryAdd(Div.placeholder(200, 50, AFloat.left));
        sgm.tryAdd(Div.placeholder(500, 150, AFloat.left));
        sgm.tryAdd(Div.placeholder(500, 600, AFloat.left));
        sgm.forEach(kv -> {
            System.out.println(kv.getKey());
        });
        System.out.println();
        sgm.forEach(kv -> {
            System.out.println(kv.getKey());
        });
    }
}