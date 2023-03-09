package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HTMLExporterTest {

    @Test
    void export()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.html");
        try (HTMLExporter exporter = new HTMLExporter(ofdPath, pdfPath)) {
            exporter.export();
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }

    @Test
    void exportMulti()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999-multi.html");
        try (HTMLExporter exporter = new HTMLExporter(ofdPath, pdfPath)) {
            exporter.export(0,1);
            exporter.export(0);
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }
}