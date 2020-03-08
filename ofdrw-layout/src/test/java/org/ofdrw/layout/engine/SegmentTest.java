package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.Div;

class SegmentTest {

    @Test
    void iterator() {
        Segment sgm = new Segment(900d);
        sgm.tryAdd(Div.placeholder(100, 300));
        sgm.tryAdd(Div.placeholder(200, 50));
        sgm.tryAdd(Div.placeholder(500, 150));
        sgm.tryAdd(Div.placeholder(500, 600));
        sgm.forEach(kv->{
            System.out.println(kv.getKey());
        });
        System.out.println();
        sgm.forEach(kv->{
            System.out.println(kv.getKey());
        });
    }
}