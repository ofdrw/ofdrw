import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PDF2OFDTest {

    @Test
    void pdfbox2ofdrw() throws Exception {

        Path path = Paths.get("C:\\Users\\pc\\Desktop\\006.pdf");
        Path dir = Paths.get("target", path.getFileName().toString());

        Path dst = Paths.get("target/helloworld.ofd");
        Files.createDirectories(dir);
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(dst);
             PDDocument pdfDoc = PDDocument.load(path.toFile())) {
            PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
            for (int pageIndex = 0; pageIndex < pdfDoc.getNumberOfPages(); pageIndex++) {
                PDRectangle pdfPageSize = pdfDoc.getPage(pageIndex).getBBox();
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(pdfPageSize.getWidth(), pdfPageSize.getHeight() );
                pdfRender.renderPageToGraphics(pageIndex, ofdPageG2d);
            }
        }

    }
}
