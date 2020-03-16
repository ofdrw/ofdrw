package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Div;

import java.util.LinkedList;
import java.util.List;


class StreamingLayoutAnalyzerTest {

    @Test
    public void analyze() {
        // h: 225 w: 138
        PageLayout layout = new PageLayout(210d, 297d);
        StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(layout);
        double width = layout.getWorkerArea().getWidth();
        System.out.println(width);
        List<Segment> seq = new LinkedList<>();

        Segment sgm = new Segment(layout.contentWidth());
        sgm.tryAdd(new Div(100d, 25d));
        seq.add(sgm);
        List<VirtualPage> virtualPages = analyzer.analyze(seq);
        Assertions.assertEquals(virtualPages.size(), 1);
        Div div = virtualPages.get(0).getContent().get(0);
        Assertions.assertEquals(div.getX(), 36d);
        Assertions.assertEquals(div.getY(), 36d);

        seq.clear();
        sgm = new Segment(layout.contentWidth());
        sgm.tryAdd(new Div(18d, 225d));
        seq.add(sgm);
        virtualPages = analyzer.analyze(seq);
        Assertions.assertEquals(virtualPages.size(), 2);

        div = virtualPages.get(0).getContent().get(1);
        Assertions.assertEquals(div.getX(), 36d);
        Assertions.assertEquals(div.getY(), 36 + 25);
        System.out.println();

        // h: 225 - 25 = 200
        seq.clear();
        sgm = new Segment(layout.contentWidth());
        sgm.tryAdd(new Div(18d, 225d));
        seq.add(sgm);
        virtualPages = analyzer.analyze(seq);
    }
}