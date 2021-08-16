package org.ofdrw.gm.sm2strut.builder;

import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openssl.PEMKeyPair;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.sm2strut.SignedData;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-08-05 19:46:17
 */
class SignedDataBuilderTest {

    /**
     * 测试打包签名值为 GBT 35275 signedData
     */
    @Test
    void signedData() throws Exception {
        Security.addProvider(new BouncyCastleProvider());

        Path certPemFile = Paths.get("src/test/resources", "sign_cert.pem");
        Path keyPemFile = Paths.get("src/test/resources", "sign_key.pem");
        try (final InputStream certOut = Files.newInputStream(certPemFile);
             final InputStream keyOut = Files.newInputStream(keyPemFile);
             final PEMParser certParser = new PEMParser(new InputStreamReader(certOut));
             final PEMParser keyParser = new PEMParser(new InputStreamReader(keyOut))) {

            // 解析证书
            final X509CertificateHolder certificateHolder = (X509CertificateHolder) certParser.readObject();
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

            final SignedData signedData = SignedDataBuilder.signedData(plainText, signatureValue, certificate);
            final byte[] raw = signedData.getEncoded();
            Path out = Paths.get("target/signed_data.dat");
            Files.write(out, raw);
            System.out.println("GB/T 35275 签名值保存位置: " + out.toAbsolutePath());
            System.out.println("\n使用 https://lapo.it/asn1js/ 查看结构\n\n");
            // 使用 https://lapo.it/asn1js/ 查看结构
            System.out.println(Base64.toBase64String(raw));

            SignedData unmarshalled = SignedData.getInstance(raw);
            assertNotNull(unmarshalled);
        }
    }
}