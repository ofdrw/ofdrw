package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * 数字电子签章验证
 */
class DigitalValidateContainerTest {

    @Test
    void validate() throws IOException, GeneralSecurityException {
        // 读取 DigitalSignContainerTest 生成在 target 目录的签名文件
        Path src = Paths.get("target", "DigitalSign.ofd");

        Path userP12Path = Paths.get("src/test/resources", "USER.p12");

        Certificate cert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            DigitalValidateContainer dsc = new DigitalValidateContainer(cert);
            validator.setValidator(dsc);
            assertDoesNotThrow(() -> validator.exeValidate(), "数字签名验证应通过");
        }
    }
}