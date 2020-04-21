package org.ofdrw.gm.cert;

import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;

/**
 * 自签名证书生成测试
 *
 * @author 权观宇
 * @since 2020-04-21 09:42:40
 */
class PKCGenerateTest {
    /**
     * 根证书 PKCS12
     */
    public static final Path RootP12Path = Paths.get("src/main/resources", "ROOT.p12");

    /**
     * 生成测试用的用户证书
     */
    @Test
    void generateUseCert() throws GeneralSecurityException, OperatorCreationException, IOException {
        // 生成密钥对
        KeyPair keyPair = PKCGenerate.GenerateKeyPair();
        // 产生证书请求
        PKCS10CertificationRequest p12Req = PKCGenerate.CertRequest(keyPair, PKCGenerate.TestND());

        char[] RootPwd = "123456".toCharArray();
        // 1. 载入P12得到证书和私钥
        KeyStore rootKs = KeyStore.getInstance("PKCS12", "BC");
        Certificate rootCert;
        PrivateKey rootPriKey;
        try (InputStream rootKsIn = Files.newInputStream(RootP12Path)) {
            rootKs.load(rootKsIn, RootPwd);
            // 2. 取得CA根证书
            rootCert = rootKs.getCertificateChain("private")[0];
            // 3. 取得CA根证书的私钥
            rootPriKey = (PrivateKey) rootKs.getKey("private", RootPwd);
        }
        // 生成用户证书
        X509Certificate userCert = PKCGenerate.GenCert(p12Req, rootCert, rootPriKey);

        Path out = Paths.get("target/USER.p12");
        // 保存用户公私钥对和证书到文件
        PKCGenerate.SaveToPKCS12(keyPair, new Certificate[]{userCert, rootCert}, "777777", out);
    }

}