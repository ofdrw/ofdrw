package org.ofdrw.converter.pdf.structure;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 附件与元数据映射测试
 *
 * @author 权观宇
 * @since 2.5.1
 */
class AttachmentAndMetadataMapperTest {

    @Test
    @DisplayName("附件：保留文件名、创建时间与 MIME")
    void mapsAttachmentMetadata() {
        CT_Attachment attachment = new CT_Attachment();
        attachment.setID("1");
        attachment.setAttachmentName("发票明细.xml");
        attachment.setCreationDate(LocalDateTime.of(2020, 5, 15, 10, 30));
        attachment.setFileLoc(new ST_Loc("/Doc_0/Attachs/attach_1.xml"));
        attachment.setSize(1234d);
        List<CT_Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);

        AttachmentMapper.Result<AttachmentMapper.PdfAttachment> result =
                new AttachmentMapper().map(attachments);

        assertEquals(1, result.getItems().size());
        AttachmentMapper.PdfAttachment pdf = result.getItems().get(0);
        assertEquals("发票明细.xml", pdf.getFileName());
        assertEquals("/Doc_0/Attachs/attach_1.xml", pdf.getFilePath());
        assertEquals(LocalDateTime.of(2020, 5, 15, 10, 30), pdf.getCreationDate());
        assertEquals("application/xml", pdf.getMimeType());
        assertEquals(1234L, pdf.getSize().longValue());
    }

    @Test
    @DisplayName("附件：未声明名称时用实际文件名并告警")
    void fallsBackToFileName() {
        CT_Attachment attachment = new CT_Attachment();
        attachment.setID("1");
        attachment.setFileLoc(new ST_Loc("/Doc_0/Attachs/photo.JPG"));
        List<CT_Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);

        AttachmentMapper.Result<AttachmentMapper.PdfAttachment> result = new AttachmentMapper().map(attachments);
        assertEquals("photo.JPG", result.getItems().get(0).getFileName());
        assertEquals("image/jpeg", result.getItems().get(0).getMimeType());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("AttachmentName")), result.getWarnings().toString());
    }

    @Test
    @DisplayName("附件：既无名称也无路径时跳过并告警")
    void skipsNameless() {
        CT_Attachment attachment = new CT_Attachment();
        attachment.setID("1");
        List<CT_Attachment> attachments = new ArrayList<>();
        attachments.add(attachment);

        AttachmentMapper.Result<AttachmentMapper.PdfAttachment> result = new AttachmentMapper().map(attachments);
        assertTrue(result.isEmpty());
        assertTrue(result.getWarnings().stream().anyMatch(w -> w.contains("缺少文件名")), result.getWarnings().toString());
    }

    @Test
    @DisplayName("附件：无附件时返回空结果")
    void emptyAttachments() {
        assertTrue(new AttachmentMapper().map(null).isEmpty());
    }

    @Test
    @DisplayName("元数据：标题/作者/主题/关键字/创建工具/日期写入，Producer 固定")
    void mapsDocumentInfo() {
        CT_DocInfo docInfo = new CT_DocInfo();
        docInfo.setTile("测试文档");
        docInfo.setAuthor("OFD R&W");
        docInfo.setSubject("回归样本");
        org.ofdrw.core.basicStructure.ofd.docInfo.Keywords keywords =
                new org.ofdrw.core.basicStructure.ofd.docInfo.Keywords();
        keywords.addKeyword("ofd");
        keywords.addKeyword("pdf");
        docInfo.setKeywords(keywords);
        docInfo.setCreator("OFD R&W");
        docInfo.setCreatorVersion("1.2.0");
        docInfo.setCreationDate(LocalDate.of(2020, 4, 25));

        MetadataMapper.Result<MetadataMapper.PdfMetadata> result = new MetadataMapper().map(docInfo);
        MetadataMapper.PdfMetadata metadata = result.getItems().get(0);
        assertEquals("测试文档", metadata.getTitle());
        assertEquals("OFD R&W", metadata.getAuthor());
        assertEquals("回归样本", metadata.getSubject());
        assertEquals("ofd,pdf", metadata.getKeywords());
        assertEquals("OFD R&W 1.2.0", metadata.getCreator());
        assertEquals(MetadataMapper.PRODUCER, metadata.getProducer());
        assertEquals(LocalDate.of(2020, 4, 25).atStartOfDay(), metadata.getCreationDate());
    }

    @Test
    @DisplayName("元数据：文档信息缺失时仍写 Producer")
    void metadataWithoutDocInfo() {
        MetadataMapper.PdfMetadata metadata = new MetadataMapper().map(null).getItems().get(0);
        assertNull(metadata.getTitle());
        assertEquals(MetadataMapper.PRODUCER, metadata.getProducer());
    }
}
