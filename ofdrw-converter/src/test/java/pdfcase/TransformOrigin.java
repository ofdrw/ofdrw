package pdfcase;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.util.Matrix;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author 权观宇
 * @since 2021-04-19 21:12:14
 */
public class TransformOrigin {

    @Test
    public void testMoveToLeftTop() throws IOException {
        Path outP = Paths.get("target/MoveToLeftTop.pdf");
        try (PDDocument doc = new PDDocument()) {
            final float height = PDRectangle.A4.getHeight();
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            try (PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.OVERWRITE, false, false)) {
                Matrix m = new Matrix();
                m.scale(1,-1);
                m.translate(0, -height);
                contentStream.transform(m);

                contentStream.saveGraphicsState();
                contentStream.setLineWidth(3);
                contentStream.setStrokingColor(0f,0f,0f);

                contentStream.moveTo(0, 0);
                contentStream.lineTo(100,200);
                contentStream.stroke();

                contentStream.moveTo(0, 0);
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 10);
                contentStream.showText("Hello world!");
                contentStream.endText();
                contentStream.restoreGraphicsState();

            }
            doc.save(outP.toFile());
            System.out.println("生成文档位置：" + outP.toAbsolutePath().toString());

        }
    }
}
