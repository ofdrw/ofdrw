import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PDF2OFDTest {

    @Test
    void pdfbox2ofdrw() throws Exception {

        Path path = Paths.get("src/test/resources/Test.pdf");
        Path dir = Paths.get("target", path.getFileName().toString());

        Path dst = Paths.get("target/helloworld.ofd");
        Files.createDirectories(dir);
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(dst);
             PDDocument pdfDoc = Loader.loadPDF(path.toFile())) {
            PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
            for (int pageIndex = 0; pageIndex < pdfDoc.getNumberOfPages(); pageIndex++) {
                PDRectangle pdfPageSize = pdfDoc.getPage(pageIndex).getBBox();
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(pdfPageSize.getWidth(), pdfPageSize.getHeight());
                pdfRender.renderPageToGraphics(pageIndex, ofdPageG2d);
            }
        }

    }


    @Test
    void pdfbox2ofdrwStream() throws Exception {

        Path path = Paths.get("src/test/resources/Test.pdf");
        Path dir = Paths.get("target", path.getFileName().toString());

        Files.createDirectories(dir);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(outputStream);
             PDDocument pdfDoc = Loader.loadPDF(path.toFile())) {
            PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
            for (int pageIndex = 0; pageIndex < pdfDoc.getNumberOfPages(); pageIndex++) {
                PDRectangle pdfPageSize = pdfDoc.getPage(pageIndex).getBBox();
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(pdfPageSize.getWidth(), pdfPageSize.getHeight());
                pdfRender.renderPageToGraphics(pageIndex, ofdPageG2d);
            }
        }
        Path out = Paths.get("target", "Test.ofd");
        Files.write(out, outputStream.toByteArray());

    }
}
