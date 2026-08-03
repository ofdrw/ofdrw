package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.attachment.CT_Attachment;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则：附件合规检查（GB/T 42133-2022 6.15）
 * <p>
 * OFD-A 中附件应仅保留文本格式（TXT/XML）或已归档的技术文档。
 * 其他类型附件应移出并保留摘要。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class AttachmentRule implements ArchiveRule {
    public static final String RULE_NAME = "ATTACHMENT";

    /** OFD-A 中可保留的附件格式（文本类） */
    private static final java.util.Set<String> KEEP_FORMATS = new java.util.HashSet<>(
            java.util.Arrays.asList("TXT", "XML"));

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            List<CT_Attachment> attachments = reader.getAttachmentList();
            if (attachments == null || attachments.isEmpty()) {
                return violations;
            }

            for (CT_Attachment att : attachments) {
                String format = att.getFormat();
                String name = att.getAttachmentName();

                if (format == null || !KEEP_FORMATS.contains(format.toUpperCase())) {
                    violations.add(new ArchiveViolation(
                            RULE_NAME, ArchiveViolation.Severity.ERROR,
                            "附件 \"" + name + "\" 格式为 " + (format != null ? format : "未知")
                                    + "，OFD-A 建议移出并以摘要信息替代",
                            att.getFileLoc(),
                            format, "TXT/XML"));
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查附件时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
