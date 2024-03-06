package org.ofdrw.tool.merge;

import org.dom4j.DocumentHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 页面迁移测试
 *
 * @author 权观宇
 * @since 2021-11-10 21:15:31
 */
class OFDMergerTest {

    /**
     * 测试水印迁移
     */
    @Test
    void testMergerWatermark() throws IOException {
        Path dst = Paths.get("target/merge_watermark.ofd");
        Path d1Path = Paths.get("src/test/resources", "hello_watermark.ofd");
        Path d2Path = Paths.get("src/test/resources", "Page5.ofd");

        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.add(d1Path);
            ofdMerger.add(d2Path);
            ofdMerger.add(d1Path);
        }
        System.out.println(dst.toAbsolutePath());
    }


    @Test
    void add() throws IOException {
        Path dst = Paths.get("target/n1.ofd");
        Path d1Path = Paths.get("../ofdrw-converter/src/test/resources/", "y.ofd");
        Path d2Path = Paths.get("../ofdrw-converter/src/test/resources/", "发票示例.ofd");

        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.add(d1Path, 2);
            ofdMerger.add(d2Path, 1);
            ofdMerger.add(d1Path, 1);
        }
    }

    @Test
    void add2() throws IOException {
        Path dst = Paths.get("target/n2.ofd");
        Path d1Path = Paths.get("../ofdrw-converter/src/test/resources/", "y.ofd");
        Path d2Path = Paths.get("../ofdrw-converter/src/test/resources/", "发票示例.ofd");

        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            ofdMerger.add(d1Path, 1, 1);
            ofdMerger.add(d2Path);
            ofdMerger.add(d1Path);
        }
    }
}