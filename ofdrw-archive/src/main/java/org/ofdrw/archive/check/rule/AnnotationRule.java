package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.annotation.pageannot.Annot;
import org.ofdrw.core.annotation.pageannot.Appearance;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.model.AnnotionEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则：注释合规检查（GB/T 42133-2022 6.10.1）
 * <p>
 * 检查项：
 * <ul>
 *   <li>ReadOnly 不为 true → WARN</li>
 *   <li>NoZoom 不为 true → WARN</li>
 *   <li>NoRotate 不为 true → WARN</li>
 *   <li>Appearance 中存在 PageBlock 嵌套 → WARN</li>
 * </ul>
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class AnnotationRule implements ArchiveRule {
    public static final String RULE_NAME = "ANNOTATION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            List<AnnotionEntity> entities = reader.getAnnotationEntities();
            if (entities == null || entities.isEmpty()) {
                return violations;
            }

            for (AnnotionEntity entity : entities) {
                for (Annot annot : entity.getAnnots()) {
                    String annotId = annot.getID() != null ? annot.getID().toString() : "?";

                    // 检查 ReadOnly
                    if (!Boolean.TRUE.equals(annot.getReadOnly())) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "注释 ReadOnly 应为 true (ID=" + annotId + ")",
                                ST_Loc.getInstance("Annots/Annot_" + entity.getPageId() + ".xml"),
                                String.valueOf(annot.getReadOnly()), "true"));
                    }

                    // 检查 NoZoom
                    if (!Boolean.TRUE.equals(annot.getNoZoom())) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "注释 NoZoom 应为 true (ID=" + annotId + ")",
                                ST_Loc.getInstance("Annots/Annot_" + entity.getPageId() + ".xml"),
                                String.valueOf(annot.getNoZoom()), "true"));
                    }

                    // 检查 NoRotate
                    if (!Boolean.TRUE.equals(annot.getNoRotate())) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "注释 NoRotate 应为 true (ID=" + annotId + ")",
                                ST_Loc.getInstance("Annots/Annot_" + entity.getPageId() + ".xml"),
                                String.valueOf(annot.getNoRotate()), "true"));
                    }

                    // 检查 Appearance 中的 PageBlock 嵌套
                    Appearance appearance = annot.getAppearance();
                    if (appearance != null) {
                        @SuppressWarnings("unchecked")
                        List<org.dom4j.Element> nestedBlocks = appearance.elements("PageBlock");
                        if (!nestedBlocks.isEmpty()) {
                            violations.add(new ArchiveViolation(
                                    RULE_NAME, ArchiveViolation.Severity.WARN,
                                    "注释 Appearance 中存在 PageBlock 嵌套 (ID=" + annotId + ")，"
                                            + "共 " + nestedBlocks.size() + " 层",
                                    ST_Loc.getInstance("Annots/Annot_" + entity.getPageId() + ".xml"),
                                    "存在嵌套", "展平"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查注释时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
