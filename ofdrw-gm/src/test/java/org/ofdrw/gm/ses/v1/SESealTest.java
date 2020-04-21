package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.keystore.PKCS12;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

/**
 * V1 版本电子印章构造
 * <p>
 * 注意：该方法只用于生成测试数据，电子印章请使用符合国家规范的电子印章服务器生成管理！
 *
 * @author 权观宇
 * @since 2020-04-21 10:16:02
 */
public class SESealTest {

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    /**
     * 获取用户证书
     *
     * @param p12 PKCS12存储文件路径
     * @param pwd 解密密钥
     * @return 用户证书
     */
    public static DEROctetString GetUserCert(Path p12, String pwd) throws Exception {
        Certificate userCert = PKCS12Tools.ReadUserCert(p12, "private", pwd);
        return new DEROctetString(userCert.getEncoded());
    }


    /**
     * 构造测试用电子印章
     * <p>
     * 注意：该方法只用于生成测试数据，电子印章请使用符合国家规范的电子印章服务器生成管理！
     */
    @Test
    void sealBuild() throws Exception {
        final Path userP12 = Paths.get("src/test/resources", "USER.p12");

        ASN1Integer version = new ASN1Integer(1);
        DERIA5String vid = new DERIA5String("OFDR&WTest");
        SES_Header header = new SES_Header(version, vid);

        /*
         * 印章属性信息构造
         */
        String esId = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        // 获取用户证书
        ASN1EncodableVector v = new ASN1EncodableVector(1);
        DEROctetString userCert = GetUserCert(userP12, "777777");
        v.add(userCert);
        SES_ESPropertyInfo property = new SES_ESPropertyInfo(
                SES_ESPropertyInfo.OrgType,
                new DERUTF8String("OFDRW测试用印章"),
                new DERSequence(v),
                new ASN1UTCTime(new Date()),
                new ASN1UTCTime(new Date()),
                new ASN1UTCTime(Date.from(LocalDateTime.now()
                        .plusYears(2)
                        .atZone(ZoneId.systemDefault())
                        .toInstant())));

        /*
         * 印章图片信息 构造
         */
        DERIA5String imgType = new DERIA5String("PNG");
        final Path picturePath = Paths.get("src/test/resources", "StampImg.png");
        SES_ESPictrueInfo picture = new SES_ESPictrueInfo(imgType,
                new DEROctetString(Files.readAllBytes(picturePath)),
                new ASN1Integer(40),
                new ASN1Integer(40));

        /*
         * 印章信息构造
         */
        SES_SealInfo sealInfo = new SES_SealInfo(header, new DERIA5String(esId), property, picture, null);

        /*
         * 电子签章数据构造
         */
        SES_SignInfo signInfo;
        Path sealerP12 = Paths.get("src/test/resources", "SealBuilder.p12");
        KeyStore sealerKs = KeyStore.getInstance("PKCS12", "BC");
        char[] pwd = "777777".toCharArray();
        try (InputStream rootKsIn = Files.newInputStream(sealerP12)) {
            sealerKs.load(rootKsIn, pwd);
            Certificate c = sealerKs.getCertificateChain("private")[0];
            DEROctetString signCert = new DEROctetString(c.getEncoded());

            // 印章信息、制章人证书、签名算法标识符组成的信息作为签名原文
            v = new ASN1EncodableVector(3);
            v.add(sealInfo);
            v.add(signCert);
            v.add(GMObjectIdentifiers.sm2sign_with_sm3);

            // 取得印章制作者证书的私钥
            PrivateKey privateKey = (PrivateKey) sealerKs.getKey("private", pwd);
            Signature signature = Signature.getInstance("SM3withSm2", "BC");
            signature.initSign(privateKey);
            signature.update(new DERSequence(v).getEncoded());
            byte[] sign = signature.sign();
            signInfo = new SES_SignInfo(signCert, GMObjectIdentifiers.sm2sign_with_sm3, new DERBitString(sign));
        }

        SESeal seal = new SESeal(sealInfo, signInfo);

        final Path out = Paths.get("target/UserV1.esl");
        Files.write(out, seal.getEncoded());
    }
}