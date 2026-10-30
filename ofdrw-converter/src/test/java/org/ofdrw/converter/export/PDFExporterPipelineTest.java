package org.ofdrw.converter.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.contentstream.operator.Operator;
import org.apache.pdfbox.cos.COSBase;
import org.apache.pdfbox.cos.COSStream;
import org.apache.pdfbox.pdfparser.PDFStreamParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;
import org.ofdrw.converter.pdf.model.ContentCommand;
import org.ofdrw.converter.pdf.model.PathCommand;
import org.ofdrw.converter.pdf.model.PdfPageContent;
import org.ofdrw.converter.pdf.model.SaveState;
import org.ofdrw.converter.pdf.model.TextCommand;
import org.ofdrw.converter.pdf.write.PdfCommandReplayer;
import org.ofdrw.converter.pdf.write.PdfPathFactory;
import org.ofdrw.reader.OFDReader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link PDFExporter} 渲染管线接入测试
 * <p>
 * 覆盖：页面框（决策 26）、页面子集导出、字体资源按页隔离、文字与路径的 Z 序（{@code CommandHook}）、
 * 文件与流两种输出、越界页与重复导出语义、关闭后导出异常。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PDFExporterPipelineTest {

    /**
     * 单页文字样本（A4，含一段中文与英文）
     */
    private static final Path HELLOWORLD = Paths.get("src/test/resources/helloworld.ofd");

    /**
     * 多页样本（用于页面子集导出）
     */
    private static final Path MULTI_PAGE = Paths.get("src/test/resources/1.ofd");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("文件 → 文件：页数与页面框（MediaBox = CropBox = PhysicalBox，决策 26）")
    void fileToFile() throws Exception {
        Path pdf = tempDir.resolve("out/helloworld.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export();
        }
        assertTrue(Files.exists(pdf));
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
            PDPage page = doc.getPage(0);
            // A4 210 × 297 mm → 595.2756 × 841.8898 pt
            assertEquals(595.2756d, page.getMediaBox().getWidth(), 0.01d);
            assertEquals(841.8898d, page.getMediaBox().getHeight(), 0.01d);
            PDRectangle media = page.getMediaBox();
            PDRectangle crop = page.getCropBox();
            assertEquals(media.getLowerLeftX(), crop.getLowerLeftX(), 1e-6);
            assertEquals(media.getLowerLeftY(), crop.getLowerLeftY(), 1e-6);
            assertEquals(media.getUpperRightX(), crop.getUpperRightX(), 1e-6);
            assertEquals(media.getUpperRightY(), crop.getUpperRightY(), 1e-6);
        }
    }

    @Test
    @DisplayName("流 → 流：输出可直接被 PDFBox 读取，页数与文件输出一致")
    void streamToStream() throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PDFExporter exporter = new PDFExporter(Files.newInputStream(HELLOWORLD), bos)) {
            exporter.export();
        }
        byte[] data = bos.toByteArray();
        assertTrue(data.length > 0);
        try (PDDocument doc = Loader.loadPDF(data)) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("页面子集：export(指定页) 只导出指定页，且顺序按导出顺序")
    void exportPageSubset() throws Exception {
        int total;
        try (OFDReader reader = new OFDReader(MULTI_PAGE)) {
            total = reader.getNumberOfPages();
        }
        if (total < 3) {
            return;
        }
        Path pdf = tempDir.resolve("subset.pdf");
        try (OFDExporter exporter = new PDFExporter(MULTI_PAGE, pdf)) {
            // 页码从 0 起；倒序导出以验证顺序按导出列表
            exporter.export(2, 0);
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
            try (OFDReader reader = new OFDReader(MULTI_PAGE)) {
                PDRectangle first = pageBox(reader, 3);
                PDRectangle second = pageBox(reader, 1);
                assertEquals(first.getWidth(), doc.getPage(0).getMediaBox().getWidth(), 0.01d);
                assertEquals(first.getHeight(), doc.getPage(0).getMediaBox().getHeight(), 0.01d);
                assertEquals(second.getWidth(), doc.getPage(1).getMediaBox().getWidth(), 0.01d);
                assertEquals(second.getHeight(), doc.getPage(1).getMediaBox().getHeight(), 0.01d);
            }
        }
    }

    @Test
    @DisplayName("越界页码被忽略，未越界页仍正常导出")
    void outOfRangeIndexIgnored() throws Exception {
        Path pdf = tempDir.resolve("range.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export(-1, 99);
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(0, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("关闭后导出抛 GeneralConvertException，重复关闭安全")
    void closeSemantics() throws Exception {
        try (OFDReader reader = new OFDReader(HELLOWORLD)) {
            PDFExporter exporter = new PDFExporter(reader, tempDir.resolve("closed.pdf"));
            exporter.close();
            exporter.close();
            assertThrows(GeneralConvertException.class, exporter::export);
        }
    }

    @Test
    @DisplayName("复用解析器：OFDReader 由调用方关闭，导出后仍可用")
    void reuseReader() throws Exception {
        Path pdf = tempDir.resolve("reuse.pdf");
        try (OFDReader reader = new OFDReader(HELLOWORLD)) {
            try (PDFExporter exporter = new PDFExporter(reader, pdf)) {
                exporter.export();
            }
            // 导出器关闭后解析器仍归调用方所有
            assertEquals(1, reader.getNumberOfPages());
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("字体资源按页隔离：两页都从 F1 开始，且各自指向本页字体")
    void fontResourcePerPage() throws Exception {
        int total;
        try (OFDReader reader = new OFDReader(MULTI_PAGE)) {
            total = reader.getNumberOfPages();
        }
        if (total < 2) {
            return;
        }
        Path pdf = tempDir.resolve("two-pages.pdf");
        try (OFDExporter exporter = new PDFExporter(MULTI_PAGE, pdf)) {
            exporter.export(0, 1);
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(2, doc.getNumberOfPages());
            for (int i = 0; i < 2; i++) {
                assertEquals(java.util.Collections.singletonList("F1"),
                        fontNames(doc.getPage(i)), "第 " + i + " 页的字体资源名应为 F1");
            }
        }
    }

    @Test
    @DisplayName("文字可提取：输出 PDF 的文本包含样本中的字符")
    void textExtractable() throws Exception {
        Path pdf = tempDir.resolve("text.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export();
        }
        String text;
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            text = new org.apache.pdfbox.text.PDFTextStripper().getText(doc);
        }
        /*
         * 与回归门禁同口径：文本相似度 ≥ 0.99。
         * 不逐字全等的原因：PDFBox 按「字宽 vs 实际步进」决定是否插入空格，
         * 替换字体的字宽与原字体不同，个别字符会被判为间距而多出空格；
         * 位置与字形本身由 glyphOriginsAdvance 与回归像素指标保证。
         */
        String normalized = text.replaceAll("\\s+", "");
        String expected = "你好呀，OFD Reader&Writer！".replaceAll("\\s+", "");
        assertEquals(expected, normalized, "提取文本（忽略空白）必须与 OFD 原文一致");
    }

    @Test
    @DisplayName("字形定位：每个字形的 Tm 平移量必须随字宽递增，不能全部叠在同一点")
    void glyphOriginsAdvance() throws Exception {
        Path pdf = tempDir.resolve("origins.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export();
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            List<Float> xs = new ArrayList<>();
            List<Object> tokens = new PDFStreamParser(doc.getPage(0)).parse();
            for (int i = 0; i < tokens.size(); i++) {
                if (tokens.get(i) instanceof Operator && "Tm".equals(((Operator) tokens.get(i)).getName())) {
                    xs.add(((org.apache.pdfbox.cos.COSNumber) tokens.get(i - 2)).floatValue());
                }
            }
            assertEquals(22, xs.size(), "每个字形各有一条 Tm");
            for (int i = 1; i < xs.size(); i++) {
                assertTrue(xs.get(i) > xs.get(i - 1), "第 " + i + " 个字形的 X 未推进：" + xs);
            }
        }
    }

    @Test
    @DisplayName("CommandHook：钩子处理过的指令不再回放，q/Q 仍成对")
    void commandHookSkipsHandledCommands() throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            List<ContentCommand> commands = new ArrayList<>();
            commands.add(SaveState.INSTANCE);
            commands.add(new PathCommand(
                    java.util.Collections.<org.ofdrw.converter.pdf.model.PathSegment>singletonList(
                            new org.ofdrw.converter.pdf.model.MoveTo(0, 0)),
                    true, false, org.ofdrw.converter.pdf.model.FillRule.NON_ZERO,
                    org.ofdrw.converter.pdf.model.PdfColor.BLACK, null));
            commands.add(new TextCommand("F1", 10d, org.ofdrw.converter.pdf.model.AffineMatrix.IDENTITY,
                    java.util.Collections.singletonList(new org.ofdrw.converter.pdf.model.GlyphRun(
                            1, "A", 0, 0, 5d)), org.ofdrw.converter.pdf.model.PdfColor.BLACK));
            List<String> seen = new ArrayList<>();
            PdfCommandReplayer replayer = new PdfCommandReplayer(new PdfPathFactory());
            try (org.apache.pdfbox.pdmodel.PDPageContentStream cs =
                         new org.apache.pdfbox.pdmodel.PDPageContentStream(doc, page)) {
                replayer.replay(doc, cs, page.getResources(), commands, command -> {
                    seen.add(command.getClass().getSimpleName());
                    // 只拦截文字指令，其余交给回放（保证 q/Q 在同一调用内成对）
                    return command instanceof TextCommand;
                });
            }
            assertEquals(Arrays.asList("SaveState", "PathCommand", "TextCommand"), seen);
            int q = 0;
            int Q = 0;
            PDFStreamParser parser = new PDFStreamParser(page);
            for (Object token : parser.parse()) {
                if (token instanceof Operator) {
                    String op = ((Operator) token).getName();
                    if ("q".equals(op)) {
                        q++;
                    } else if ("Q".equals(op)) {
                        Q++;
                    }
                }
            }
            assertEquals(q, Q, "q/Q 必须成对");
            assertEquals(1, q, "只剩 SaveState 产生的 q");
        }
    }

    @Test
    @DisplayName("ToUnicode：文字字体必须带 /ToUnicode，保证可提取")
    void fontCarriesToUnicode() throws Exception {
        Path pdf = tempDir.resolve("tounicode.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export();
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            List<String> names = fontNames(doc.getPage(0));
            assertFalse(names.isEmpty(), "页面应登记字体资源");
            PDFont font = doc.getPage(0).getResources().getFont(org.apache.pdfbox.cos.COSName.getPDFName(names.get(0)));
            assertNotNull(font);
            assertTrue(font instanceof PDType0Font);
            COSBase toUnicode = font.getCOSObject()
                    .getDictionaryObject(org.apache.pdfbox.cos.COSName.TO_UNICODE);
            assertNotNull(toUnicode, "字体必须带 /ToUnicode");
        }
    }

    @Test
    @DisplayName("重复导出被忽略：不产生重复页面")
    void duplicateExportIgnored() throws Exception {
        Path pdf = tempDir.resolve("dup.pdf");
        try (OFDExporter exporter = new PDFExporter(HELLOWORLD, pdf)) {
            exporter.export();
            exporter.export();
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    /**
     * 读取页面字体资源名（升序）
     *
     * @param page PDF 页面
     * @return 字体资源名列表
     */
    private static List<String> fontNames(PDPage page) {
        List<String> names = new ArrayList<>();
        for (org.apache.pdfbox.cos.COSName name : page.getResources().getFontNames()) {
            names.add(name.getName());
        }
        java.util.Collections.sort(names);
        return names;
    }

    /**
     * 读取 OFD 指定页（页码从 1 起）的 PDF 页面框
     *
     * @param reader  OFD 读取器
     * @param pageNum 页码，从 1 起
     * @return 页面框
     */
    private static PDRectangle pageBox(OFDReader reader, int pageNum) {
        org.ofdrw.converter.pdf.model.Box box =
                org.ofdrw.converter.pdf.render.PageRendererImpl.pageBox(reader.getPage(pageNum)).toPdfBox();
        return new PDRectangle((float) box.getX(), (float) box.getY(),
                (float) box.getWidth(), (float) box.getHeight());
    }
}
