package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 权观宇
 * @since 2020-03-29 12:24:42
 */
public class LayoutTest {

    @Test
    void test1() throws IOException {
        Path path = Paths.get("target/Layout1.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph p = new Paragraph().setFontSize(8d).add("Float: center");
            p.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(p);

            Div div1 = new Div(50d, 65d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setClear(Clear.none)
                    .setFloat(AFloat.center);
            Div div2 = new Div(20d, 30d)
                    .setBackgroundColor(255, 0, 0)
                    .setBorder(0.353)
                    .setBorderLeft(0d)
                    .setClear(Clear.none)
                    .setFloat(AFloat.center);
            ofdDoc.add(div1)
                    .add(div2);

            p = new Paragraph().setFontSize(8d).add("Float: left");
            p.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(p);
            div1 = new Div(50d, 65d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setClear(Clear.none)
                    .setFloat(AFloat.left);
            div2 = new Div(20d, 30d)
                    .setBackgroundColor(255, 0, 0)
                    .setBorder(0.353)
                    .setBorderLeft(0d)
                    .setClear(Clear.none)
                    .setFloat(AFloat.left);
            ofdDoc.add(div1)
                    .add(div2);

            ofdDoc.add(new PageAreaFiller());

            p = new Paragraph().setFontSize(8d).add("Float: right");
            p.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(p);
            div1 = new Div(50d, 65d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setClear(Clear.none)
                    .setFloat(AFloat.right);
            div2 = new Div(20d, 30d)
                    .setBackgroundColor(255, 0, 0)
                    .setBorder(0.353)
                    .setBorderRight(0d)
                    .setClear(Clear.none)
                    .setFloat(AFloat.right);
            ofdDoc.add(div1)
                    .add(div2);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void test2() throws IOException {
        Path path = Paths.get("target/Layout2.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Div div1 = new Div(35d, 45d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setFloat(AFloat.left);
            Paragraph p1 = new Paragraph().setFontSize(8d).add("Float: Left");
            p1.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(div1)
                    .add(p1);

            Div div2 = new Div(35d, 45d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setFloat(AFloat.center);
            Paragraph p2 = new Paragraph().setFontSize(8d).add("Float: center");
            p2.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(div2)
                    .add(p2);

            Div div3 = new Div(35d, 45d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setFloat(AFloat.right);
            Paragraph p3 = new Paragraph().setFontSize(8d).add("Float: right");
            p3.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(div3)
                    .add(p3);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void test3() throws IOException {
        Path path = Paths.get("target/Layout3.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Paragraph normal = new Paragraph().setFontSize(8d).add("Normal");
            normal.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(normal);
            Div div1 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setMargin(3d)
                    .setBackgroundColor(0, 0, 255)
                    .setBorder(1d);
            Div div2 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setMargin(3d)
                    .setBackgroundColor(255, 255, 0)
                    .setBorder(1d);
            Div div3 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setMargin(3d)
                    .setBackgroundColor(0, 0, 255)
                    .setBorder(1d);
            ofdDoc.add(div1).add(div2).add(div3);


            Paragraph relative = new Paragraph().setFontSize(8d).add("Relative");
            relative.setFloat(AFloat.center).setMargin(7d);
            ofdDoc.add(relative);
            div1 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setMargin(3d)
                    .setBackgroundColor(0, 0, 255)
                    .setBorder(1d);
            div2 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setPosition(Position.Relative)
                    .setLeft(17d)
                    .setTop(17d)
                    .setMargin(3d)
                    .setBackgroundColor(255, 255, 0)
                    .setBorder(1d);

            div3 = new Div(35d, 35d)
                    .setClear(Clear.none)
                    .setMargin(3d)
                    .setBackgroundColor(0, 0, 255)
                    .setBorder(1d);
            ofdDoc.add(div1).add(div2).add(div3);
        }
    }

    @Test
    void test4() throws IOException {
        Path path = Paths.get("target/Layout4.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Div div1 = new Div(30d, 30d)
                    .setBackgroundColor(105, 105, 105)
                    .setBorder(0.353)
                    .setClear(Clear.none);
            Div div2 = new Div(60d, 60d)
                    .setClear(Clear.none)
                    .setBackgroundColor(0, 0, 255)
                    .setBorder(0.353);
            Div div3 = new Div(120d, 30d)
                    .setClear(Clear.none)
                    .setBackgroundColor(255, 255, 0)
                    .setBorder(0.353);
            ofdDoc.add(div1).add(div2).add(div3);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void test5() throws IOException {
        Path path = Paths.get("target/Layout5.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            final PageLayout pageLayout = ofdDoc.getPageLayout();
            double remain = 50d;
            double h = pageLayout.contentHeight() - remain;
            Div div1 = new Div(pageLayout.contentWidth(), h)
                    .setBackgroundColor(255, 255, 0)
                    .setClear(Clear.none);

            Div div2 = new Div(40d, 40d)
                    .setClear(Clear.none)
                    .setIntegrity(true)
                    .setBackgroundColor(255, 0, 0);
            Div div3 = new Div(40d, 80d)
                    .setClear(Clear.none)
                    .setIntegrity(true)
                    .setBackgroundColor(105, 105, 105);
            Div div4 = new Div(40d, 60d)
                    .setClear(Clear.none)
                    .setIntegrity(true)
                    .setBackgroundColor(0, 0, 0);
            ofdDoc.add(div1).add(div2).add(div3).add(div4);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void test6() throws IOException {
        Path path = Paths.get("target/Layout6.ofd");
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            final PageLayout pageLayout = ofdDoc.getPageLayout();
            double remain = 50d;
            double h = pageLayout.contentHeight() - remain;
            Div div1 = new Div(pageLayout.contentWidth(), h)
                    .setBackgroundColor(255, 255, 0)
                    .setMarginBottom(10d)
                    .setClear(Clear.none);

            Div div2 = new Div(40d, 40d)
                    .setClear(Clear.none)

                    .setBackgroundColor(255, 0, 0);
            Div div3 = new Div(40d, 400d)
                    .setClear(Clear.none)

                    .setBackgroundColor(105, 105, 105);
            Div div4 = new Div(40d, 800d)
                    .setClear(Clear.none)
                    .setBackgroundColor(0, 0, 0);
            ofdDoc.add(div1).add(div2).add(div3).add(div4);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }
}
