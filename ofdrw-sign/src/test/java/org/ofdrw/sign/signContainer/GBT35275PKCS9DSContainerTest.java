package org.ofdrw.sign.signContainer;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PEMLoader;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.ExtendSignatureContainer;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.OFDSigner;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试数科兼容的PKCS#7 CMS 签名格式
 *
 * @author 权观宇
 * @since 2022-06-24 22:40:45
 */
class GBT35275PKCS9DSContainerTest {
    /**
     * 测试数科兼容的数字签名
     */
    @Test
    void sign() throws GeneralSecurityException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        Path certPemFile = Paths.get("src/test/resources", "sign_cert.pem");
        Path keyPemFile = Paths.get("src/test/resources", "sign_key.pem");
        final PrivateKey privateKey = PEMLoader.loadPrivateKey(keyPemFile);
        final Certificate certificate = PEMLoader.loadCert(certPemFile);

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/GB35275PKCS9DigitalSign.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out,  new NumberFormatAtomicSignID())) {
            ExtendSignatureContainer signContainer = new GBT35275PKCS9DSContainer(certificate, privateKey);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 执行签名
            signer.exeSign();
            // 5. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }
}