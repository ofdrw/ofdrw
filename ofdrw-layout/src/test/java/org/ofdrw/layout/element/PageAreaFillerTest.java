package org.ofdrw.layout.element;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.engine.Segment;

public class PageAreaFillerTest {
    @Test
    public void test() {
        Segment sgm = new Segment(138d);
        Assertions.assertTrue(sgm.tryAdd(new PageAreaFiller()));
        // h:138 - 50 = 88
        Assertions.assertFalse(sgm.tryAdd(
                new Div(50d, 10d)
                        .setClear(Clear.none)
        ));
        Assertions.assertTrue(sgm.isRemainAreaFiller());
    }

}