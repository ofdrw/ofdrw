package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicStructure.res.CT_MultiMedia;
import org.ofdrw.core.basicStructure.res.MediaType;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 12：禁止音频视频资源（GB/T 42133-2022 6.2.6g）
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class AudioVideoRule implements ArchiveRule {
    public static final String RULE_NAME = "AUDIO_VIDEO";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return violations;

        try {
            List<CT_MultiMedia> mediaList = resMgt.getMultiMedias();
            if (mediaList != null) {
                for (CT_MultiMedia media : mediaList) {
                    MediaType type = media.getType();
                    if (type == MediaType.Audio || type == MediaType.Video) {
                        String id = media.getID() != null ? media.getID().toString() : "?";
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "OFD-A 不允许" + type + "资源 (ID=" + id + ")",
                                media.getMediaFile(),
                                type.toString(), "删除"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查音视频资源时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
