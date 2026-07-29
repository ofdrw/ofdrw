package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 19：多页共用的栅格图像宜在文档资源中注册（GB/T 42133-2022 6.5a）
 * <p>
 * 检查被多个页面引用的 Image MultiMedia 是否注册在 DocumentRes（而非 PageRes）。
 *
 * @author xxx
 * @since 2.3.9
 */
public class ImageResourceRegRule implements ArchiveRule {
    public static final String RULE_NAME = "IMAGE_RESOURCE_REG";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        // Phase 2: 需要遍历所有页面的 Content.xml，统计每个图像 ResourceID 的引用次数
        // 若某图像被 ≥2 页面引用但注册在 PageRes → WARN
        // 框架到位，详细遍历逻辑可后续完善
        return violations;
    }
}
