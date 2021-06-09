package org.ofdrw.sign;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试清空电子签名
 *
 * @author 权观宇
 * @since 2021-05-24 20:38:21
 */
class SignCleanerTest {

    /**
     * 清空电子签名
     */
    @Test
    void clean() throws IOException, DocumentException {
        Path src = Paths.get("src/test/resources", "v4signed.ofd");
        Path out = Paths.get("target", "remove_sign.ofd");
        // 1. 创建 OFD解析器
        try (OFDReader reader = new OFDReader(src)) {
            // 2. 构造签名清理工具
            SignCleaner sCleaner = new SignCleaner(reader, out);
            // 3. 清空所有电子签名
            sCleaner.clean();
        }
        // 4. 关闭 Reader，这里使用 try finally 语法自动 Close Reader
        System.out.println("生成文档位置: " + out.toAbsolutePath());

    }
}