package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.action.actionType.OFDAction;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 6：Document + Page 中不得包含非 Goto 动作（GB/T 42133-2022 6.2.2c/6.2.3c）
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class NonGotoActionRule implements ArchiveRule {
    public static final String RULE_NAME = "NON_GOTO_ACTION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            Document document = reader.getDoc(0);
            if (document != null && document.getActions() != null) {
                // 检查 Document 级别的 Actions
                document.getActions().getActions().forEach(action -> {
                    OFDAction act = action.getAction();
                    if (act != null && !(act instanceof Goto)) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "Document 包含非 Goto 动作: " + act.getClass().getSimpleName(),
                                ST_Loc.getInstance("Document.xml"),
                                act.getClass().getSimpleName(), "Goto"));
                    }
                });
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法检查动作: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
