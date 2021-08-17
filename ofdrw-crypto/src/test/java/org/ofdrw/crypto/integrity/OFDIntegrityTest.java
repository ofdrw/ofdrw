package org.ofdrw.crypto.integrity;

import org.junit.jupiter.api.Test;
import org.ofdrw.crypto.OFDEncryptor;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.crypto.enryptor.UserPasswordEncryptor;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-08-17 20:09:59
 */
class OFDIntegrityTest {

    @Test
    void protect() throws IOException, GeneralSecurityException {
        Path src = Paths.get("src/test/resources/hello.ofd");
        Path out = Paths.get("target/hello-integrity.ofd");
        try (OFDIntegrity ofdIntegrity = new OFDIntegrity(src, out)) {
            ofdIntegrity.protect(tbs -> new byte[16]);

//            final UserFEKEncryptor encryptor = new UserPasswordEncryptor("777", "12345678");
//            ofdEncryptor.addUser(encryptor);
//            ofdEncryptor.encrypt();
        }
        System.out.println(">> " + out.toAbsolutePath().toString());
    }
}