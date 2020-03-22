package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.Div;
import org.ofdrw.layout.element.Img;
import org.ofdrw.layout.element.Position;

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
                    .setMargin(10)
                    .setBorder(10)
                    .setPadding(10);
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
                    .setMargin(10)
                    .setBorderTop(10d).setBorderRight(7d).setBorderBottom(3d).setBorderLeft(0.5d)
                    .setPadding(10);
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
            Img img = new Img(35, 13, imgPath);

            double x = pageLayout.getHeight() / 2 - (35.0 / 2);
            double y = pageLayout.getWidth() / 2 - (13.0 / 2);
            img.setPosition(Position.Absolute)
                    .setX(x).setY(y);
            vPage.add(img);
            ofdDoc.addVPage(vPage);
        }

    }

}