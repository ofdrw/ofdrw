package org.ofdrw.converter.ofdconverter;

import org.junit.jupiter.api.Test;
import org.ofdrw.layout.PageLayout;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class TextConverterTest {

    @Test
    void convert() throws Exception {
        Path src = Paths.get("doc/EXPORTER.md");
        Path dst = Paths.get("target/EXPORTER.ofd");
        try (TextConverter converter = new TextConverter(dst)) {
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertFontSize() throws Exception {
        Path src = Paths.get("doc/EXPORTER.md");
        Path dst = Paths.get("target/EXPORTER-5.ofd");
        try (TextConverter converter = new TextConverter(dst)) {
            converter.setFontSize(5);
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void convertA3() throws Exception {
        Path src = Paths.get("doc/EXPORTER.md");
        Path dst = Paths.get("target/EXPORTER-A3.ofd");
        try (TextConverter converter = new TextConverter(dst)) {
            converter.setPageSize(PageLayout.A3());
            converter.convert(src);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}