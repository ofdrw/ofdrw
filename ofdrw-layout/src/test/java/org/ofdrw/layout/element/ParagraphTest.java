package org.ofdrw.layout.element;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.font.Font;
import org.ofdrw.layout.Rectangle;

import java.nio.file.Paths;

public class ParagraphTest {

    /**
     * 测试字体，显式绑定仓库内的字体文件
     * <p>
     * 不绑定字体文件时按字体名在运行环境中查找系统字体，缺失字形时还会回退到系统的默认字体，
     * 测量结果随是否安装中文字体而变，因此测试中的文字都绑定该字体文件。
     */
    private static final Font DEFAULT_FONT = new Font("宋体", "宋体",
            Paths.get("src/test/resources/simhei-cut1.ttf").toAbsolutePath());

    @Test
    public void doPrepare() {
        Paragraph p1 = new Paragraph();
        p1.setDefaultFont(DEFAULT_FONT);
        p1.setFontSize(10.0);
        Span head = new Span("我说你好").setFont(DEFAULT_FONT).setFontSize(10.0);
        p1.add(head);
        Span span = new Span("你说Hi")
                .setFont(DEFAULT_FONT)
                .setFontSize(15d);
        p1.add(span);

        Rectangle rectangle = p1.doPrepare(400d);
        System.out.println(rectangle);
        Assertions.assertEquals(19.109475, rectangle.getHeight(), 0.01);
        // 宽度为两段文字各自按字号测得宽度之和，期望值取自同一字体文件的测量结果，不随运行环境字体而变
        Assertions.assertEquals(head.blockSize().getWidth() + span.blockSize().getWidth(),
                rectangle.getWidth(), 0.01);

        rectangle = p1.doPrepare(55d);
        System.out.println(rectangle);
        Assertions.assertEquals(19.109475, rectangle.getHeight(), 0.01);

        span.setLetterSpacing(3d);
        rectangle = p1.doPrepare(56d);
        System.out.println(rectangle);
        Assertions.assertEquals(19.109475, rectangle.getHeight(), 0.01);
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
        Paragraph p = new Paragraph(10d, 20d).setDefaultFont(DEFAULT_FONT).setFontSize(15d);
        p.add("1说");
        p.doPrepare(10d);

        Paragraph p2 = new Paragraph(10d, 20d).setDefaultFont(DEFAULT_FONT).setFontSize(15d);
        p2.add("好的");
        p2.doPrepare(10d);
    }

    @Test
    public void split() {
        Paragraph p = new Paragraph().setDefaultFont(DEFAULT_FONT).setFontSize(10d).add("岂不美哉");
        Span span = new Span("妙妙oh").setFont(DEFAULT_FONT).setFontSize(15d).setLetterSpacing(3d);
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
        Assertions.assertEquals(3, ((Paragraph)sp2[1]).getLines().size());

        p.setMarginTop(12d);
        Div[] sp3 = p.split(11d);
        Assertions.assertEquals(sp3[0].getMarginTop(), 11);
        Assertions.assertEquals(sp3[0].box().getHeight(), 11);
        Assertions.assertEquals(sp3[1].getMarginTop(), 1);

    }

}
