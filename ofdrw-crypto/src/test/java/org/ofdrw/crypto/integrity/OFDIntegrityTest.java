package org.ofdrw.crypto.integrity;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.crypto.OFDEncryptor;
import org.ofdrw.crypto.enryptor.UserFEKEncryptor;
import org.ofdrw.crypto.enryptor.UserPasswordEncryptor;
import org.ofdrw.gm.cert.PEMLoader;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * OFD完整性保护协议
 *
 * @author 权观宇
 * @since 2021-08-17 20:09:59
 */
class OFDIntegrityTest {

    /**
     * 完整性保护示例
     */
    @Test
    void protect() throws IOException, GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());

        Path src = Paths.get("src/test/resources/hello.ofd");
        Path out = Paths.get("target/hello-integrity.ofd");

        Path certPemFile = Paths.get("src/test/resources", "sign_cert.pem");
        Path keyPemFile = Paths.get("src/test/resources", "sign_key.pem");

        // 1. 准备 签名私钥 和 签名证书
        final PrivateKey privateKey = PEMLoader.loadPrivateKey(keyPemFile);
        final Certificate certificate = PEMLoader.loadCert(certPemFile);

        // 2. 创建完整性保护对象
        try (OFDIntegrity ofdIntegrity = new OFDIntegrity(src, out)) {
            // 3. 创建签名实现容器
            GMProtectSigner gmSigner = new GMProtectSigner(privateKey, certificate);
            // 4. 执行完整性保护
            ofdIntegrity.protect(gmSigner);
        }
        // 5. 关闭对象，清除临时文件 try()
        System.out.println(">> " + out.toAbsolutePath());
    }
}