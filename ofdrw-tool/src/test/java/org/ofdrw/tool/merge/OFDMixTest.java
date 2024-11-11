package org.ofdrw.tool.merge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

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
        Path p1P = Paths.get("src/test/resources", "Page1.ofd");
        Path p2P = Paths.get("src/test/resources", "Page2.ofd");
        // 1. 创建一个新的OFD文档合并对象
        try (OFDMerger ofdMerger = new OFDMerger(dst)) {
            // 2. 合并两页
            ofdMerger.addMix(p1P, 1, p2P, 1);
            // 3. 关闭OFD文档合并对象
        }
        System.out.println(dst.toAbsolutePath());
    }

    /**
     * 测试添加和混合
     */
    @Test
    public void testMixMilPage() throws Exception {
        Path dst = Paths.get("target/mixed_multi_page.ofd");

        Path p1P = Paths.get("src/test/resources", "Page1.ofd");
        Path p2P = Paths.get("src/test/resources", "Page2.ofd");
        Path p3P = Paths.get("src/test/resources", "Page3.ofd");

        try (OFDMerger ofdMerger = new OFDMerger(dst)) {

            // 混合多个页面为一个页面
            ArrayList<DocPage> boBeMixPages = new ArrayList<>();
            boBeMixPages.add(new DocPage(p1P, 1));
            boBeMixPages.add(new DocPage(p2P, 1));
            boBeMixPages.add(new DocPage(p3P, 1));

            // 混合并加入到新文档中
            ofdMerger.addMix(boBeMixPages);
        }
        System.out.println(dst.toAbsolutePath());
    }
}
