package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.dom4j.DocumentException;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.List;

/**
 * 规则 4：去除权限声明（GB/T 42133-2022 6.2.2a）
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class PermissionRule implements ArchiveRule {
    public static final String RULE_NAME = "PERMISSION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        try {
            Document document = reader.getDoc(0);
            if (document != null && document.getPermission() != null) {
                return Collections.singletonList(new ArchiveViolation(
                        RULE_NAME, ArchiveViolation.Severity.WARN,
                        "文档包含权限声明，OFD-A 建议去除",
                        ST_Loc.getInstance("Document.xml"),
                        "存在", "无"));
            }
        } catch (DocumentException | FileNotFoundException e) {
            return Collections.singletonList(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法读取文档: " + e.getMessage(),
                    ST_Loc.getInstance("Document.xml"), null, null));
        }
        return Collections.emptyList();
    }
}
