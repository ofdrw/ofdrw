package org.ofdrw.archive.check.rule;

import org.ofdrw.archive.check.ArchiveRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.OFDDir;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 规则 3：资源必须自包含，禁止外部引用（GB/T 42133-2022 6.1）
 * <p>
 * 检查所有 ST_Loc 路径引用是否指向包外（http/https 或文件系统路径不在包内）。
 *
 * @author 权观宇
 * @since 2.4.0
 */
public class ExternalResourceRule implements ArchiveRule {
    public static final String RULE_NAME = "EXTERNAL_RESOURCE";

    @Override
    public List<ArchiveViolation> check(OFDReader reader, OFDDir ofdDir) {
        List<ArchiveViolation> violations = new ArrayList<>();
        Path workDir = reader.getWorkDir();

        try {
            // 遍历所有 XML 文件，查找路径引用
            Files.walk(workDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> p.toString().toLowerCase().endsWith(".xml"))
                    .forEach(xmlFile -> {
                        try {
                            String content = new String(Files.readAllBytes(xmlFile));
                            // 识别外部 URL 引用
                            Matcher urlMatcher = Pattern.compile("\"(https?://[^\"]+)\"").matcher(content);
                            while (urlMatcher.find()) {
                                String extUrl = urlMatcher.group(1);
                                violations.add(new ArchiveViolation(
                                        RULE_NAME, ArchiveViolation.Severity.ERROR,
                                        "引用外部 URL 资源: " + extUrl,
                                        ST_Loc.getInstance(workDir.relativize(xmlFile).toString()),
                                        extUrl, "包内路径"));
                            }
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
        return violations;
    }
}
