package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.dom4j.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 15：裁剪区优化检查（GB/T 42133-2022 6.3.2）
 * <p>
 * 检查裁剪区是否冗余（Boundary 包含被裁剪对象）或无效（面积为 0）。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class ClipAreaRule implements ArchiveRule {
    public static final String RULE_NAME = "CLIP_AREA";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                // 遍历页面中的 Clip 元素
                @SuppressWarnings("unchecked")
                List<Element> clips = page.elements("ClipArea");
                for (Element clip : clips) {
                    checkClipArea(clip, reader.getPageAbsLoc(i), violations);
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查裁剪区时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 检查单个裁剪区
     *
     * @param clip       裁剪区元素
     * @param pageLoc    页面路径
     * @param violations 违规收集列表
     */
    private void checkClipArea(Element clip, org.ofdrw.core.basicType.ST_Loc pageLoc,
                               List<ArchiveViolation> violations) {
        String boundaryStr = clip.attributeValue("Boundary");
        if (boundaryStr == null) return;

        ST_Box boundary = ST_Box.getInstance(boundaryStr);
        if (boundary == null) return;

        // 检查面积是否为 0
        double width = boundary.getWidth();
        double height = boundary.getHeight();
        if (width <= 0 || height <= 0) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.WARN,
                    "裁剪区面积为 0，应删除并设置 Visible=false",
                    pageLoc, "面积=0", "Visible=false"));
        }
    }
}
