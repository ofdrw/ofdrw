import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.ConvertHelper;
import org.ofdrw.converter.FontLoader;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.export.OFDExporter;
import org.ofdrw.converter.export.PDFExporterPDFBox;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.graphics2d.OFDPageGraphics2D;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;

public class OFD2PDFTest {


    @Deprecated
    @Test
    public void convertPdf() {
//        Path src = Paths.get("src/test/resources/1.ofd");
//        Path dst = Paths.get("target/1.pdf");
//        Path src = Paths.get("src/test/resources/zsbk.ofd");
//        Path dst = Paths.get("target/zsbk.pdf");

        FontLoader.DEBUG = true;
        // 为不规范的字体名创建映射
        FontLoader.getInstance()
                .addAliasMapping("小标宋体", "方正小标宋简体")
                .addSimilarFontReplaceRegexMapping(".*SimSun.*", "SimSun");
        long start = System.currentTimeMillis();
        try {
//            ConvertHelper.toPdf(src, dst);
            ConvertHelper.toPdf(Paths.get("src/test/resources/发票示例.ofd"), Paths.get("target/发票示例.pdf"));
            ConvertHelper.toPdf(Paths.get("src/test/resources/zsbk.ofd"), Paths.get("target/zsbk.pdf"));
            ConvertHelper.toPdf(Paths.get("src/test/resources/999.ofd"), Paths.get("target/999.pdf"));
            System.out.printf(">> 总计花费: %dms\n", System.currentTimeMillis() - start);
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

    /**
     * 测试转换Pattern水印的ofd文件
     */
    @Test
    void testPattern() throws Exception {
        try {
            ConvertHelper.toPdf(Paths.get("src/test/resources/pattern类型.ofd"), Paths.get("target/pattern类型.pdf"));
        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }

    }

    @Test
    void testAnnotImage() throws Exception {
        try {
            ConvertHelper.toPdf(Paths.get("src/test/resources/csfapiao1.ofd"), Paths.get("target/csfapiao1.pdf"));
        } catch (GeneralConvertException e) {
            e.printStackTrace();
        }

    }

    /**
     * 验证转换颜色值异常
     */
    @Test
    void testExportCE() throws Exception {
        Path dst = Paths.get("target", "HelloWorld.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setColor(Color.BLACK);
            g.setFont(new Font("宋体", Font.PLAIN, 7));
            g.drawString("你好OFD Reader & Writer Graphics-2D", 40, 40);
        }
        Path pdfPath = Paths.get("target", "HelloWorld.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(dst, pdfPath)) {
            exporter.export();
        }
    }

    /**
     * 验证包含 JPEG 图片的 OFD 文件转换为 PDF 后文件大小是否正常
     */
    @Test
    void testExportContainsJpeg() throws Exception {
        Path dst = Paths.get("src/test/resources/containsJPEG.ofd");
        Assertions.assertNotNull(dst);
        Path pdfPath = Paths.get("target/containsJPEG.pdf");
        try (OFDExporter exporter = new PDFExporterPDFBox(dst, pdfPath)) {
            exporter.export();
        }
    }

}
