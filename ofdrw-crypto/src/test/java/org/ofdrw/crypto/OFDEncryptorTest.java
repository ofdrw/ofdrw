package org.ofdrw.crypto;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.crypto.encryt.UserInfo;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-15 18:20:18
 */
class OFDEncryptorTest {

    @Test
    void encrypt() throws IOException, GeneralSecurityException {
        Path src = Paths.get("src/test/resources/hello.ofd");
        Path out = Paths.get("target/hello-enc.ofd");
        try (OFDEncryptor ofdEncryptor = new OFDEncryptor(src, out)) {
            ofdEncryptor.addUser(new UserFEKEncryptor() {
                @Override
                public UserInfo encrypt(@NotNull String username, @Nullable String userType, @NotNull String fek) throws GeneralSecurityException, IOException {
                    return null;
                }

                @Override
                public byte[] userCert() {
                    return new byte[0];
                }

                @Override
                public @NotNull String encryptCaseId() {
                    return null;
                }
            });
            ofdEncryptor.setContainerFileFilter((p, pp) -> {
                final Random rd = new Random();
                System.out.println(p);
                return rd.nextBoolean();
            });
            ofdEncryptor.encrypt();
        }
    }
}