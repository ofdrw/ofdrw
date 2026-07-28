package org.ofdrw.converter.export;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageBlendModeExportTest {

    @TempDir
    Path tempDir;

    @Test
    void itextUsesNormalBlendModeForOpaqueImages() throws Exception {
        assertOpaqueImageCoversEarlierContent("itext", PDFExporterIText::new);
    }

    @Test
    void pdfboxUsesNormalBlendModeForOpaqueImages() throws Exception {
        assertOpaqueImageCoversEarlierContent("pdfbox", PDFExporterPDFBox::new);
    }

    private void assertOpaqueImageCoversEarlierContent(String name, ExporterFactory factory) throws Exception {
        Path ofd = tempDir.resolve(name + ".ofd");
        Path pdf = tempDir.resolve(name + ".pdf");
        createOverlappingImageDocument(ofd);

        try (OFDExporter exporter = factory.create(ofd, pdf)) {
            exporter.export();
        }

        try (PDDocument document = PDDocument.load(pdf.toFile())) {
            BufferedImage page = new PDFRenderer(document).renderImageWithDPI(0, 144);
            Color background = new Color(page.getRGB(page.getWidth() / 10, page.getHeight() / 10));
            Color covered = new Color(page.getRGB(page.getWidth() / 2, page.getHeight() / 2));

            assertTrue(isAlmostBlack(background), "The lower black layer was not rendered");
            assertTrue(isAlmostWhite(covered),
                    "An opaque white image must cover earlier content, but was " + covered);
        }
    }

    private void createOverlappingImageDocument(Path destination) throws IOException {
        BufferedImage whiteImage = new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB);
        Graphics2D imageGraphics = whiteImage.createGraphics();
        try {
            imageGraphics.setColor(Color.WHITE);
            imageGraphics.fillRect(0, 0, whiteImage.getWidth(), whiteImage.getHeight());
        } finally {
            imageGraphics.dispose();
        }

        try (OFDGraphicsDocument document = new OFDGraphicsDocument(destination)) {
            OFDPageGraphics2D page = document.newPage(100, 100);
            page.setColor(Color.BLACK);
            page.fillRect(0, 0, 100, 100);
            page.drawImage(whiteImage, 20, 20, 60, 60, null);
        }
    }

    private boolean isAlmostBlack(Color color) {
        return color.getRed() < 20 && color.getGreen() < 20 && color.getBlue() < 20;
    }

    private boolean isAlmostWhite(Color color) {
        return color.getRed() > 235 && color.getGreen() > 235 && color.getBlue() > 235;
    }

    @FunctionalInterface
    private interface ExporterFactory {
        OFDExporter create(Path ofd, Path pdf) throws IOException;
    }
}
