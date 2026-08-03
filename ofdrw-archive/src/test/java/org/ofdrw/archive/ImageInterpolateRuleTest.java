package org.ofdrw.archive;

import org.junit.jupiter.api.Test;
import org.ofdrw.archive.check.rule.ImageInterpolateRule;
import org.ofdrw.archive.check.ArchiveViolation;
import org.ofdrw.reader.OFDReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ImageInterpolateRule 测试（GB/T 42133-2022 6.5b）
 */
class ImageInterpolateRuleTest {

    @Test
    void testNormalOfdNoInterpolateIssue() throws Exception {
        Path ofdPath = TestOFDGenerator.createNormalOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new ImageInterpolateRule().check(reader, reader.getOFDDir());
            // 纯文字 OFD 无图像，无违规
            assertTrue(violations.isEmpty());
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }

    @Test
    void testWithImageOfd() throws Exception {
        Path ofdPath = TestOFDGenerator.createWithImageOfd();
        try (OFDReader reader = new OFDReader(ofdPath)) {
            List<ArchiveViolation> violations = new ImageInterpolateRule().check(reader, reader.getOFDDir());
            // 含图像的 OFD 应能正常检查（不抛异常）
            assertNotNull(violations);
        } finally {
            Files.deleteIfExists(ofdPath);
        }
    }
}
