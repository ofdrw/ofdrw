package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class PDFConverterTest {

    @Test
    void convert() throws Exception {
        Path src = Paths.get("src/test/resources/Test.pdf");
        Path dst = Paths.get("target/Test.ofd");
        try (PDFConverter converter = new PDFConverter(dst)) {
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}