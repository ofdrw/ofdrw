package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

/**
 * SESV4 电子签章验签测试。
 * <p>
 * 被测文件固定为测试资源 {@code src/test/resources/SESV4SignDoc.ofd}，
 * 不依赖其他测试类在 target 目录下生成的临时产物，可单独执行。
 *
 * @author 权观宇
 * @since 2020-04-22 23:10:01
 */
class SESV4ValidateContainerTest {

    @Test
    void validate() throws IOException, GeneralSecurityException {
        Path src = Paths.get("src/test/resources", "SESV4SignDoc.ofd");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new SESV4ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }
}
