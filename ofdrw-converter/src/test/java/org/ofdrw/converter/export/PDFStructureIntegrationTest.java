package org.ofdrw.converter.export;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.pdf.render.GraphicsStateResolver;
import org.ofdrw.converter.pdf.render.ImageRendererImpl;
import org.ofdrw.converter.pdf.render.ImageResourceLoader;
import org.ofdrw.converter.pdf.render.PathRenderer;
import org.ofdrw.converter.pdf.render.TextRendererImpl;
import org.ofdrw.converter.pdf.write.PdfStructureWriter;
import org.ofdrw.converter.pdf.write.StructureSource;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 结构映射端到端测试（P5）
 * <p>
 * 走“真实 OFD 样本 → {@link StructureSource} 抽取 → {@link PdfStructureWriter} 写入 → 保存并回读”的完整链路，
 * 验证结构信息确实落入了 PDF 文件。
 * <p>
 * 说明：P5 阶段只验证结构写入；页面内容的绘制由 {@code PDFExporter}（t10）接入完整管线后统一回归，
 * 因此这里创建空白页面承载结构，像素级结果不在本测试断言范围内。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PDFStructureIntegrationTest {

    /**
     * 入库样本目录
     */
    private static final Path SAMPLES = Paths.get("src/test/resources/pdf-match");

    /**
     * 临时目录
     */
    @TempDir
    Path tempDir;

    /**
     * 对指定样本执行“抽取结构 → 写入 PDF → 回读”
     *
     * @param sample 样本名（不含扩展名）
     * @return 回读的文档，调用方负责关闭
     * @throws Exception 读写失败
     */
    private PDDocument exportStructure(String sample) throws Exception {
        Path ofd = SAMPLES.resolve(sample + ".ofd");
        Path pdf = tempDir.resolve(sample + ".pdf");
        try (OFDReader reader = new OFDReader(ofd);
             PDDocument document = new PDDocument()) {
            List<String> pageIds = new ArrayList<>();
            List<org.apache.pdfbox.pdmodel.PDPage> pdfPages = new ArrayList<>();
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                pageIds.add(reader.getPageObjectId(i).toString());
                org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage();
                document.addPage(page);
                pdfPages.add(page);
            }
            StructureSource source = StructureSource.load(reader, null);
            new PdfStructureWriter(document, source, pdfPages, pageIds).writeAll();
            document.save(pdf.toFile());
        }
        return Loader.loadPDF(pdf.toFile());
    }

    @Test
    @DisplayName("书签：testImageNotFound 的 OFD Outline 落成 PDF /Outlines")
    void outlinesWrittenToPdf() throws Exception {
        try (PDDocument pdf = exportStructure("testImageNotFound")) {
            assertNotNull(pdf.getDocumentCatalog().getDocumentOutline(), "应写入大纲");
            List<PDOutlineItem> items = new ArrayList<>();
            for (PDOutlineItem item : pdf.getDocumentCatalog().getDocumentOutline().children()) {
                items.add(item);
            }
            assertTrue(items.size() >= 2, "书签数量：" + items.size());
            for (PDOutlineItem item : items) {
                assertNotNull(item.getTitle());
                assertNotNull(item.getDestination(), "决策 25：书签应带目标：" + item.getTitle());
            }
        }
    }

    @Test
    @DisplayName("附件：AddAttachment 的附件落成 /Names /EmbeddedFiles")
    void attachmentsWrittenToPdf() throws Exception {
        try (PDDocument pdf = exportStructure("AddAttachment")) {
            assertNotNull(pdf.getDocumentCatalog().getNames(), "应写入 /Names");
            assertNotNull(pdf.getDocumentCatalog().getNames().getEmbeddedFiles(), "应写入 /EmbeddedFiles");
            assertEquals(1, pdf.getDocumentCatalog().getNames().getEmbeddedFiles().getNames().size());
        }
    }

    @Test
    @DisplayName("注释：AddWatermarkAnnot 的 Watermark 注释落成页面 /Annots")
    void annotationsWrittenToPdf() throws Exception {
        try (PDDocument pdf = exportStructure("AddWatermarkAnnot")) {
            int total = 0;
            for (org.apache.pdfbox.pdmodel.PDPage page : pdf.getPages()) {
                total += page.getAnnotations().size();
            }
            assertTrue(total >= 1, "应至少写入 1 条注释");
        }
    }

    @Test
    @DisplayName("元数据：/Info 含 Producer，样本标题/作者按需写入")
    void metadataWrittenToPdf() throws Exception {
        try (PDDocument pdf = exportStructure("helloworld")) {
            org.apache.pdfbox.pdmodel.PDDocumentInformation info = pdf.getDocumentInformation();
            assertEquals(org.ofdrw.converter.pdf.structure.MetadataMapper.PRODUCER, info.getProducer());
        }
    }

    @Test
    @DisplayName("无结构样本：不产生大纲与附件，导出不报错")
    void sampleWithoutStructure() throws Exception {
        try (PDDocument pdf = exportStructure("helloworld")) {
            assertEquals(null, pdf.getDocumentCatalog().getDocumentOutline());
            assertEquals(null, pdf.getDocumentCatalog().getNames());
        }
    }
}
