package org.ofdrw.tool.merge;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;


/**
 * 文档页面删除测试
 */
class OFDPageDeleterTest {

    /**
     * 文档页面删除示例
     */
    @Test
    void delete() throws Exception {
        Path srcP = Paths.get("src/test/resources", "Page5.ofd");
        Path outP = Paths.get("target/page_deleted.ofd");
        try (OFDPageDeleter deleter = new OFDPageDeleter(srcP, outP)) {
            deleter.delete(0, 1, 2);
        }
        System.out.println(">> 生成文档位置：" + outP.toAbsolutePath());
    }
}