package org.ofdrw.archive.check.rule;

import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 17：图像对象插值绘制必须为 false（GB/T 42133-2022 6.5b）
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageInterpolateRule implements ArchiveRule {
    public static final String RULE_NAME = "IMAGE_INTERPOLATE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                ST_Loc pageLoc = reader.getPageAbsLoc(i);
                // 遍历所有 ImageObject
                @SuppressWarnings("unchecked")
                List<Element> imageObjects = page.elements("ImageObject");
                for (Element imgObj : imageObjects) {
                    String interpolate = imgObj.attributeValue("Interpolate");
                    if ("true".equalsIgnoreCase(interpolate)) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "图像对象 Interpolate=true，OFD-A 要求设置为 false",
                                pageLoc, "true", "false"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查图像插值时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
