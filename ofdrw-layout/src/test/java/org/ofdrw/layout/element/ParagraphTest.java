package org.ofdrw.layout.element;


import org.junit.jupiter.api.Test;
import org.ofdrw.layout.Rectangle;

public class ParagraphTest {


    @Test
    public void reSize() {
        Paragraph p1 = new Paragraph();
        p1.setFontSize(10.0);
        // w += 40; h += 10
        p1.add("我说你好");
        Span span = new Span("你说Hi")
                .setFontSize(15d);
        p1.add(span);

        Rectangle rectangle = p1.reSize(400d);
        System.out.println(rectangle);

        rectangle = p1.reSize(55d);
        System.out.println(rectangle);
    }
}
