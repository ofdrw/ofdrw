package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

/**
 * SESV5 电子签章验签测试。
 * <p>
 * 被测文件固定为测试资源 {@code src/test/resources/SESV5SignDoc.ofd}，
 * 不依赖其他测试类在 target 目录下生成的临时产物，可单独执行。
 */
class SESV5ValidateContainerTest {

    @Test
    void validate() throws IOException, GeneralSecurityException {
        Path src = Paths.get("src/test/resources", "SESV5SignDoc.ofd");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV5ValidateContainer());
            validator.exeValidate();
            System.out.println(">> V5验签通过");
        }
        // 如果没抛异常就是验签通过
    }
}
