package org.ofdrw.sign.signContainer;

import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PEMLoader;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.OFDSigner;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * @author 权观宇
 * @since 2021-08-09 16:08:42
 */
class GBT35275DSContainerTest {

    /**
     * 测试符合 GB/T 35275 的签名
     */
    @Test
    void sign() throws GeneralSecurityException, IOException {
        Security.addProvider(new BouncyCastleProvider());

        Path certPemFile = Paths.get("src/test/resources", "sign_cert.pem");
        Path keyPemFile = Paths.get("src/test/resources", "sign_key.pem");
        final PrivateKey privateKey = PEMLoader.loadPrivateKey(keyPemFile);
        final Certificate certificate = PEMLoader.loadCert(certPemFile);

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/GB35275DigitalSign.ofd");
        // 1. 构造签名引擎
        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out)) {
            GBT35275DSContainer signContainer = new GBT35275DSContainer(certificate, privateKey);
//            // 该参数用于兼容部分阅读只支持对Hash结果的Base64签名
//            signContainer.setEnableFileHashBase64(true);
            // 3. 设置签名使用的扩展签名容器
            signer.setSignContainer(signContainer);
            // 4. 执行签名
            signer.exeSign();
            // 5. 关闭签名引擎，生成文档。
        }
        System.out.println(">> 生成文件位置: " + out.toAbsolutePath().toAbsolutePath());
    }

}