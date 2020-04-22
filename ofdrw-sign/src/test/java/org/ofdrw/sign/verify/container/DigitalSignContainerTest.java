package org.ofdrw.sign.verify.container;

import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
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
 * 数字电子签章验证
 */
class DigitalSignContainerTest {

    @Test
    void validate() throws IOException, GeneralSecurityException {
//        Path src = Paths.get("src/test/resources", "DigitalSign.ofd");
        Path src = Paths.get("target/DigitalSign", "DigitalSign.ofd");

        Path userP12Path = Paths.get("src/test/resources", "USER.p12");

        Certificate cert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");

        try (OFDReader reader = new OFDReader(src);
             OFDValidator validator = new OFDValidator(reader)) {
            DigitalSignContainer dsc = new DigitalSignContainer(cert);
            validator.setValidator(dsc);
            validator.exeValidate();
            System.out.println(">> 验证通过");
        }
    }
}