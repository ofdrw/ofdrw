package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Div;

class SegmentTest {

    @Test
    public void tryAdd() {
        Segment sgm = new Segment(138d);

        Assertions.assertTrue(sgm.tryAdd(new Div(70d, 70d)));
        Assertions.assertFalse(sgm.tryAdd(new Div(70d, 70d)));

        sgm = new Segment(138d);
        // h:138 - 50 = 88
        Assertions.assertTrue(sgm.tryAdd(
                new Div(50d, 10d)
                        .setClear(Clear.none)
        ));
        // h: 88 - 38 = 50
        Assertions.assertTrue(sgm.tryAdd(
                new Div(38d, 70d)
                        .setClear(Clear.none)
        ));
        Assertions.assertEquals(sgm.getHeight(), 70);

        Assertions.assertFalse(sgm.tryAdd(
                new Div(55d, 70d)
                        .setClear(Clear.none)
        ));
        Assertions.assertFalse(sgm.tryAdd(
                new Div(1d, 70d)
                        .setClear(Clear.none)
        ));

    }


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