package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class PDFExporterPDFBoxTest {


    /**
     * 测试转换全部页面为PDF
     */
    @Test
    void export() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdPath, pdfPath)) {
            exporter.export();
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }

    /**
     * 忽略无法解析图片
     */
    @Test
    void testErrImgResource() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testImageNotFound.ofd");
        Path pdfOut = Paths.get("target/testImageNotFound.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

}