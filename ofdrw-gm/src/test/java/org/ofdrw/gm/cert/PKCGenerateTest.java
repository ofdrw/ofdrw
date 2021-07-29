package org.ofdrw.gm.cert;

import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
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

        Path out = Paths.get("target/SealBuilder.p12");
        // 保存用户公私钥对和证书到文件
        PKCGenerate.SaveToPKCS12(keyPair, new Certificate[]{userCert}, "777777", out);
    }


    /**
     * 生成测试用的用户证书
     */
    @Test
    void generateUseCertPEM() throws GeneralSecurityException, OperatorCreationException, IOException {
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

        Path certPemFile = Paths.get("target/sign_cert.pem");
        Path keyPemFile = Paths.get("target/sign_key.pem");
        try (final OutputStream certOut = Files.newOutputStream(certPemFile);
             final OutputStream keyOut = Files.newOutputStream(keyPemFile);
             final JcaPEMWriter certWriter = new JcaPEMWriter(new OutputStreamWriter(certOut));
             final JcaPEMWriter keyWriter = new JcaPEMWriter(new OutputStreamWriter(keyOut));) {

            certWriter.writeObject(userCert);
            keyWriter.writeObject(keyPair.getPrivate());
        }
    }

    /**
     * 解析证书和私钥
     */
    @Test
    public void testLoadPEM() throws IOException, GeneralSecurityException {
        Security.addProvider(new BouncyCastleProvider());
        Path certPemFile = Paths.get("src/test/resources", "sign_cert.pem");
        Path keyPemFile = Paths.get("src/test/resources", "sign_key.pem");
        try (final InputStream certOut = Files.newInputStream(certPemFile);
             final InputStream keyOut = Files.newInputStream(keyPemFile);
             final PEMParser certParser = new PEMParser(new InputStreamReader(certOut));
             final PEMParser keyParser = new PEMParser(new InputStreamReader(keyOut))) {

            // 解析证书
            final X509CertificateHolder certificateHolder = (X509CertificateHolder)certParser.readObject();
            final X509Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateHolder);
            // 解析私钥
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter().setProvider("BC");
            final PEMKeyPair pemKeyPair = (PEMKeyPair) keyParser.readObject();
            final PrivateKey privateKey = converter.getPrivateKey(pemKeyPair.getPrivateKeyInfo());

            // 通过签名验签验证
            Signature signature = Signature.getInstance(GMObjectIdentifiers.sm2sign_with_sm3.toString(), "BC");
            signature.initSign(privateKey);
            byte[] plainText = "Hello world".getBytes(StandardCharsets.UTF_8);
            signature.update(plainText);
            byte[] signatureValue = signature.sign();

            signature.initVerify(certificate);
            signature.update(plainText);
            final boolean result = signature.verify(signatureValue);
            Assertions.assertTrue(result);
        }
    }

}