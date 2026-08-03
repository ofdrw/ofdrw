package org.ofdrw.archive;

import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.rule.DocTypeRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * DocTypeRule 测试（GB/T 42133-2022 6.2.1a）
 */
class DocTypeRuleTest {

    @Test
    void testNormalOfdDocTypeViolation() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new DocTypeRule().check(reader, reader.getOFDDir());
            assertEquals(1, violations.size());
            assertEquals("DOC_TYPE", violations.get(0).getRuleName());
            assertEquals(ArchiveViolation.Severity.ERROR, violations.get(0).getSeverity());
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }

    @Test
    void testOFDADocTypePasses() throws Exception {
        Path ofdPath = TestOFDGenerator.createOFDAOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new DocTypeRule().check(reader, reader.getOFDDir());
            assertTrue(violations.isEmpty(), "OFD-A 文件应无 DocType 违规");
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }

    @Test
    void testDocTypeNullDetected() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        // 手动删除 DocType 属性 → 期望检测到"缺失"的 DocType
        TestOFDGenerator.removeDocTypeAttribute(ofdPath);
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new DocTypeRule().check(reader, reader.getOFDDir());
            assertEquals(1, violations.size());
            assertTrue(violations.get(0).getDescription().contains("缺失")
                    || violations.get(0).getDescription().contains("null"));
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }
}
