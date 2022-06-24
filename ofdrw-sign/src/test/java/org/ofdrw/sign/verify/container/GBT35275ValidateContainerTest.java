package org.ofdrw.sign.verify.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.verify.OFDValidator;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-08-09 16:50:27
 */
class GBT35275ValidateContainerTest {

    /**
     * 测试符合 GB/T 35275 的签名验签
     */
    @Test
    void validate() throws IOException, GeneralSecurityException {
        Path src = Paths.get("target/GB35275PKCS9DigitalSign.ofd");
        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            validator.setValidator(new GBT35275ValidateContainer());
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }
}