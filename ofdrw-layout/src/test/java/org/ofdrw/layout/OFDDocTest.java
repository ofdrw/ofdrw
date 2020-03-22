package org.ofdrw.layout;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.element.Div;
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
    void addVPage() throws IOException {
        System.out.println(Paths.get("target/VPage.ofd").toAbsolutePath());
        Path path = Paths.get("target/VPage.ofd").toAbsolutePath();
        try (OFDDoc ofdDoc = new OFDDoc(path)) {
            PageLayout pageLayout = ofdDoc.getPageLayout();
            VirtualPage vPage = new VirtualPage(pageLayout);
            Div e = new Div(10d, 10d)
                    .setPosition(Position.Absolute)
                    .setX(85d).setY(128.5)
                    .setBackgroundColor(30, 144, 255)
                    .setMargin(10)
                    .setBorder(10)
                    .setPadding(10);
            vPage.add(e);
            ofdDoc.addVPage(vPage);
        }


    }
}