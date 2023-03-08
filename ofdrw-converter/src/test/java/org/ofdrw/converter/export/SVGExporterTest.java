package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SVGExporterTest {

    @Test
    void export()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.ofd");
        try (SVGExporter exporter = new SVGExporter(ofdPath, pdfPath)) {
            exporter.export();
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }

    @Test
    void exportMulti()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.ofd");
        try (SVGExporter exporter = new SVGExporter(ofdPath, pdfPath)) {
            exporter.export(0,1);
            exporter.export(0);
            List<Path> imgFilePaths = exporter.getSvgFilePaths();
            System.out.println(imgFilePaths.toString());
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }
}