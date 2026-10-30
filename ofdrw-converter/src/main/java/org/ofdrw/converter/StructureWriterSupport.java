package org.ofdrw.converter;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentNameDictionary;
import org.apache.pdfbox.pdmodel.PDEmbeddedFilesNameTreeNode;
import org.apache.pdfbox.pdmodel.common.filespecification.PDComplexFileSpecification;
import org.apache.pdfbox.pdmodel.common.filespecification.PDEmbeddedFile;
import org.ofdrw.converter.pdf.structure.AttachmentMapper;
import org.ofdrw.converter.pdf.structure.StructureMapper;
import org.ofdrw.reader.OFDReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧接口的文档级信息写入支持（仅内部使用）
 * <p>
 * 历史类 {@link ItextMaker}/{@link PdfboxMaker} 的 {@code addAttachments} 允许调用方把附件写入自建的
 * {@link PDDocument}。签名保留，实现复用 {@link AttachmentMapper}（容器内路径 → 解压目录真实路径的映射
 * 与 PDFBox 对象构造都在那里），不构成第二套结构映射（决策 4）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
final class StructureWriterSupport {

    private static final Logger logger = LoggerFactory.getLogger(StructureWriterSupport.class);

    /**
     * 工具类不允许实例化
     */
    private StructureWriterSupport() {
    }

    /**
     * 把 OFD 附件写入 PDF 的 {@code /Names /EmbeddedFiles}
     *
     * @param pdf       PDF 文档，不能为空
     * @param ofdReader OFD 解析器，不能为空
     * @throws IOException 写入失败
     */
    static void writeAttachments(PDDocument pdf, OFDReader ofdReader) throws IOException {
        if (pdf == null || ofdReader == null) {
            throw new IllegalArgumentException("PDF 文档与 OFD 解析器不能为空");
        }
        List<org.ofdrw.core.attachment.CT_Attachment> attachments = ofdReader.getAttachmentList();
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        AttachmentMapper mapper = new AttachmentMapper();
        StructureMapper.Result<AttachmentMapper.PdfAttachment> mapped = mapper.map(attachments);
        for (String warning : mapped.getWarnings()) {
            logger.warn("[{}] {}", "OFD2PDF", warning);
        }
        if (mapped.isEmpty()) {
            return;
        }
        PDDocumentNameDictionary names = pdf.getDocumentCatalog().getNames();
        if (names == null) {
            names = new PDDocumentNameDictionary(pdf.getDocumentCatalog());
            pdf.getDocumentCatalog().setNames(names);
        }
        PDEmbeddedFilesNameTreeNode efTree = names.getEmbeddedFiles();
        if (efTree == null) {
            efTree = new PDEmbeddedFilesNameTreeNode();
            names.setEmbeddedFiles(efTree);
        }
        Map<String, PDComplexFileSpecification> existing = efTree.getNames();
        Map<String, PDComplexFileSpecification> merged = existing == null
                ? new LinkedHashMap<String, PDComplexFileSpecification>()
                : new LinkedHashMap<>(existing);
        for (AttachmentMapper.PdfAttachment attachment : mapped.getItems()) {
            byte[] data = readFile(attachment.getFilePath());
            if (data == null) {
                continue;
            }
            PDComplexFileSpecification spec = new PDComplexFileSpecification();
            spec.setFile(attachment.getFileName());
            spec.setFileUnicode(attachment.getFileName());
            PDEmbeddedFile embedded = new PDEmbeddedFile(pdf, new java.io.ByteArrayInputStream(data));
            embedded.setSize(data.length);
            spec.setEmbeddedFile(embedded);
            spec.setEmbeddedFileUnicode(embedded);
            merged.put(attachment.getFileName(), spec);
        }
        efTree.setNames(merged);
    }

    /**
     * 读取附件文件内容
     *
     * @param filePath 真实文件路径（已由 AttachmentMapper 从容器内路径换算）
     * @return 文件内容；不存在或读取失败时返回 {@code null} 并告警
     */
    private static byte[] readFile(String filePath) {
        if (filePath == null) {
            return null;
        }
        Path file = java.nio.file.Paths.get(filePath);
        if (!Files.exists(file)) {
            logger.warn("[{}] 附件文件不存在，已跳过：{}", "OFD2PDF", filePath);
            return null;
        }
        try (InputStream in = Files.newInputStream(file)) {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) >= 0) {
                bos.write(buffer, 0, read);
            }
            return bos.toByteArray();
        } catch (IOException e) {
            logger.warn("[{}] 附件读取失败，已跳过：{}（{}）", "OFD2PDF", filePath, e.getMessage());
            return null;
        }
    }
}
