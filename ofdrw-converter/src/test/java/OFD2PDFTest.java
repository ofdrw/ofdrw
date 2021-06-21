import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.GeneralConvertException;

import java.nio.file.Paths;

public class OFD2PDFTest {


    @Test
    public void convertPdf() {
//        Path src = Paths.get("src/test/resources/1.ofd");
//        Path dst = Paths.get("target/1.pdf");
//        Path src = Paths.get("src/test/resources/zsbk.ofd");
//        Path dst = Paths.get("target/zsbk.pdf");

        FontLoader.getInstance()
            .addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
            .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体")

            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*SimSun.*", null, "SimSun")
            .addSimilarFontReplaceRegexMapping(null, ".*Song.*", null, "宋体")
            .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun");

        FontLoader.setSimilarFontReplace(true);

        try {
//            ConvertHelper.toPdf(src, dst);
//            ConvertHelper.toPdf(Paths.get("src/test/resources/signout.ofd"), Paths.get("target/signout.pdf"));
            ConvertHelper.toPdf(Paths.get("src/test/resources/n.ofd"), Paths.get("target/n.pdf"));
//            ConvertHelper.toPdf(Paths.get("src/test/resources/999.ofd"), Paths.get("target/n.pdf"));

        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }
    }


//    @Test
//    public void itextGlyph() throws IOException {
//        String dst =  "target/glyph.pdf";
//        ByteArrayOutputStream bos = new ByteArrayOutputStream();
//        WriterProperties wp = new WriterProperties().setCompressionLevel(CompressionConstants.NO_COMPRESSION);
//        PdfWriter pdfWriter = new PdfWriter(bos,wp);
//        PdfDocument pdfDocument = new PdfDocument(pdfWriter);
//        PageSize pageSize = new PageSize((float) 240, (float) 297);
//        PdfPage pdfPage = pdfDocument.addNewPage(pageSize);
//        PdfCanvas pdfCanvas = new PdfCanvas(pdfPage);
//        pdfPage.setMediaBox(
//                new Rectangle(
//                       0,0,
//                        240, 297
//                )
//        );
//        String ttfPath =  "src/test/resources/font_13132_0_edit.ttf";
//        FontProgram fontProgram = FontProgramFactory.createFont(ttfPath);
//        PdfFont font =  PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H, true);
//        pdfCanvas.setFillColor(new DeviceRgb(0 / 255f,0 / 255f, 0 / 255f));
//        pdfCanvas.beginText();
//        pdfCanvas.moveText(110, 120);
//        pdfCanvas.setFontAndSize(font, 16);
//        List<Glyph> glyphs = new ArrayList<>();
//        glyphs.add(font.getGlyph(0xE11E));
//        pdfCanvas.showText(new GlyphLine(glyphs));
//        pdfCanvas.endText();
//        pdfDocument.close();
//        FileUtils.writeByteArrayToFile(new File(dst), bos.toByteArray());
//
//    }

}
