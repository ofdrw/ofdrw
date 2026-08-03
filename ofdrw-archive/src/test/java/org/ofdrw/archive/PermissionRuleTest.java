package org.ofdrw.archive;

import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.rule.PermissionRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PermissionRule 测试（GB/T 42133-2022 6.2.2a）
 */
class PermissionRuleTest {

    @Test
    void testNormalOfdNoPermission() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new PermissionRule().check(reader, reader.getOFDDir());
            assertTrue(violations.isEmpty(), "普通 OFD 无 Permissions 应无违规");
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }

    @Test
    void testWithPermissionsDetected() throws Exception {
        Path ofdPath = TestOFDGenerator.createWithPermissionsOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new PermissionRule().check(reader, reader.getOFDDir());
            assertEquals(1, violations.size());
            assertEquals("PERMISSION", violations.get(0).getRuleName());
            assertEquals(ArchiveViolation.Severity.WARN, violations.get(0).getSeverity());
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }
}
