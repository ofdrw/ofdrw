package org.ofdrw.layout.engine;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;

import java.util.LinkedList;
import java.util.List;


class StreamingLayoutAnalyzerTest {

    @Test
    public void analyze(){
        // TODO 分析
        PageLayout layout = new PageLayout(210d, 297d);
        StreamingLayoutAnalyzer analyzer = new StreamingLayoutAnalyzer(layout);
        double width = layout.getWorkerArea().getWidth();
        System.out.println(width);
        List<Segment> seq = new LinkedList<>();

    }
}