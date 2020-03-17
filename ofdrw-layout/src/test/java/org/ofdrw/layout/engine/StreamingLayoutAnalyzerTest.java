package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;
import org.ofdrw.layout.VirtualPage;
import org.ofdrw.layout.element.Clear;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.PageAreaFiller;

import java.util.LinkedList;
import java.util.List;


class StreamingLayoutAnalyzerTest {

    @Test
    public void analyze() {
        // h: 225 w: 138
        PageLayout layout = new PageLayout(210d, 297d)
                .setMargin(36d);
        StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(layout);
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
        // 段内包含和分段和不可分段元素
        sgm.tryAdd(new Div(50d, 50d).setClear(Clear.none));
        sgm.tryAdd(new Div(50d, 225d).setClear(Clear.none).setIntegrity(true));
        seq.add(sgm);
        virtualPages = analyzer.analyze(seq);
        Assertions.assertEquals(virtualPages.size(), 3);
        Assertions.assertEquals(virtualPages.get(2).getContent().size(), 1);
        div = virtualPages.get(2).getContent().get(0);
        Assertions.assertEquals(div.getX(), 36 + 50);
    }
}