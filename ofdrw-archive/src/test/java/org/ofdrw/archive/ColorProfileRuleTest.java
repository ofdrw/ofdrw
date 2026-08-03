package org.ofdrw.archive;

import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.rule.ColorProfileRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ColorProfileRule 测试（GB/T 42133-2022 6.3.1c）
 */
class ColorProfileRuleTest {

    @Test
    void testNormalOfdColorProfileCheck() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new ColorProfileRule().check(reader, reader.getOFDDir());
            // INFO 级别提示不阻塞，至少不抛异常
            assertNotNull(violations);
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }
}
