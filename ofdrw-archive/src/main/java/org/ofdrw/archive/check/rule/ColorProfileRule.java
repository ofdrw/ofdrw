package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 24：颜色空间建议带有颜色配置文件（GB/T 42133-2022 6.3.1c）
 * <p>
 * 仅 INFO 级别提示，不做转换。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ColorProfileRule implements ArchiveRule {
    public static final String RULE_NAME = "COLOR_PROFILE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return violations;

        try {
            List<CT_ColorSpace> colorSpaces = resMgt.getColorSpaces();
            if (colorSpaces != null) {
                for (CT_ColorSpace cs : colorSpaces) {
                    if (cs.getProfile() == null) {
                        String id = cs.getID() != null ? cs.getID().toString() : "?";
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.INFO,
                                "建议为颜色空间添加 ICC 颜色配置文件 (ID=" + id + ")",
                                ST_Loc.getInstance("PublicRes.xml"),
                                "缺失", "ICC Profile"));
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
        return violations;
    }
}
