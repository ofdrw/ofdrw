package org.ofdrw.archive.check.rule;

import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 23：文字对象应使用 Size 属性标识大小（GB/T 42133-2022 6.6c）
 * <p>
 * 检查 TextObject 是否设置了 Size 属性。仅警告，不做转换。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class TextSizeRule implements ArchiveRule {
    public static final String RULE_NAME = "TEXT_SIZE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                ST_Loc pageLoc = reader.getPageAbsLoc(i);
                // 遍历所有 TextObject
                @SuppressWarnings("unchecked")
                List<Element> textObjects = page.elements("TextObject");
                for (Element textObj : textObjects) {
                    String size = textObj.attributeValue("Size");
                    if (size == null || size.isEmpty()) {
                        String id = textObj.attributeValue("ID");
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "文字对象缺少 Size 属性 (ID=" + (id != null ? id : "?") + ")",
                                pageLoc, "缺失", "应设置 Size 属性"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查文字 Size 时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
