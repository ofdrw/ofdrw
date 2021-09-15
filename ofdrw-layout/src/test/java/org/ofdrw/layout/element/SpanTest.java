package org.ofdrw.layout.element;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.Rectangle;

import java.util.Arrays;
import java.util.LinkedList;

public class SpanTest {

    @Test
    public void split() {
        Span span = new Span("你好OFD R&W");
        Span[] split = span.split(2);
        System.out.println();
        Assertions.assertEquals(split[0].getText(), "你好");
        Assertions.assertEquals(split[1].getText(), "OFD R&W");
    }

    @Test
    public void blockSize() {
        Span span = new Span("你好OFD R&W")
                .setFontSize(10d);
        Rectangle rectangle = span.blockSize();
        Assertions.assertEquals(rectangle.getHeight(), 10);
        Assertions.assertEquals(rectangle.getWidth(), 20 + 7 * 5);
        Assertions.assertEquals(span.blockSize().getWidth(), 20 + 7 * 5);

        span.setLetterSpacing(3d);
        Assertions.assertEquals(span.blockSize().getWidth(), 20 + 7 * 5 + 9 * 3);

    }

    @Test
    public void getDeltaX() {
        Span span = new Span("你好OFD R&W")
                .setFontSize(10d);
        Double[] deltaX = span.getDeltaX();
        System.out.println(Arrays.toString(deltaX));
        Double[] expect = new Double[]{
                10d, 10d, 5d, 5d, 5d, 5d, 5d, 5d
        };

        Assertions.assertArrayEquals(expect, deltaX);
    }

    @Test
    public void splitLineBreak() {
        Span span = new Span("line 1\n");
        LinkedList<Span> spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 1);

        span = new Span("line 1\nline 2");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 2);

        span = new Span("line 1\n\nline 3");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 3);
        span = new Span("line 1\n\n\nline 4");
        spans = span.splitLineBreak();
        Assertions.assertEquals(spans.size(), 4);

    }
}
