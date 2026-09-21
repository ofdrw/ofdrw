package org.ofdrw.converter.export;

import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.ImageMaker;
import org.ofdrw.converter.SVGMaker;
import org.ofdrw.reader.OFDReader;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyPathExportTest {

    @TempDir
    Path tempDir;

    @Test
    void pdfBoxKeepsLegacyPathPositionAndLineWidth() throws Exception {
        assertLegacyPath(new PDFExporterFactory() {
            @Override
            public OFDExporter create(Path ofd, Path pdf) throws IOException {
                return new PDFExporterPDFBox(ofd, pdf);
            }
        }, "pdfbox");
    }

    @Test
    void iTextKeepsLegacyPathPositionAndLineWidth() throws Exception {
        assertLegacyPath(new PDFExporterFactory() {
            @Override
            public OFDExporter create(Path ofd, Path pdf) throws IOException {
                return new PDFExporterIText(ofd, pdf);
            }
        }, "itext");
    }

    @Test
    void imageMakerKeepsLegacyPathPosition() throws Exception {
        Path ofd = tempDir.resolve("image.ofd");
        createLegacyPathOFD(ofd);

        BufferedImage image;
        try (OFDReader reader = new OFDReader(ofd)) {
            image = new ImageMaker(reader, 10d).makePage(0);
        }

        assertTrue(hasInk(image, 380, 1455, 420, 1475),
                "legacy path should be rendered near its absolute 300 DPI position");
    }

    @Test
    void imageMakerKeepsStandardPathPosition() throws Exception {
        Path ofd = tempDir.resolve("standard-image.ofd");
        createPathOFD(ofd,
                "CTM=\"1 0 0 1 0 0\" Boundary=\"38 146 3 1\" LineWidth=\"0.4\"",
                "M 0 0.5 L 3 0.5");

        BufferedImage image;
        try (OFDReader reader = new OFDReader(ofd)) {
            image = new ImageMaker(reader, 10d).makePage(0);
        }

        assertTrue(hasInk(image, 375, 1455, 420, 1475),
                "standard object-space path should still use Boundary and CTM");
    }

    @Test
    void svgMakerKeepsLegacyPathPositionAndLineWidth() throws Exception {
        Path ofd = tempDir.resolve("svg.ofd");
        createLegacyPathOFD(ofd);

        String svg;
        try (OFDReader reader = new OFDReader(ofd)) {
            SVGMaker maker = new SVGMaker(reader, 10d);
            maker.config.setClip(false);
            svg = maker.makePage(0);
        }

        assertTrue(svg.contains("stroke-width=\"1.3547\""),
                "legacy line width should remain expressed in PDF points");
        assertTrue(svg.contains("transform=\"matrix(3.5278,0,0,3.5278,0,0)\""),
                "PDF points should be converted to millimetres before applying PPM");
        assertTrue(svg.contains("d=\"M109.44 415.68 L116.64 415.68\""),
                "legacy 300 DPI coordinates should be converted to their absolute PDF positions");
    }

    private void assertLegacyPath(PDFExporterFactory factory, String name) throws Exception {
        Path ofd = tempDir.resolve(name + ".ofd");
        Path pdf = tempDir.resolve(name + ".pdf");
        createLegacyPathOFD(ofd);
        try (OFDExporter exporter = factory.create(ofd, pdf)) {
            exporter.export();
        }

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
            parser.parse();
            List<Object> tokens = parser.getTokens();
            assertEquals(1.35467d, operand(tokens, "w", 1, 0), 0.01d);
            assertEquals(456d * 72d / 300d, operand(tokens, "m", 2, 0), 0.001d);
            assertEquals(486d * 72d / 300d, operand(tokens, "l", 2, 0), 0.001d);
        }
    }

    private static double operand(List<Object> tokens, String operator, int operandCount, int operandIndex) {
        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if (token instanceof Operator && operator.equals(((Operator) token).getName())) {
                return ((COSNumber) tokens.get(i - operandCount + operandIndex)).doubleValue();
            }
        }
        throw new AssertionError("Missing PDF operator: " + operator);
    }

    private static boolean hasInk(BufferedImage image, int left, int top, int right, int bottom) {
        for (int y = top; y <= bottom; y++) {
            for (int x = left; x <= right; x++) {
                if ((image.getRGB(x, y) & 0x00FFFFFF) != 0x00FFFFFF) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void createLegacyPathOFD(Path output) throws IOException {
        createPathOFD(output,
                "CTM=\"0.010948 0 0 0.010925 -6.688669 -5.418684\" "
                        + "Boundary=\"38.438663 146.473328 2.963333 0.423333\" LineWidth=\"1.35467\" "
                        + "Join=\"Round\" Cap=\"Round\"",
                "M 456 1732 L 486 1732");
    }

    private static void createPathOFD(Path output, String pathAttributes, String abbreviatedData)
            throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(java.nio.file.Files.newOutputStream(output))) {
            put(zip, "OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org\" Version=\"1.0\" DocType=\"OFD\">"
                    + "<ofd:DocBody><ofd:DocInfo><ofd:DocID>legacy-path-test</ofd:DocID>"
                    + "<ofd:Creator>Foxit OFD Creator</ofd:Creator></ofd:DocInfo>"
                    + "<ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
            put(zip, "Doc_0/Document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org\"><ofd:CommonData>"
                    + "<ofd:PageArea><ofd:PhysicalBox>0 0 210 297</ofd:PhysicalBox></ofd:PageArea>"
                    + "<ofd:MaxUnitID>3</ofd:MaxUnitID></ofd:CommonData><ofd:Pages>"
                    + "<ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/>"
                    + "</ofd:Pages></ofd:Document>");
            put(zip, "Doc_0/Pages/Page_0/Content.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org\"><ofd:Content><ofd:Layer ID=\"2\">"
                    + "<ofd:PathObject ID=\"3\" " + pathAttributes + "><ofd:AbbreviatedData>"
                    + abbreviatedData
                    + "</ofd:AbbreviatedData></ofd:PathObject></ofd:Layer></ofd:Content></ofd:Page>");
        }
    }

    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private interface PDFExporterFactory {
        OFDExporter create(Path ofd, Path pdf) throws IOException;
    }
}
