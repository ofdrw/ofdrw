package org.ofdrw.sign.verify.container;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.Security;

/**
 * 符合 GB/T 35275 的签名验签测试。
 * <p>
 * 被测文件固定为测试资源 {@code src/test/resources/GB35275PKCS9DigitalSign.ofd}，
 * 不依赖其他测试类在 target 目录下生成的临时产物，可单独执行。
 *
 * @author 权观宇
 * @since 2021-08-09 16:50:27
 */
class GBT35275ValidateContainerTest {

    /**
     * 测试符合 GB/T 35275 的签名验签
     */
    @Test
    void validate() throws IOException, GeneralSecurityException {
        // 样本使用 SM3 摘要算法，验签需要 BouncyCastle 提供算法实现
        Security.addProvider(new BouncyCastleProvider());

        Path src = Paths.get("src/test/resources", "GB35275PKCS9DigitalSign.ofd");
        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new GBT35275ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }
}
