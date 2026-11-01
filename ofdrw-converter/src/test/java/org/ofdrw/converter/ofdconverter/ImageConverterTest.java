package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

@Disabled("图像转 OFD 集成用例，需真实样本。")
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