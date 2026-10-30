package org.ofdrw.converter.pdf.write;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionURI;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotation;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.ofdrw.converter.pdf.structure.PdfAction;
import org.ofdrw.converter.pdf.structure.PdfDestination;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.action.CT_Action;
import org.ofdrw.core.action.EventType;
import org.ofdrw.core.action.actionType.URI;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.DestType;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.AnnotType;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicStructure.outlines.CT_OutlineElem;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PDF 结构写入测试（P5）
 * <p>
 * 用内存构造的结构源数据驱动 {@link PdfStructureWriter}，再 {@code save → load} 回读 PDF，
 * 断言结构与写出的对象确实落在 PDF 里（而不是只停留在中间描述）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
class PdfStructureWriterTest {

    /**
     * 容限
     */
    private static final double EPS = 1e-6;

    /**
     * 临时目录
     */
    @TempDir
    Path tempDir;

    /**
     * 构造一页 100×100mm 的 OFD 页面
     *
     * @return 页面
     */
    private static Page page() {
        Page page = new Page();
        page.setArea(new CT_PageArea(0, 0, 100, 100));
        return page;
    }

    /**
     * 构造结构源数据
     *
     * @param outlines    大纲
     * @param attachments 附件（可为空）
     * @param docInfo     文档信息（可为空）
     * @param annots      页对象 ID → 注释
     * @return 结构源
     */
    private static StructureSource source(List<CT_OutlineElem> outlines, List<CT_Attachment> attachments,
                                          CT_DocInfo docInfo, Map<String, List<Annot>> annots) {
        Map<String, Integer> index = new HashMap<>();
        index.put("10", 0);
        index.put("20", 1);
        List<Page> pages = new ArrayList<>();
        pages.add(page());
        pages.add(page());
        return new StructureSource(index, pages, outlines, docInfo, attachments, annots, null);
    }

    /**
     * 构造 PDF 文档并写入结构后回读
     *
     * @param source 结构源
     * @param ids    页对象 ID（与 PDF 页一一对应）
     * @return 回读的文档（调用方负责关闭）
     * @throws Exception 写入或读取失败
     */
    private PDDocument writeAndReload(StructureSource source, String... ids) throws Exception {
        Path pdf = tempDir.resolve("out.pdf");
        PDDocument document = new PDDocument();
        List<PDPage> pdfPages = new ArrayList<>();
        for (int i = 0; i < ids.length; i++) {
            PDPage pdfPage = new PDPage(new PDRectangle((float) org.ofdrw.converter.pdf.CoordSystem.mmToPt(100),
                    (float) org.ofdrw.converter.pdf.CoordSystem.mmToPt(100)));
            document.addPage(pdfPage);
            pdfPages.add(pdfPage);
        }
        PdfStructureWriter writer = new PdfStructureWriter(document, source, pdfPages, java.util.Arrays.asList(ids));
        writer.writeAll();
        document.save(pdf.toFile());
        document.close();
        assertTrue(Files.size(pdf) > 0);
        return Loader.loadPDF(pdf.toFile());
    }

    /**
     * 构造带目标的大纲项
     *
     * @param title  标题
     * @param pageId 目标页对象 ID
     * @return 大纲项
     */
    private static CT_OutlineElem outline(String title, long pageId) {
        CT_Dest dest = new CT_Dest();
        dest.setType(DestType.Fit);
        dest.setPageID(new ST_RefID(pageId));
        CT_OutlineElem elem = new CT_OutlineElem(title);
        elem.setActions(new Actions().addAction(new CT_Action(EventType.CLICK, new Goto(dest))));
        return elem;
    }

    @Test
    @DisplayName("书签：标题与目标写入 /Outlines，层级保留")
    void writesOutlines() throws Exception {
        CT_OutlineElem chapter = outline("第一章", 10L);
        chapter.addOutlineElem(outline("第一节", 20L));
        List<CT_OutlineElem> outlines = new ArrayList<>();
        outlines.add(chapter);

        try (PDDocument reloaded = writeAndReload(source(outlines, null, null, null), "10", "20")) {
            org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline root =
                    reloaded.getDocumentCatalog().getDocumentOutline();
            assertNotNull(root, "应写入大纲");
            List<org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem> top = new ArrayList<>();
            for (org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem item : root.children()) {
                top.add(item);
            }
            assertEquals(1, top.size());
            assertEquals("第一章", top.get(0).getTitle());
            assertNotNull(top.get(0).getDestination(), "书签应带目标");
            int children = 0;
            for (org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem ignored : top.get(0).children()) {
                children++;
            }
            assertEquals(1, children, "子书签应保留层级");
        }
    }

    @Test
    @DisplayName("附件：写入 /Names /EmbeddedFiles，保留文件名与 MIME")
    void writesAttachments() throws Exception {
        Path file = tempDir.resolve("明细.xml");
        Files.write(file, "<data>1</data>".getBytes(StandardCharsets.UTF_8));
        CT_Attachment attachment = new CT_Attachment();
        attachment.setID("1");
        attachment.setAttachmentName("明细.xml");
        attachment.setFileLoc(new ST_Loc(file.toAbsolutePath().toString()));
        List<CT_Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);

