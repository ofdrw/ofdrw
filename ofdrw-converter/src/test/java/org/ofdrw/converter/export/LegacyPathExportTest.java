package org.ofdrw.converter.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSNumber;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    private void assertLegacyPath(PDFExporterFactory factory, String name) throws Exception {
        Path ofd = tempDir.resolve(name + ".ofd");
        Path pdf = tempDir.resolve(name + ".pdf");
        createLegacyPathOFD(ofd);
        try (OFDExporter exporter = factory.create(ofd, pdf)) {
            exporter.export();
        }

        try (PDDocument document = Loader.loadPDF(pdf.toFile())) {
            /*
             * 旧实现把 OFD 的 mm 线宽直接写成 PDF 的 w（1.35467），把 Foxit 的像素坐标按 300dpi 猜成 pt；
             * 新管线统一按 GB/T 33190 语义换算：线宽 1.35467mm → 3.8400pt，
             * 路径点由 CTM(0.010948/0.010925) 与 Boundary 平移换算为页面 pt。
             * 两个历史入口现在委托同一套实现（决策 4），因此断言语义一致。
             */
            // PDFBox 3.x 不再提供 getTokens()，parse() 直接返回指令序列
            PDFStreamParser parser = new PDFStreamParser(document.getPage(0));
            List<Object> tokens = parser.parse();
            assertEquals(1.35467d * 72d / 25.4d, operand(tokens, "w", 1, 0), 0.01d);
            // 起点：(456 × 0.010948 − 6.688669 + 38.438663) mm → pt
            assertEquals((456d * 0.010948d - 6.688669d + 38.438663d) * 72d / 25.4d,
                    operand(tokens, "m", 2, 0), 0.001d);
            // 终点：(486 × 0.010948 − 6.688669 + 38.438663) mm → pt
            assertEquals((486d * 0.010948d - 6.688669d + 38.438663d) * 72d / 25.4d,
                    operand(tokens, "l", 2, 0), 0.001d);
            // 线端/线连接保持 Round
            assertEquals(1d, operand(tokens, "J", 1, 0), 0.001d);
            assertEquals(1d, operand(tokens, "j", 1, 0), 0.001d);
        }
    }

    private static double operand(List<Object> tokens, String operator, int operandCount, int operandIndex) {
        for (int i = 0; i < tokens.size(); i++) {
            Object token = tokens.get(i);
            if (token instanceof Operator && operator.equals(((Operator) token).getName())) {
                // PDFBox 3.x 的 COSNumber 不再提供 doubleValue()，按 double 精度转换 float 值
                return ((COSNumber) tokens.get(i - operandCount + operandIndex)).floatValue();
            }
        }
        throw new AssertionError("Missing PDF operator: " + operator);
    }

    private static void createLegacyPathOFD(Path output) throws IOException {
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
                    + "<ofd:PathObject ID=\"3\" CTM=\"0.010948 0 0 0.010925 -6.688669 -5.418684\" "
                    + "Boundary=\"38.438663 146.473328 2.963333 0.423333\" LineWidth=\"1.35467\" "
                    + "Join=\"Round\" Cap=\"Round\"><ofd:AbbreviatedData>M 456 1732 L 486 1732"
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
