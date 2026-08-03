package org.ofdrw.archive.check.rule;

import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 14：PageBlock 嵌套深度 ≤ 3（GB/T 42133-2022 6.2.3e）
 * <p>
 * 递归遍历每个页面 Content.xml 中的 CT_PageBlock 树，检查嵌套深度。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class PageBlockDepthRule implements ArchiveRule {
    public static final String RULE_NAME = "PAGEBLOCK_DEPTH";

    /** 最大允许嵌套深度 */
    private static final int MAX_DEPTH = 3;

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                Page page = reader.getPage(i);
                ST_Loc pageLoc = reader.getPageAbsLoc(i);
                // 递归检查 PageBlock 嵌套深度
                @SuppressWarnings("unchecked")
                List<Element> pageBlocks = page.elements("PageBlock");
                for (Element pageBlock : pageBlocks) {
                    checkDepth(reader, pageBlock, 1, pageLoc, violations);
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查 PageBlock 深度时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 递归检查 PageBlock 嵌套深度
     *
     * @param reader     OFD 阅读器
     * @param element    当前元素
     * @param depth      当前深度（1 起始）
     * @param pageLoc    所在页面路径
     * @param violations 违规收集列表
     */
    private void checkDepth(OFDReader reader, Element element, int depth,
                            ST_Loc pageLoc, List<ArchiveViolation> violations) {
        if (depth > MAX_DEPTH) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "PageBlock 嵌套深度超过最大允许值: " + depth + " > " + MAX_DEPTH,
                    pageLoc, String.valueOf(depth), "≤" + MAX_DEPTH));
            return; // 不再递归检查更深层次
        }

        // 递归检查子 PageBlock
        @SuppressWarnings("unchecked")
        List<Element> childBlocks = element.elements("PageBlock");
        for (Element child : childBlocks) {
            checkDepth(reader, child, depth + 1, pageLoc, violations);
        }
    }
}
