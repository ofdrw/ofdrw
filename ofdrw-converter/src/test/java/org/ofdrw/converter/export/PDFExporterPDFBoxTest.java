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

    /**
     * 图片覆盖整个页面
     */
    @Test
    void testImageOverridePage() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testImageOverridePage.ofd");
        Path pdfOut = Paths.get("target/testImageOverridePage.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 增加Path裁剪逻辑
     */
    @Test
    void testPathClip() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testPathClip.ofd");
        Path pdfOut = Paths.get("target/testPathClip.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 补充填充规则逻辑
     */
    @Test
    void testFillRule() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testImageNotFound.ofd");
        Path pdfOut = Paths.get("target/testFillRule.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 优化Path颜色逻辑(国徽处)
     */
    @Test
    void testFillColor() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testPathClip.ofd");
        Path pdfOut = Paths.get("target/testPathColor.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 优化线宽和文字的比例(印章处)
     */
    @Test
    void testScaleOfLineWidthAndTextPoint() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/SignScaleError.ofd");
        Path pdfOut = Paths.get("target/testScaleOfLineWidthAndTextPoint.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

    /**
     * 优化字体加载机制，嵌入式字体无法显示，字体加载失败等
     */
    @Test
    void testFontLoad() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/SignScaleError.ofd");
        Path pdfOut = Paths.get("target/testFontLoad.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }



    /**
     * 测试Path对象的填充透明度的问题
     */
    @Test
    void testPathFillOpacity() throws IOException {
        Path ofdIn = Paths.get("src/test/resources/testPathFillOpacity.ofd");
        Path pdfOut = Paths.get("target/testPathFillOpacity.pdf");
        try (OFDExporter exporter = new PDFExporterIText(ofdIn, pdfOut)) {
            exporter.export();
        }
        System.out.println(">> " + pdfOut.toAbsolutePath());
    }

}