        try (PDDocument reloaded = writeAndReload(source(null, attachments, null, null), "10")) {
            org.apache.pdfbox.pdmodel.PDDocumentNameDictionary names =
                    reloaded.getDocumentCatalog().getNames();
            assertNotNull(names, "应写入 /Names");
            assertNotNull(names.getEmbeddedFiles(), "应写入 /EmbeddedFiles");
            org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification spec =
                    names.getEmbeddedFiles().getNames().get("明细.xml");
            assertNotNull(spec, "附件名应保留");
            assertNotNull(spec.getEmbeddedFile(), "应嵌入附件内容");
            assertEquals("application/xml", spec.getEmbeddedFile().getSubtype());
        }
    }

    @Test
    @DisplayName("元数据：/Info 写入标题、作者、Creator、Producer")
    void writesMetadata() throws Exception {
        CT_DocInfo docInfo = new CT_DocInfo();
        docInfo.setTile("测试文档");
        docInfo.setAuthor("OFD R&W");
        docInfo.setCreator("OFD R&W");

        try (PDDocument reloaded = writeAndReload(source(null, null, docInfo, null), "10")) {
            org.apache.pdfbox.pdmodel.PDDocumentInformation info = reloaded.getDocumentInformation();
            assertEquals("测试文档", info.getTitle());
            assertEquals("OFD R&W", info.getAuthor());
            assertEquals("OFD R&W", info.getCreator());
            assertEquals(org.ofdrw.converter.pdf.structure.MetadataMapper.PRODUCER, info.getProducer());
        }
    }

    @Test
    @DisplayName("注释：Link 带 URI 写入 /Annots，热区按页面换算")
    void writesLinkAnnotation() throws Exception {
        Annot annot = new Annot();
        annot.setID(new ST_ID(1L));
        annot.setType(AnnotType.Link);
        annot.setAppearance(new Appearance(new ST_Box(10, 20, 30, 40)));
        annot.add(new Actions().addAction(new CT_Action(EventType.CLICK, new URI("https://ofdrw.github.io"))));
        Map<String, List<Annot>> annots = new HashMap<>();
        List<Annot> list = new ArrayList<>();
        list.add(annot);
        annots.put("10", list);

        try (PDDocument reloaded = writeAndReload(source(null, null, null, annots), "10")) {
            List<PDAnnotation> pageAnnots = reloaded.getPage(0).getAnnotations();
            assertEquals(1, pageAnnots.size(), "应写入 1 条注释");
            PDAnnotation annotation = pageAnnots.get(0);
            assertTrue(annotation instanceof PDAnnotationLink);
            PDActionURI uri = (PDActionURI) ((PDAnnotationLink) annotation).getAction();
            assertEquals("https://ofdrw.github.io", uri.getURI());
            PDRectangle rect = annotation.getRectangle();
            assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(10), rect.getLowerLeftX(), 0.5);
            assertEquals(org.ofdrw.converter.pdf.CoordSystem.mmToPt(40), rect.getLowerLeftY(), 0.5);
        }
    }

    @Test
    @DisplayName("注释：Watermark 写入 /Stamp 注释，无动作时不写动作")
    void writesWatermarkAnnotation() throws Exception {
        Annot annot = new Annot();
        annot.setID(new ST_ID(2L));
        annot.setType(AnnotType.Watermark);
        annot.setAppearance(new Appearance(new ST_Box(10, 20, 30, 40)));
        Map<String, List<Annot>> annots = new HashMap<>();
        List<Annot> list = new ArrayList<>();
        list.add(annot);
        annots.put("10", list);

        try (PDDocument reloaded = writeAndReload(source(null, null, null, annots), "10")) {
            List<PDAnnotation> pageAnnots = reloaded.getPage(0).getAnnotations();
            assertEquals(1, pageAnnots.size());
            assertEquals("Stamp", pageAnnots.get(0).getSubtype());
        }
    }

    @Test
    @DisplayName("目标页越界（导出页面子集）：不写目标、不抛异常")
    void destinationOutsideExportedSubset() {
        assertNull(new PdfStructureWriter(new PDDocument(), source(null, null, null, null),
                new ArrayList<>(), new ArrayList<>()).toPdfDestination(PdfDestination.fit(3)));
    }

    @Test
    @DisplayName("GotoA：引用不存在的附件时跳过动作")
    void gotoAMissingAttachmentSkipped() throws Exception {
        Annot annot = new Annot();
        annot.setID(new ST_ID(3L));
        annot.setType(AnnotType.Link);
        annot.setAppearance(new Appearance(new ST_Box(0, 0, 10, 10)));
        annot.add(new Actions().addAction(new CT_Action(EventType.CLICK,
                new org.ofdrw.core.action.actionType.GotoA("not-exist"))));
        Map<String, List<Annot>> annots = new HashMap<>();
        List<Annot> list = new ArrayList<>();
        list.add(annot);
        annots.put("10", list);

        try (PDDocument reloaded = writeAndReload(source(null, null, null, annots), "10")) {
            assertEquals(0, reloaded.getPage(0).getAnnotations().size(), "引用不存在附件的 Link 不生成注释");
        }
    }

    @Test
    @DisplayName("结构源：页对象 ID → 页码与页面框（决策 26）")
    void structureSourcePageBox() {
        StructureSource src = source(null, null, null, null);
        assertEquals(Integer.valueOf(1), src.getPageIndexById().get("20"));
        assertEquals(100, src.pageBox(0).getWidth(), EPS);
        assertEquals(100, src.pageBox(1).getHeight(), EPS);
        assertEquals(210, src.pageBox(9).getWidth(), EPS);
    }

    @Test
    @DisplayName("空结构源：无大纲/附件/元数据时不产生任何结构，writeAll 不抛异常")
    void emptySourceWritesNothing() throws Exception {
        StructureSource src = new StructureSource(null, null, null, null, null, null, null);
        try (PDDocument reloaded = writeAndReload(src, "10")) {
            assertNull(reloaded.getDocumentCatalog().getDocumentOutline());
            assertNull(reloaded.getDocumentCatalog().getNames());
            assertEquals(0, reloaded.getPage(0).getAnnotations().size());
        }
    }
}
