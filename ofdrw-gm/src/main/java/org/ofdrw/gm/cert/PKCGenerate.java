package org.ofdrw.gm.cert;

import org.bouncycastle.asn1.misc.MiscObjectIdentifiers;
import org.bouncycastle.asn1.misc.NetscapeCertType;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.jce.X509KeyUsage;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequest;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

/**
 * 生成测试的公私钥对和证书
 * <p>
 * P12文件
 * <p>
 * PK - Public Key and Private Key Pair
 * <p>
 * C - Certificate
 *
 * @author 权观宇
 * @since 2020-04-21 09:37:10
 */
public class PKCGenerate {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * @return 证书请求识别名称 （也就是证书的Subject）
     */
    public static X500Name TestND() {
        return new X500NameBuilder()
                // 国家代码
                .addRDN(BCStyle.C, "CN")
                // 组织
                .addRDN(BCStyle.O, "OFD R&W")
                // 省份
                .addRDN(BCStyle.ST, "Zhejiang")
                // 地区
                .addRDN(BCStyle.L, "Hangzhou")
                // 通用名称
                .addRDN(BCStyle.CN, "Test Certificate")
                .build();
    }

    /**
     * 生成SM2密钥对的证书请求（pkcs10格式）
     *
     * @param kp      SM2密钥对
     * @param subject 证书使用者
     * @return 证书请求
     * @throws OperatorCreationException  操作异常
     */
    public static PKCS10CertificationRequest CertRequest(KeyPair kp, X500Name subject) throws OperatorCreationException {
        // 构造请求信息，主要是由“实体”的DN和公钥构成
        PKCS10CertificationRequestBuilder requestBuilder =
                new JcaPKCS10CertificationRequestBuilder(subject, kp.getPublic());
        // 使用“实体”私钥对请求的信息进行签名,然后组装成ASN.1对象
        return requestBuilder.build(
                new JcaContentSignerBuilder("SM3withSM2")
                        .setProvider("BC")
                        .build(kp.getPrivate()));

    }


    /**
     * 生成测试SM2密钥对
     *
     * @return 密钥对
     * @throws GeneralSecurityException 安全操作异常
     */
    public static KeyPair GenerateKeyPair() throws GeneralSecurityException {
        // 获取SM2椭圆曲线的参数
        final ECGenParameterSpec sm2Spec = new ECGenParameterSpec("sm2p256v1");
        // 获取一个椭圆曲线类型的密钥对生成器
        final KeyPairGenerator kpg = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider());
        // 使用SM2参数初始化生成器
        kpg.initialize(sm2Spec);

        // 使用SM2的算法区域初始化密钥生成器
        kpg.initialize(sm2Spec, new SecureRandom());
        // 获取密钥对
        return kpg.generateKeyPair();
    }


    /**
     * 签发证书
     *
     * @param p10Obj     证书请求ASN1对象
     * @param root       CA根证书
     * @param privateKey CA私钥
     * @return X509证书对象
     * @throws GeneralSecurityException 安全操作异常
     * @throws IOException 文件读写异常
     * @throws OperatorCreationException 操作异常
     */
    public static X509Certificate GenCert(PKCS10CertificationRequest p10Obj,
                                          Certificate root,
                                          PrivateKey privateKey)
            throws GeneralSecurityException,
            IOException,
            OperatorCreationException {
        JcaPKCS10CertificationRequest req = new JcaPKCS10CertificationRequest(p10Obj);
        // 从证书请求中获取到使用DN
        X500Name subject = req.getSubject();

        // 取得根证书的Subject，签发证书的使用者就是根证书的使用者
        X500Name issuer = new X509CertificateHolder(root.getEncoded())
                .getSubject();

        // 根据需求构造实体证书
        X509v3CertificateBuilder certGen = new JcaX509v3CertificateBuilder(
                // 颁发者信息
                issuer
                // 证书序列号
                , BigInteger.valueOf(Instant.now().toEpochMilli())
                // 证书生效日期
                , Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant())
                // 证书失效日期
                , Date.from(LocalDateTime.now().plusYears(2).atZone(ZoneId.systemDefault()).toInstant())
                // 使用者信息
                , subject
                // 证书公钥
                , req.getPublicKey())
                // 设置密钥用法
                .addExtension(Extension.keyUsage,
                        false
                        , new X509KeyUsage(X509KeyUsage.digitalSignature
                                | X509KeyUsage.nonRepudiation
                                | X509KeyUsage.keyCertSign))
                // 设置扩展密钥用法：客户端身份认证
                .addExtension(Extension.extendedKeyUsage,
                        false,
                        new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth))
                // 基础约束,标识是否是CA证书，这里false标识为实体证书
                .addExtension(Extension.basicConstraints,
                        false,
                        new BasicConstraints(false))
                // Netscape Cert Type SSL客户端身份认证
                .addExtension(MiscObjectIdentifiers.netscapeCertType,
                        false,
                        new NetscapeCertType(NetscapeCertType.sslClient));

        // 5. 证书签名实现类
        ContentSigner sigGen = new JcaContentSignerBuilder("SM3withSM2")
                .setProvider("BC")
                .build(privateKey);

        // 6. 签发证书
        return new JcaX509CertificateConverter()
                .setProvider("BC")
                .getCertificate(certGen.build(sigGen));
    }


    /**
     * 生成P12存储文件
     * <p>
     * 存储公私钥对以及证书
     *
     * @param pk    密钥对
     * @param certs 证书链
     * @param pwd   P12密码
     * @param outP  保存位置
     * @throws GeneralSecurityException 安全异常
     * @throws IOException IO异常
     */
    public static void SaveToPKCS12(KeyPair pk,
                                    Certificate[] certs,
                                    String pwd,
                                    Path outP)
            throws GeneralSecurityException, IOException {
        // 3. 以KeyStore保存
        KeyStore store = KeyStore.getInstance("PKCS12", "BC");
        // 3.1 初始化
        store.load(null, null);
        char[] pwds = pwd.toCharArray();
        // 3.2 写入证书以及公钥
        store.setKeyEntry("private", pk.getPrivate(), pwds, certs);
        try (OutputStream out = Files.newOutputStream(outP);) {
            // 3.3 加密写入文件
            store.store(out, pwds);
        }
    }

}
