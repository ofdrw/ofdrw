package org.ofdrw.archive;

import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.rule.ExtensionRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ExtensionRule 测试（GB/T 42133-2022 6.2.2e）
 */
class ExtensionRuleTest {

    @Test
    void testNormalOfdNoExtension() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new ExtensionRule().check(reader, reader.getOFDDir());
            assertTrue(violations.isEmpty(), "普通 OFD 无 Extensions 应无违规");
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }

    @Test
    void testWithExtensionsDetected() throws Exception {
        Path ofdPath = TestOFDGenerator.createWithExtensionsOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new ExtensionRule().check(reader, reader.getOFDDir());
            assertTrue(violations.size() >= 1, "含 Extensions 的 OFD 应被检测到");
            assertEquals("EXTENSION", violations.get(0).getRuleName());
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }
}
