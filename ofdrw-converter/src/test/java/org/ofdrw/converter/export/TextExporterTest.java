package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试导出为纯文本
 */
class TextExporterTest {

    @Test
    void export()throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path txtPath = Paths.get("target/999.txt");
        try (TextExporter exporter = new TextExporter(ofdPath, txtPath)) {
            exporter.export();
        }
        System.out.println(">> " + txtPath.toAbsolutePath());
    }
}