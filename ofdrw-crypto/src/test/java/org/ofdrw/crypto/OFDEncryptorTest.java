package org.ofdrw.crypto;

import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.engines.SM4Engine;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PKCS7Padding;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.crypto.encryt.UserInfo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-15 18:20:18
 */
class OFDEncryptorTest {

    @Test
    public void ofCase() throws Exception {
        SecureRandom random = new SecureRandom();
        PaddedBufferedBlockCipher blockCipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(new SM4Engine()), new PKCS7Padding());
        final int blockSize = blockCipher.getBlockSize();
        byte[] fek = new byte[blockSize];
        byte[] iv = new byte[blockSize];
        random.nextBytes(fek);
        random.nextBytes(iv);
        ParametersWithIV keyParam = new ParametersWithIV(new KeyParameter(fek), iv);
        blockCipher.init(true,keyParam);

        Path src = Paths.get("src/test/resources/byte16.bin");
        Path dst = Paths.get("target/byte16.enc");

        byte[] buffIn = new byte[1024];
        byte[] buffOut = new byte[1024 + blockSize];

        int len = 0;
        try(final InputStream in = Files.newInputStream(src);
            final OutputStream out = Files.newOutputStream(dst)){
            int bytesProcessed = 0;
            while ((len = in.read(buffIn)) != -1) {
                bytesProcessed = blockCipher.processBytes(buffIn, 0, len, buffOut, 0);
                if (bytesProcessed > 0){
                    out.write(buffOut, 0, bytesProcessed);
                }
            }
            bytesProcessed = blockCipher.doFinal(buffOut, 0);
            out.write(buffOut, 0, bytesProcessed);
            blockCipher.reset();
        }
    }

    @Test
    void encrypt() throws IOException, CryptoException {
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
//                final Random rd = new Random();
//                System.out.println(p);
                return true;
            });
            ofdEncryptor.encrypt();
        }
    }
}