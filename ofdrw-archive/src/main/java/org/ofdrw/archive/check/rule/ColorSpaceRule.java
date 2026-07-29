package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 9：颜色空间仅限 Gray/RGB/CMYK（GB/T 42133-2022 6.3.1b）
 * <p>
 * 只检查，不做转换。OFDColorSpaceType 枚举本身只定义了 GRAY/RGB/CMYK，
 * 非法类型在解析时就会抛出异常。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ColorSpaceRule implements ArchiveRule {
    public static final String RULE_NAME = "COLOR_SPACE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) {
            return violations;
        }

        try {
            List<CT_ColorSpace> colorSpaces = resMgt.getColorSpaces();
            if (colorSpaces != null) {
                for (CT_ColorSpace cs : colorSpaces) {
                    // OFDColorSpaceType 枚举只有 GRAY/RGB/CMYK
                    // 解析时非法类型会抛异常，此处只需要确认是否缺 Profile
                    OFDColorSpaceType type = cs.getType();
                    if (type == null) {
                        String id = cs.getID() != null ? cs.getID().toString() : "?";
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "颜色空间类型未知 (ID=" + id + ")",
                                ST_Loc.getInstance("PublicRes.xml"),
                                "null", "GRAY/RGB/CMYK"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "颜色空间类型不支持: " + e.getMessage(),
                    ST_Loc.getInstance("PublicRes.xml"), null, "GRAY/RGB/CMYK"));
        }
        return violations;
    }
}
