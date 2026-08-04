import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.junit.jupiter.api.Test;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;

public class PDF2OFDTest {

    @Test
    void pdfbox2ofdrw() throws Exception {

        Path path = Paths.get("src/test/resources/Test.pdf");

        Path dst = Paths.get("target/helloworld.ofd");
        // 清除上次运行残留（可能是文件或 OFD 解压目录）
        if (Files.exists(dst)) {
            if (Files.isDirectory(dst)) {
                Files.walk(dst)
                     .sorted(Comparator.reverseOrder())
                     .map(Path::toFile)
                     .forEach(File::delete);
            } else {
                Files.delete(dst);
            }
        }
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(dst);
             PDDocument pdfDoc = PDDocument.load(path.toFile())) {
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

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(outputStream);
             PDDocument pdfDoc = PDDocument.load(path.toFile())) {
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
