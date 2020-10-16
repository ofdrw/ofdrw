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
        int cnt = 0;
        for (TxtLineBlock line : p1.getLines()) {
            cnt++;
            System.out.print(cnt + ": ");
            line.getInlineSpans().forEach(s-> System.out.print(s.getText()));
            System.out.println();
        }
    }

    /**
     * 字体大小溢出测试
     */
    @Test
    public void doPrepare2() {
        Paragraph p = new Paragraph(10d, 20d).setFontSize(15d);
        p.add("1说");
        p.doPrepare(10d);

        Paragraph p2 = new Paragraph(10d, 20d).setFontSize(15d);
        p2.add("好的");
        p2.doPrepare(10d);
    }

    @Test
    public void split() {
        Paragraph p = new Paragraph().setFontSize(10d).add("岂不美哉");
        Span span = new Span("妙妙oh").setFontSize(15d).setLetterSpacing(3d);
        p.add(span);
        p.doPrepare(55d);
        /*
        line1: 岂不美哉 40 = 10 * 4 | H 10
        lineSpace: 2
        line2: 妙妙o    46 = (15 + 3) * 2 + (7 + 1 + 3) | H 15
        lineSpace: 2
        line3: h        10 = 7 + 1 + 3 | H 15
        lineSpace: 2

        height = 10 + 15 * 2 + 2 * 3 = 46
        */
        Div[] sp1 =p.split(11);
        Assertions.assertEquals(sp1.length, 2);
        Assertions.assertTrue(sp1[0].isPlaceholder());
        Assertions.assertEquals(sp1[0].doPrepare(55d).getHeight(), 11);

        Div[] sp2 = p.split(12);
        Assertions.assertEquals(((Paragraph)sp2[1]).getLines().size(), 2);

        p.setMarginTop(12d);
        Div[] sp3 = p.split(11d);
        Assertions.assertEquals(sp3[0].getMarginTop(), 11);
        Assertions.assertEquals(sp3[0].box().getHeight(), 11);
        Assertions.assertEquals(sp3[1].getMarginTop(), 1);

    }

}
