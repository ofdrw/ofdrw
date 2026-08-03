package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 规则 11：字体必须嵌入子集化字型数据（GB/T 42133-2022 6.2.6b）
 * <p>
 * Phase 1 仅检查字体文件是否嵌入，子集化验证留到 Phase 2。
 *
 * @author 权观宇
 * @since 2.3.9
 */
public class FontSubsetRule implements ArchiveRule {
    public static final String RULE_NAME = "FONT_SUBSET";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        ResourceManage resMgt = reader.getResMgt();
        if (resMgt == null) return violations;

        try {
            List<CT_Font> fonts = resMgt.getFonts();
            if (fonts != null) {
                for (CT_Font font : fonts) {
                    ST_Loc fontFile = font.getFontFile();
                    if (fontFile == null) {
                        String id = font.getID() != null ? font.getID().toString() : "?";
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "字体未嵌入字型文件 (ID=" + id + ")",
                                ST_Loc.getInstance("PublicRes.xml"),
                                "缺失", "嵌入字型文件"));
                        continue;
                    }
                    // 检查字体文件是否存在
                    String path = fontFile.toString();
                    if (path.startsWith("/")) path = path.substring(1);
                    Path filePath = reader.getWorkDir().resolve(path);
                    if (!Files.exists(filePath)) {
                        violations.add(new ArchiveViolation(
                                RULE_NAME, ArchiveViolation.Severity.ERROR,
                                "字体文件不存在: " + path,
                                fontFile, "文件不存在", "有效字体文件"));
                    }
                }
            }
        } catch (Exception e) {
            violations.add(new ArchiveViolation(
                    RULE_NAME, ArchiveViolation.Severity.ERROR,
                    "检查字体嵌入时异常: " + e.getMessage(), null, null, null));
        }
        return violations;
    }
}
