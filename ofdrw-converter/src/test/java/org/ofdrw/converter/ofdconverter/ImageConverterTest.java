package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ImageConverterTest {

    @Test
    void convert() throws Exception{
        Path dst = Paths.get("target/IMAGE-PAGES.ofd");
        Path img1 = Paths.get("src/test/resources/img.jpg");
        Path img2 = Paths.get("src/test/resources/ofd2html.jpg");

        try (ImageConverter converter = new ImageConverter(dst)) {
            converter.convert(img1);
            converter.convert(img2);
            converter.convert(img1);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}