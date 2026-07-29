package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

/**
 * 规则 8：去除扩展信息（GB/T 42133-2022 6.2.2e）
 *
 * @author xxx
 * @since 2.3.9
 */
public class ExtensionRule implements ArchiveRule {
    public static final String RULE_NAME = "EXTENSION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            Document document = reader.getDoc(0);
            if (document != null && document.getExtensions() != null) {
                violations.add(new ArchiveViolation(
                        RULE_NAME, ArchiveViolation.Severity.WARN,
                        "文档包含扩展信息，OFD-A 要求去除",
                        ST_Loc.getInstance("Document.xml"),
                        "存在", "无"));
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法检查扩展信息: " + e.getMessage(),
                    null, null, null));
        }
        return violations;
    }
}
