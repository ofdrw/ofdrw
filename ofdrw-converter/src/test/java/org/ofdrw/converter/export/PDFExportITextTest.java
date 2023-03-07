package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class PDFExportITextTest {

    /**
     * 测试转换全部页面为PDF
     */
    @Test
    void export() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.pdf");
        try (OFDExporter exporter = new PDFExportIText(ofdPath, pdfPath)) {
            exporter.export();
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }
}