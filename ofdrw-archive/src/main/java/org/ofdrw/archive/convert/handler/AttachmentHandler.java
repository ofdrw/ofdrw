package org.ofdrw.archive.convert.handler;

import org.ofdrw.archive.convert.ArchiveHandler;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * 处理器：附件处理（GB/T 42133-2022 6.15）
 * <p>
 * 仅保留文本格式附件（TXT/XML）；其余移出并保留摘要信息。
 *
 * @author xxx
 * @since 2.3.9
 */
public class AttachmentHandler implements ArchiveHandler {

    /** 可保留的附件格式 */
    private static final java.util.Set<String> KEEP_FORMATS = new java.util.HashSet<>(
            java.util.Arrays.asList("TXT", "XML"));

    @Override
    public void handle(OFDReader reader, OFDDir ofdDir) throws IOException {
        try {
            List<CT_Attachment> attachments = reader.getAttachmentList();
            if (attachments == null || attachments.isEmpty()) {
                return;
            }

            Path workDir = reader.getWorkDir();

            for (CT_Attachment att : attachments) {
                String format = att.getFormat();
                if (format != null && KEEP_FORMATS.contains(format.toUpperCase())) {
                    continue;  // 文本格式保留
                }

                // 删除附件文件
                if (att.getFileLoc() != null) {
                    String path = att.getFileLoc().toString();
                    if (path.startsWith("/")) path = path.substring(1);
                    Path filePath = workDir.resolve(path);
                    Files.deleteIfExists(filePath);
                }

                // 从附件列表中移除
                att.detach();
            }
        } catch (Exception e) {
            throw new IOException("处理附件时异常: " + e.getMessage(), e);
        }
    }
}
