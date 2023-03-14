package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

class ImageExporterTest {

    @Test
    void export() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path imgDirPath = Paths.get("target/999.ofd/");
        try (ImageExporter exporter = new ImageExporter(ofdPath, imgDirPath)) {
            exporter.export();
        }
        System.out.println(">> " + imgDirPath.toAbsolutePath());
    }

    @Test
    void exportMulti() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path imgDirPath = Paths.get("target/999.ofd/");
        try (ImageExporter exporter = new ImageExporter(ofdPath, imgDirPath)) {
            exporter.export(0, 1);
            exporter.export(0);
            List<Path> imgFilePaths = exporter.getImgFilePaths();
            System.out.println(imgFilePaths.toString());
        }
        System.out.println(">> " + imgDirPath.toAbsolutePath());
    }

    @Test
    void exportPPM() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path imgDirPath = Paths.get("target/999.ofd-20ppm/");
        try (ImageExporter exporter = new ImageExporter(ofdPath, imgDirPath, "JPG", 20d)) {
            exporter.export(0);
            exporter.setPPM(10);
            exporter.export(1);
        }
        System.out.println(">> " + imgDirPath.toAbsolutePath());
    }
}