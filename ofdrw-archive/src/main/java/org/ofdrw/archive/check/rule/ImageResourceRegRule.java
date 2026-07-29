package org.ofdrw.archive.check.rule;

import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.io.FileNotFoundException;
import java.util.*;

/**
 * 规则 19：多页共用的栅格图像应在文档资源中注册（GB/T 42133-2022 6.5a）
 * <p>
 * 遍历所有页面统计每个图像 ResourceID 的引用次数。
 * 若某图像被 ≥2 页面引用但注册在 PageRes → WARN。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageResourceRegRule implements ArchiveRule {
    public static final String RULE_NAME = "IMAGE_RESOURCE_REG";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        try {
            int pageCount = reader.getNumberOfPages();
            // 统计每个图像 ResourceID 在哪些页面中被引用
            Map<String, Set<Integer>> imageRefs = new HashMap<>();

            for (int i = 1; i <= pageCount; i++) {
                org.ofdrw.core.basicStructure.pageObj.Page page = reader.getPage(i);
                collectImageRefs(page, i, imageRefs);
            }

            // 检查多页共用的图像
            ResourceManage resMgt = reader.getResMgt();
            for (Map.Entry<String, Set<Integer>> entry : imageRefs.entrySet()) {
                if (entry.getValue().size() >= 2) {
                    // 被多个页面引用
                    CT_MultiMedia media = resMgt.getMultiMedia(entry.getKey());
                    if (media != null && media.getType() == MediaType.Image) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.WARN,
                                "图像资源 (ID=" + entry.getKey() + ") 被 " + entry.getValue().size()
                                        + " 个页面引用，建议注册到 DocumentRes",
                                media.getMediaFile(),
                                "PageRes", "DocumentRes"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查图像注册时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }

    /**
     * 收集页面中所有图像的 ResourceID 引用
     */
    @SuppressWarnings("unchecked")
    private void collectImageRefs(Element container, int pageNum,
                                   Map<String, Set<Integer>> imageRefs) {
        List<Element> children = container.elements();
        for (Element child : children) {
            // ImageObject 引用
            if ("ImageObject".equals(child.getName())) {
                String resId = child.attributeValue("ResourceID");
                if (resId != null) {
                    imageRefs.computeIfAbsent(resId, k -> new HashSet<>()).add(pageNum);
                }
            }
            // 递归处理子元素（PageBlock 嵌套）
            collectImageRefs(child, pageNum, imageRefs);
        }
    }
}
