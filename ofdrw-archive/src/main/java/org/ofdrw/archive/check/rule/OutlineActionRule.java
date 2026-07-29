package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 7：大纲节点动作检查（GB/T 42133-2022 6.2.5a/b）
 *
 * @author xxx
 * @since 2.3.9
 */
public class OutlineActionRule implements ArchiveRule {
    public static final String RULE_NAME = "OUTLINE_ACTION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            Document document = reader.getDoc(0);
            if (document != null && document.getOutlines() != null) {
                // 遍历大纲节点，检查每个 OutlineElem 的动作
                document.getOutlines().getOutlineElems().forEach(elem -> {
                    if (elem.getActions() != null) {
                        elem.getActions().getActions().forEach(action -> {
                            org.ofdrw.core.action.actionType.OFDAction act = action.getAction();
                            if (act != null && !(act instanceof org.ofdrw.core.action.actionType.actionGoto.Goto)) {
                                violations.add(new ArchiveViolation(
                                        RULE_NAME, ArchiveViolation.Severity.ERROR,
                                        "大纲节点包含非 Goto 动作: " + act.getClass().getSimpleName(),
                                        null, act.getClass().getSimpleName(), "Goto"));
                            }
                        });
                    }
                });
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "无法检查大纲: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
