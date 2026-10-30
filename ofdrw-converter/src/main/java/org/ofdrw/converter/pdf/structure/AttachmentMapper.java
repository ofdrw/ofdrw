package org.ofdrw.converter.pdf.structure;

import org.ofdrw.core.attachment.CT_Attachment;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 附件映射器：OFD Attachments → PDF /Names /EmbeddedFiles
 * <p>
 * 保留文件名、创建时间与 MIME 类型（设计 3.8 节）。附件在导出器 {@code close()} 时统一写入（设计 4.0 节契约）。
 * <p>
 * 文件名取值优先级：{@code AttachmentName} > 附件实际文件名；两者都缺失时跳过并 warning。
 * MIME 由文件扩展名推断（OFD {@code Format} 属性是非标准的自由文本，不作为 MIME 依据）。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class AttachmentMapper implements StructureMapper {

    /**
     * 附件描述（库无关）
     */
    public static final class PdfAttachment {

        /**
         * 写入 PDF 的文件名
         */
        private final String fileName;

        /**
         * 实际文件路径（绝对路径），可能为空（仅元数据、无法读取内容）
         */
        private final String filePath;

        /**
         * 创建时间，可能为空
         */
        private final LocalDateTime creationDate;

        /**
         * 修改时间，可能为空
         */
        private final LocalDateTime modDate;

        /**
         * MIME 类型
         */
        private final String mimeType;

        /**
         * 文件大小（字节），可能为空
         */
        private final Long size;

        /**
         * @param fileName     文件名
         * @param filePath     实际文件路径，可为空
         * @param creationDate 创建时间，可为空
         * @param modDate      修改时间，可为空
         * @param mimeType     MIME 类型
         * @param size         文件大小（字节），可为空
         */
        public PdfAttachment(String fileName, String filePath, LocalDateTime creationDate, LocalDateTime modDate,
                             String mimeType, Long size) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.creationDate = creationDate;
            this.modDate = modDate;
            this.mimeType = mimeType;
            this.size = size;
        }

        /**
         * @return 文件名
         */
        public String getFileName() {
            return fileName;
        }

        /**
         * @return 实际文件路径，可能为空
         */
        public String getFilePath() {
            return filePath;
        }

        /**
         * @return 创建时间，可能为空
         */
        public LocalDateTime getCreationDate() {
            return creationDate;
        }

        /**
         * @return 修改时间，可能为空
         */
        public LocalDateTime getModDate() {
            return modDate;
        }

        /**
         * @return MIME 类型
         */
        public String getMimeType() {
            return mimeType;
        }

        /**
         * @return 文件大小（字节），可能为空
         */
        public Long getSize() {
            return size;
        }

        @Override
        public String toString() {
            return "PdfAttachment{" + fileName + ", mime=" + mimeType + ", creation=" + creationDate + '}';
        }
    }

    /**
     * @return 映射器名称
     */
    @Override
    public String getName() {
        return "AttachmentMapper";
    }

    /**
     * 把 OFD 附件列表映射为 PDF 附件描述
     *
     * @param attachments OFD 附件列表，可为空
     * @return 附件描述；无附件时返回空结果
     */
    public Result<PdfAttachment> map(List<CT_Attachment> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return Result.empty();
        }
        List<PdfAttachment> items = new ArrayList<>(attachments.size());
        List<String> warnings = new ArrayList<>();
        for (CT_Attachment attachment : attachments) {
            if (attachment == null) {
                continue;
            }
            String path = attachment.getFileLoc() == null ? null : attachment.getFileLoc().toString();
            // CT_Attachment#getAttachmentName() 在属性缺失时抛 IllegalArgumentException，这里直接读属性文本
            String declaredName = attachment.attributeValue("Name");
            String fileName = declaredName == null || declaredName.trim().isEmpty() ? null : declaredName;
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = fileNameOf(path);
                if (fileName != null) {
                    warnings.add("附件未声明 AttachmentName，已使用实际文件名：" + fileName);
                }
            }
            if (fileName == null) {
                warnings.add("附件缺少文件名，已跳过：" + path);
                continue;
            }
            items.add(new PdfAttachment(fileName, path,
                    attachment.getCreationDateTime(), attachment.getModDateTime(),
                    guessMimeType(fileName),
                    attachment.getSize() == null ? null : attachment.getSize().longValue()));
        }
        return new Result<>(items, warnings);
    }

    /**
     * 取路径中的文件名
     *
     * @param path 路径（{@code /} 或 {@code \} 分隔），可为空
     * @return 文件名；入参为空返回 null
     */
    static String fileNameOf(String path) {
        if (path == null) {
            return null;
        }
        String normalized = path.replace('\\', '/');
        int index = normalized.lastIndexOf('/');
        String name = index >= 0 ? normalized.substring(index + 1) : normalized;
        return name.isEmpty() ? null : name;
    }

    /**
     * 按扩展名推断 MIME
     * <p>
     * 只覆盖常见类型，未命中时返回 {@code application/octet-stream}（不影响 PDF 合法性）。
     *
     * @param fileName 文件名
     * @return MIME 类型
     */
    static String guessMimeType(String fileName) {
        if (fileName == null) {
            return "application/octet-stream";
        }
        String lower = fileName.toLowerCase();
        int dot = lower.lastIndexOf('.');
        String ext = dot < 0 ? "" : lower.substring(dot + 1);
        switch (ext) {
            case "pdf":
                return "application/pdf";
            case "xml":
                return "application/xml";
            case "ofd":
                return "application/ofd";
            case "txt":
                return "text/plain";
            case "json":
                return "application/json";
            case "html":
            case "htm":
                return "text/html";
            case "csv":
                return "text/csv";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "bmp":
                return "image/bmp";
            case "tif":
            case "tiff":
                return "image/tiff";
            case "svg":
                return "image/svg+xml";
            case "zip":
                return "application/zip";
            case "doc":
                return "application/msword";
            case "docx":
                return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "xls":
                return "application/vnd.ms-excel";
            case "xlsx":
                return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            default:
                return "application/octet-stream";
        }
    }
}
