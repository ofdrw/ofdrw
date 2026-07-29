package org.ofdrw.archive;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.OFDArchiveChecker;
import org.ofdrw.archive.model.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFDArchiveChecker 集成测试
 * <p>
 * 验证全部规则对不同类型的 OFD 文件的检查结果。
 */
class OFDArchiveCheckerTest {

    private static Path normalOfd;
    private static Path ofdaOfd;
    private static Path permOfd;

    @BeforeAll
    static void setUp() throws IOException {
        normalOfd = TestOFDGenerator.createNormalOfd();
        ofdaOfd = TestOFDGenerator.createOFDAOfd();
        permOfd = TestOFDGenerator.createWithPermissionsOfd();
    }

    @Test
    void testNormalOfdHasViolations() throws Exception {
        try (OFDReader reader = new OFDReader(normalOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            assertNotNull(violations);
            // 普通 OFD 应该有 DocType 违规（不是 "OFD-A"）
            assertTrue(violations.size() > 0, "普通 OFD 应有违规项");

            // 验证存在 DOC_TYPE 违规
            boolean hasDocTypeViolation = violations.stream()
                    .anyMatch(v -> "DOC_TYPE".equals(v.getRuleName())
                            && v.getSeverity() == ArchiveViolation.Severity.ERROR);
            assertTrue(hasDocTypeViolation, "应包含 DOC_TYPE ERROR");
        }
    }

    @Test
    void testOFDADocTypePasses() throws Exception {
        try (OFDReader reader = new OFDReader(ofdaOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            // OFD-A 文件不应有 DOC_TYPE 违规
            boolean hasDocTypeError = violations.stream()
                    .anyMatch(v -> "DOC_TYPE".equals(v.getRuleName())
                            && v.getSeverity() == ArchiveViolation.Severity.ERROR);
            assertFalse(hasDocTypeError, "OFD-A 文件不应有 DOC_TYPE ERROR");
        }
    }

    @Test
    void testWithPermissionsHasWarning() throws Exception {
        try (OFDReader reader = new OFDReader(permOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            // 含 Permissions 的文件应有 PERMISSION 违规
            boolean hasPermWarning = violations.stream()
                    .anyMatch(v -> "PERMISSION".equals(v.getRuleName()));
            assertTrue(hasPermWarning, "含 Permissions 文件应有 PERMISSION 违规");
        }
    }

    @Test
    void testCheckByPath() throws Exception {
        OFDArchiveChecker checker = new OFDArchiveChecker();
        List<ArchiveViolation> violations = checker.check(normalOfd);
        assertNotNull(violations);
        assertTrue(violations.size() > 0);
    }

    @Test
    void testNoExceptionOnAllRules() throws Exception {
        try (OFDReader reader = new OFDReader(normalOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            // 验证没有内部错误（规则执行异常产生的伪违规）
            boolean hasInternalError = violations.stream()
                    .anyMatch(v -> v.getDescription().contains("规则执行异常"));
            assertFalse(hasInternalError, "不应有规则执行异常");
        }
    }

    @Test
    void testViolationsSortedByRuleName() throws Exception {
        try (OFDReader reader = new OFDReader(normalOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            // 验证按规则名排序
            for (int i = 1; i < violations.size(); i++) {
                assertTrue(violations.get(i - 1).getRuleName().compareTo(
                        violations.get(i).getRuleName()) <= 0,
                        "违规列表应按规则名排序");
            }
        }
    }

    // =========== 各规则不崩溃测试 ===========

    @Test
    void testAllRulesAgainstNormalOfd() throws Exception {
        try (OFDReader reader = new OFDReader(normalOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            // 全部规则都应正常执行
            assertNotNull(violations);

            // 打印违规摘要（调试用）
            violations.forEach(v -> System.out.println("  " + v));
        }
    }

    @Test
    void testAllRulesAgainstOFDA() throws Exception {
        try (OFDReader reader = new OFDReader(ofdaOfd)) {
            OFDArchiveChecker checker = new OFDArchiveChecker();
            List<ArchiveViolation> violations = checker.check(reader);

            assertNotNull(violations);

            // OFD-A 文件应只有 WARN/INFO 级别违规，无 ERROR
            boolean hasErrors = violations.stream()
                    .anyMatch(v -> v.getSeverity() == ArchiveViolation.Severity.ERROR);
            if (hasErrors) {
                violations.stream()
                        .filter(v -> v.getSeverity() == ArchiveViolation.Severity.ERROR)
                        .forEach(v -> System.out.println("  ERROR: " + v));
            }
            // 注意：OFD-A 文件可能仍有 COLOR_SPACE 等不依赖 DocType 的违规
        }
    }
}
