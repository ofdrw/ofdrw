package org.ofdrw.layout.element;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.Rectangle;

public class ParagraphTest {


    @Test
    public void doPrepare() {
        Paragraph p1 = new Paragraph();
        p1.setFontSize(10.0);
        // w = 40; h = 10
        p1.add("我说你好");
        // w = 30 + 15 + 2; h = 15
        Span span = new Span("你说Hi")
                .setFontSize(15d);
        p1.add(span);

        Rectangle rectangle = p1.doPrepare(400d);
        System.out.println(rectangle);
        Assertions.assertEquals(rectangle.getHeight(), 17);
        Assertions.assertEquals(rectangle.getWidth(), 400);
        // Line1: 40 + 15 = 55 | Line2: 15 + 2 * (7 + 1) = 41
        rectangle = p1.doPrepare(55d);
        System.out.println(rectangle);
        // lineSpace: 2
        Assertions.assertEquals(rectangle.getHeight(), (2 + 15) * 2);

        span.setLetterSpacing(3d);
        rectangle = p1.doPrepare(56d);
        System.out.println(rectangle);
        Assertions.assertEquals(rectangle.getHeight(), 12 + (15 + 2) * 2);

    }
}
