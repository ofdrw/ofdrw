package org.ofdrw.tool.merge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * OFD页面混合测试
 *
 * @author 权观宇
 * @since 2024-11-7 22:55:51
 */
public class OFDMixTest {

    /**
     * 测试添加和混合
     */
    @Test
    public void testAddMix() throws Exception {
        Path dst = Paths.get("target/mixed.ofd");
        Path d1Path = Paths.get("src/test/resources", "hello_watermark.ofd");
        Path d2Path = Paths.get("src/test/resources", "Page5.ofd");

        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.addMix(d2Path, 5, d1Path, 1);
        }
        System.out.println(dst.toAbsolutePath());
    }
}
