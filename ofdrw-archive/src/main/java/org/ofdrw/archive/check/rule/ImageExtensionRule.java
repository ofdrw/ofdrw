package org.ofdrw.archive.check.rule;

import org.dom4j.Element;
import org.dom4j.Namespace;
import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.core.Const;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 13：图像资源禁止使用扩展机制加入自定义数据（GB/T 42133-2022 6.2.6f）
 * <p>
 * 检查 Image 类型的 MultiMedia 中是否包含非标准 OFD 命名空间的子元素。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class ImageExtensionRule implements ArchiveRule {
    public static final String RULE_NAME = "IMAGE_EXTENSION";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return violations;

        try {
            List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
            if (mediaList != null) {
                for (CT_MultiMedia media : mediaList) {
                    if (media.getType() != MediaType.Image) continue;

                    // 检查是否有非 OFD 命名空间的子元素
                    @SuppressWarnings("unchecked")
                    List<Element> children = media.elements();
                    for (Element child : children) {
                        Namespace ns = child.getNamespace();
                        // 非空且非 OFD 命名空间视为自定义扩展
                        if (ns != null && !Const.OFD_NAMESPACE_URI.equals(ns.getURI())) {
                            violations.add(new ArchiveViolation(
                                    RULE_NAME, ArchiveViolation.Severity.WARN,
                                    "图像资源包含自定义扩展数据: " + child.getName(),
                                    media.getMediaFile(),
                                    child.getName(), "删除"));
                        }
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查图像扩展时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
