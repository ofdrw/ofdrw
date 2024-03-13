package org.ofdrw.converter.export;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PDFExporterITextTest {

    /**
     * 测试转换全部页面为PDF
     */
    @Test
    void export() throws Exception {
        Path ofdPath = Paths.get("src/test/resources/999.ofd");
        Path pdfPath = Paths.get("target/999.pdf");
        try (OFDExporter exporter = new PDFExporterIText(ofdPath, pdfPath)) {
            exporter.export();
        }
        System.out.println(">> " + pdfPath.toAbsolutePath());
    }


    /**
     * 忽略无法解析图片
     */
    @Test
    void testErrImgResource() {
        ConvertHelper.useIText();
        Path ofdIn = Paths.get("src/test/resources/testImageNotFound.ofd");
        Path pdfOut = Paths.get("target/testImageNotFound.pdf");
        ConvertHelper.toPdf(ofdIn, pdfOut);
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 图片覆盖整个页面
     */
    @Test
    void testImageOverridePage() {
        ConvertHelper.useIText();
        Path ofdIn = Paths.get("src/test/resources/testImageOverridePage.ofd");
        Path pdfOut = Paths.get("target/testImageOverridePage.pdf");
        ConvertHelper.toPdf(ofdIn, pdfOut);
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 图片覆盖整个页面
     */
    @Test
    void testPathClip() {
        ConvertHelper.useIText();
        Path ofdIn = Paths.get("src/test/resources/testPathClip.ofd");
        Path pdfOut = Paths.get("target/testPathClip.pdf");
        ConvertHelper.toPdf(ofdIn, pdfOut);
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

}