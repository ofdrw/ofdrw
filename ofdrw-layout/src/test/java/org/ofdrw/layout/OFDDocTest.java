package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.*;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-22 11:38:48
 */
class OFDDocTest {


    @Test
    void divBoxTest() throws IOException {
        Path path = Paths.get("target/VPage1.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(70d).setY(113.5)
                    .setBackgroundColor(30, 144, 255)
                    .setMargin(10d)
                    .setBorder(10d)
                    .setPadding(10d);
            vPage.add(e);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void divBoxDiffBorderTest() throws IOException {
        Path path = Paths.get("target/VPage2.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(70d).setY(113.5)
                    .setBackgroundColor(30, 144, 255)
                    .setBorderColor(255, 0, 0)
                    .setMargin(10d)
                    .setBorderTop(10d).setBorderRight(7d).setBorderBottom(3d).setBorderLeft(0.5d)
                    .setPadding(10d);
            vPage.add(e);
            ofdDoc.addVPage(vPage);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void imgTest() throws IOException {
        Path path = Paths.get("target/VPage3.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Path imgPath = Paths.get("src/test/resources", "testimg.png");
            Img img = new Img(imgPath);

            double x = (pageLayout.getWidth() - img.getWidth()) / 2;
            double y = (pageLayout.getHeight() - img.getHeight()) / 2;
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            img.setBorder(3d);
            img.setPadding(3d);
            vPage.add(img);
            ofdDoc.addVPage(vPage);
        }
    }

    @Test
    void paragraphTest() throws IOException {
        Path path = Paths.get("target/VPage4.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);

            Paragraph p = new Paragraph(120d, 10d);
            p.add("我们无论遇到什么困难也不要怕，微笑着面对它，消除恐惧的最好办法就是直面恐惧，坚持就是胜利，加油！奥力给！ （OFD Reader & Write）");
            p.setPosition(Position.Absolute)
                    .setPadding(3d)
                    .setBorder(0.353);
            double x = pageLayout.getWidth() / 2 - p.getMarginLeft() - p.getWidth() / 2;
            p.setX(x).setY(pageLayout.getMarginTop());

            vPage.add(p);
            ofdDoc.addVPage(vPage);
        }
    }

    @Test
    void paragraphTest2() throws IOException {
        Path path = Paths.get("target/VPage5.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);

            Paragraph p = new Paragraph(120d, 10d);
            StringBuilder txt = new StringBuilder();
            for (char ch = 32; ch <= 126; ch++) {
                txt.append(ch);
            }
            p.add(txt.toString());
            p.setPosition(Position.Absolute)
                    .setPadding(3d)
                    .setBorder(0.353);
            double x = pageLayout.getWidth() / 2 - p.getMarginLeft() - p.getWidth() / 2;
            p.setX(x).setY(pageLayout.getMarginTop());
            vPage.add(p);

            ofdDoc.addVPage(vPage);
        }
    }


    @Test
    void streamTestFloat() throws IOException {
        Path path = Paths.get("target/VPage6.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            Div eLeft = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.left)
                    .setBorder(10d)
                    .setPadding(10d);
            Div eCenter = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.center)
                    .setBorder(10d)
                    .setPadding(10d);
            Div eRight = new Div(10d, 10d)
                    .setBackgroundColor(30, 144, 255)
                    .setFloat(AFloat.right)
                    .setBorder(10d)
                    .setPadding(10d);

            ofdDoc.add(eLeft);
            ofdDoc.add(eCenter);
            ofdDoc.add(eRight);
        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

    @Test
    void streamTestPageSplit() throws IOException {
        Path path = Paths.get("target/VPage7.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            ofdDoc.setDefaultPageLayout(PageLayout.A4.setMargin(0d));
            for (int i = 0; i < 500; i++) {
                Div e = new Div(70d, 30d)
                        .setBackgroundColor(	255,0,0)
                        .setFloat(AFloat.center)
                        .setMargin(5d)
                        .setBorder(0.353d);
                ofdDoc.add(e);
            }

        }
        System.out.println("生成文档位置: " + path.toAbsolutePath());
    }

}