package org.ofdrw.layout.engine;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.element.AFloat;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Div;

import java.util.LinkedList;
import java.util.List;

public class SegmentationEngineTest {

    @Test
    public void process() {
        // w: 138  h: 225
        SegmentationEngine sgnEngine = new SegmentationEngine(PageLayout.A4);
        List<Div> list = new LinkedList<>();

        list.add(new Div(50d, 25d).setFloat(AFloat.center));

        // w: 138 - 38 = 100
        Div div = new Div()
                .setClear(Clear.none)
                .setFloat(AFloat.left)
                .setWidth(38d)
                .setHeight(10d);
        list.add(div);
        List<Segment> segments = sgnEngine.process(list);
        Assertions.assertEquals(segments.size(), 2);

        // w: 100 - 90 = 10
        list.add(new Div()
                .setClear(Clear.none)
                .setFloat(AFloat.left)
                .setWidth(90d)
                .setHeight(70d));
        segments = sgnEngine.process(list);
        Assertions.assertEquals(segments.size(), 2);

        // h: 120 w: 138 - 70
        list.add(new Div()
                .setClear(Clear.none)
                .setFloat(AFloat.left)
                .setWidth(70d)
                .setHeight(70d));
        segments = sgnEngine.process(list);
        Assertions.assertEquals(segments.size(), 3);

    }
}
