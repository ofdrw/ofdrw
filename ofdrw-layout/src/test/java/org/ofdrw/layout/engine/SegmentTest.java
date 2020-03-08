package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.Div;

import static org.junit.jupiter.api.Assertions.*;

class SegmentTest {

    @Test
    void iterator() {
        Segment sgm = new Segment(900d);
        sgm.tryAdd(Div.empty(100, 300));
        sgm.tryAdd(Div.empty(200, 50));
        sgm.tryAdd(Div.empty(500, 150));
        sgm.tryAdd(Div.empty(500, 600));
        sgm.forEach(kv->{
            System.out.println(kv.getKey());
        });
        System.out.println();
        sgm.forEach(kv->{
            System.out.println(kv.getKey());
        });
    }
}