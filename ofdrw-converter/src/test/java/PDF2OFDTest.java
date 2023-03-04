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

        Path path = Paths.get("C:\\Users\\pc\\Documents\\我的坚果云\\GB 国标\\GBT 38636-2020 信息安全技术 传输层密码协议（TLCP）.pdf");
        Path dir = Paths.get("target", path.getFileName().toString());

        Path dst = Paths.get("target/helloworld.ofd");
        Files.createDirectories(dir);
        try (OFDGraphicsDocument ofdDoc = new OFDGraphicsDocument(dst);
             PDDocument pdfDoc = PDDocument.load(path.toFile())) {
            PDFRenderer pdfRender = new PDFRenderer(pdfDoc);
            for (int pageIndex = 0; pageIndex < 3; pageIndex++) {
                PDRectangle pdfPageSize = pdfDoc.getPage(pageIndex).getBBox();
                OFDPageGraphics2D ofdPageG2d = ofdDoc.newPage(pdfPageSize.getWidth(), pdfPageSize.getHeight() );
                pdfRender.renderPageToGraphics(pageIndex, ofdPageG2d);
            }
        }

    }
}
