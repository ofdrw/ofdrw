package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class HTMLExporterTest {

    @Test
    void export()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/n.ofd");
        Path htmlPath = Paths.get("target/999.html");
        try (HTMLExporter exporter = new HTMLExporter(ofdPath, htmlPath)) {
            exporter.export();
        }
        System.out.println(">> " + htmlPath.toAbsolutePath());
    }

    @Test
    void exportMulti()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/n.ofd");
        Path htmlPath = Paths.get("target/n-multi.html");
        try (HTMLExporter exporter = new HTMLExporter(ofdPath, htmlPath)) {
            exporter.export(0,1);
            exporter.export(0);
        }
        System.out.println(">> " + htmlPath.toAbsolutePath());
    }
}