package org.ofdrw.converter.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.GeneralConvertException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 加密文档（决策 24）与旧接口兼容（决策 4）
 * <ul>
 *     <li>含 {@code Encryptions.xml} 的 OFD 必须直接抛异常，不产生空白页；</li>
 *     <li>历史类 {@code ItextMaker}/{@code PdfboxMaker} 的 {@code makePage}/{@code addAttachments}
 *     必须与 {@code PDFExporter} 走同一渲染管线，而不是抛不支持异常。</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PDFExporterEncryptionAndLegacyTest {

    /**
     * 普通单页样本
     */
    private static final Path SAMPLE = Paths.get("src/test/resources/helloworld.ofd");

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("加密文档：含 Encryptions.xml 时抛 GeneralConvertException，且不产生输出")
    void encryptedDocumentRejected() throws IOException {
        Path ofd = tempDir.resolve("encrypted.ofd");
        createEncryptedOFD(ofd);
        Path pdf = tempDir.resolve("encrypted.pdf");

        PDFExporter exporter = new PDFExporter(ofd, pdf);
        assertThrows(GeneralConvertException.class, exporter::export,
                "加密文档必须直接抛异常，不输出空白页");
        try {
            exporter.close();
        } catch (IOException ignored) {
            // 释放阶段可能因未导出而失败，不影响断言
        }
        assertFalse(Files.size(pdf) > 0, "加密文档不应产生任何 PDF 内容");
    }

    @Test
    @DisplayName("加密文档（流输入）：同样抛异常")
    void encryptedStreamRejected() throws IOException {
        Path ofd = tempDir.resolve("encrypted-stream.ofd");
        createEncryptedOFD(ofd);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (PDDocument ignored = null) {
            // 占位：保持 try 结构，实际断言在下方
        }
        assertThrows(GeneralConvertException.class, () -> {
            try (PDFExporter exporter = new PDFExporter(Files.newInputStream(ofd), bos)) {
                exporter.export();
            } catch (IOException e) {
                throw new GeneralConvertException(e);
            }
        }, "流输入的加密文档同样必须抛异常");
    }

    @Test
    @DisplayName("加密信息为空/损坏：仍视为加密，抛异常不静默放行")
    void brokenEncryptionsRejected() throws IOException {
        Path ofd = tempDir.resolve("broken-encrypted.ofd");
        createOFDWithEncryptions(ofd, "");
        Path pdf = tempDir.resolve("broken.pdf");
        PDFExporter exporter = new PDFExporter(ofd, pdf);
        assertThrows(GeneralConvertException.class, exporter::export,
                "Encryptions.xml 存在但内容为空也必须拒绝转换");
        assertFalse(Files.size(pdf) > 0, "不应产生任何 PDF 内容");
    }

    @Test
    @DisplayName("加密信息不可读：抛异常而不是当作未加密")
    void unreadableEncryptionsRejected() throws IOException {
        Path ofd = tempDir.resolve("unreadable.ofd");
        createEncryptedOFD(ofd);
        Path pdf = tempDir.resolve("unreadable.pdf");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(ofd)) {
            Path encryptions = reader.getWorkDir().resolve("Encryptions.xml");
            assertTrue(Files.exists(encryptions), "工作目录应包含 Encryptions.xml");
            // 取消读权限；若当前文件系统不支持（如 root 或 Windows），跳过该断言
            boolean denied = encryptions.toFile().setReadable(false, false);
            try {
                if (!denied || Files.isReadable(encryptions)) {
                    return;
                }
                PDFExporter exporter = new PDFExporter(reader, pdf);
                assertThrows(GeneralConvertException.class, exporter::export,
                        "加密信息文件不可读时必须抛异常");
            } finally {
                encryptions.toFile().setReadable(true, false);
            }
        }
    }

    @Test
    @DisplayName("未加密文档：正常导出")
    void plainDocumentExported() throws Exception {
        Path pdf = tempDir.resolve("plain.pdf");
        try (OFDExporter exporter = new PDFExporter(SAMPLE, pdf)) {
            exporter.export();
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    @Test
    @DisplayName("旧接口 ItextMaker.makePage：走同一管线，把该页追加到调用方文档")
    void itextMakerMakePage() throws Exception {
        Path pdf = tempDir.resolve("itext-legacy.pdf");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(SAMPLE)) {
            org.ofdrw.converter.ItextMaker maker = new org.ofdrw.converter.ItextMaker(reader);
            try (PDDocument doc = new PDDocument()) {
                maker.makePage(doc, reader.getPageInfo(1));
                assertEquals(1, doc.getNumberOfPages(), "应追加一页");
                doc.save(pdf.toFile());
            }
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
            assertTrue(doc.getPage(0).getMediaBox().getWidth() > 0);
        }
    }

    @Test
    @DisplayName("旧接口 PdfboxMaker.makePage + addAttachments：逐页追加并写入附件")
    void pdfboxMakerMakePageAndAttachments() throws Exception {
        Path pdf = tempDir.resolve("pdfbox-legacy.pdf");
        try (org.ofdrw.reader.OFDReader reader = new org.ofdrw.reader.OFDReader(SAMPLE)) {
            try (PDDocument doc = new PDDocument()) {
                org.ofdrw.converter.PdfboxMaker maker = new org.ofdrw.converter.PdfboxMaker(reader, doc);
                maker.makePage(reader.getPageInfo(1));
                maker.addAttachments(reader);
                assertEquals(1, doc.getNumberOfPages(), "应追加一页");
                doc.save(pdf.toFile());
            }
        }
        try (PDDocument doc = Loader.loadPDF(pdf.toFile())) {
            assertEquals(1, doc.getNumberOfPages());
        }
    }

    /**
     * 构造一个带 {@code Encryptions.xml} 的最小 OFD
     *
     * @param output 输出路径
     * @throws IOException 写出失败
     */
    private static void createEncryptedOFD(Path output) throws IOException {
        createOFDWithEncryptions(output, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                + "<ofd:Encryptions xmlns:ofd=\"http://www.ofdspec.org\" ID=\"1\">"
                + "<ofd:Encryption ID=\"2\" Type=\"1\"/></ofd:Encryptions>");
    }

    /**
     * 构造带指定 {@code Encryptions.xml} 内容的最小 OFD
     *
     * @param output          输出路径
     * @param encryptionsText Encryptions.xml 内容，可为空串（模拟“存在但无内容”）
     * @throws IOException 写出失败
     */
    private static void createOFDWithEncryptions(Path output, String encryptionsText) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(output))) {
            put(zip, "OFD.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:OFD xmlns:ofd=\"http://www.ofdspec.org\" Version=\"1.0\" DocType=\"OFD\">"
                    + "<ofd:DocBody><ofd:DocInfo><ofd:DocID>encrypted</ofd:DocID></ofd:DocInfo>"
                    + "<ofd:DocRoot>Doc_0/Document.xml</ofd:DocRoot></ofd:DocBody></ofd:OFD>");
            // GB/T 33190 第 18 章：加密文档在包根目录放置 Encryptions.xml
            put(zip, "Encryptions.xml", encryptionsText);
            put(zip, "Doc_0/Document.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Document xmlns:ofd=\"http://www.ofdspec.org\"><ofd:CommonData>"
                    + "<ofd:PageArea><ofd:PhysicalBox>0 0 210 297</ofd:PhysicalBox></ofd:PageArea>"
                    + "<ofd:MaxUnitID>3</ofd:MaxUnitID></ofd:CommonData><ofd:Pages>"
                    + "<ofd:Page ID=\"1\" BaseLoc=\"Pages/Page_0/Content.xml\"/></ofd:Pages></ofd:Document>");
            put(zip, "Doc_0/Pages/Page_0/Content.xml", "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                    + "<ofd:Page xmlns:ofd=\"http://www.ofdspec.org\"><ofd:Content><ofd:Layer ID=\"2\"/>"
                    + "</ofd:Content></ofd:Page>");
        }
    }

    /**
     * 写入 ZIP 条目
     *
     * @param zip     ZIP 输出流
     * @param name    条目名
     * @param content 内容
     * @throws IOException 写入失败
     */
    private static void put(ZipOutputStream zip, String name, String content) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(content.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }
}
