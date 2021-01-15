import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.io.font.otf.Glyph;
import com.itextpdf.io.font.otf.GlyphLine;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import org.apache.commons.io.FileUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.PdfBoxFontHolder;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class OFD2PDFTest {


    @Test
    public void convertPdf() {
        Path src = Paths.get("src/test/resources/发票示例.ofd");
        Path dst = Paths.get("target/发票示例.pdf");
        try {
            ConvertHelper.toPdf(src, dst);
        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }
    }


    @Test
    public void itextGlyph() throws IOException {
        String dst =  "target/glyph.pdf";
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        WriterProperties wp = new WriterProperties().setCompressionLevel(CompressionConstants.NO_COMPRESSION);
        PdfWriter pdfWriter = new PdfWriter(bos,wp);
        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
        PageSize pageSize = new PageSize((float) 240, (float) 297);
        PdfPage pdfPage = pdfDocument.addNewPage(pageSize);
        PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
        pdfPage.setMediaBox(
                new Rectangle(
                       0,0,
                        240, 297
                )
        );
        String ttfPath =  "src/test/resources/font_13132_0_edit.ttf";
        FontProgram fontProgram = FontProgramFactory.createFont(ttfPath);
        PdfFont font =  PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H, true);
        pdfCanvas.setFillColor(new DeviceRgb(0 / 255f,0 / 255f, 0 / 255f));
        pdfCanvas.beginText();
        pdfCanvas.moveText(110, 120);
        pdfCanvas.setFontAndSize(font, 16);
        List<Glyph> glyphs = new ArrayList<>();
        glyphs.add(font.getGlyph(0xE11E));
        pdfCanvas.showText(new GlyphLine(glyphs));
        pdfCanvas.endText();
        pdfDocument.close();
        FileUtils.writeByteArrayToFile(new File(dst), bos.toByteArray());

    }

}