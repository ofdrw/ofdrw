package org.ofdrw.gm.ses.v1;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jcajce.provider.keystore.PKCS12;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
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
     * 构造测试用电子印章
     * <p>
     * 注意：该方法只用于生成测试数据，电子印章请使用符合国家规范的电子印章服务器生成管理！
     */
    @Test
    void sealBuild() throws Exception {
        Path userP12 = Paths.get("src/test/resources", "USER.p12");
        Path sealerP12 = Paths.get("src/test/resources", "SealBuilder.p12");
        Path picturePath = Paths.get("src/test/resources", "StampImg.png");
        Path out = Paths.get("target/UserV1.esl");


        SES_Header header = new SES_Header(new ASN1Integer(1), new DERIA5String("OFDR&WTest"));

        /*
         * 印章属性信息构造
         */
        // 获取用户证书
        Certificate userCert = PKCS12Tools.ReadUserCert(userP12, "private", "777777");
        ASN1EncodableVector v = new ASN1EncodableVector(1);
        v.add(new DEROctetString(userCert.getEncoded()));

        Calendar then = Calendar.getInstance();
        then.add(Calendar.YEAR, 2);
        SES_ESPropertyInfo property = new SES_ESPropertyInfo()
                .setType(SES_ESPropertyInfo.OrgType)
                .setName(new DERUTF8String("OFDRW测试用印章"))
                .setCertList(new DERSequence(v))
                .setCreateDate(new ASN1UTCTime(new Date()))
                .setValidStart(new ASN1UTCTime(new Date()))
                .setValidEnd(new ASN1UTCTime(then.getTime()));

        /*
         * 印章图片信息 构造
         */
        SES_ESPictrueInfo picture = new SES_ESPictrueInfo()
                .setType("PNG")
                .setData(Files.readAllBytes(picturePath))
                .setWidth(40)
                .setHeight(40);

        /*
         * 印章信息构造
         */
        SES_SealInfo sealInfo = new SES_SealInfo()
                .setHeader(header)
                .setEsID(UUID.randomUUID().toString().replace("-", "").toUpperCase())
                .setProperty(property)
                .setPicture(picture);
        /*
         * 电子签章数据构造
         */
        Certificate sealerCert = PKCS12Tools.ReadUserCert(sealerP12, "private", "777777");
        PrivateKey privateKey = PKCS12Tools.ReadPrvKey(sealerP12, "private", "777777");
        DEROctetString signCert = new DEROctetString(sealerCert.getEncoded());

        // 印章信息、制章人证书、签名算法标识符组成的信息作为签名原文
        v = new ASN1EncodableVector(3);
        v.add(sealInfo);
        v.add(signCert);
        v.add(GMObjectIdentifiers.sm2sign_with_sm3);

        Signature signature = Signature.getInstance("SM3withSm2", "BC");
        signature.initSign(privateKey);
        signature.update(new DERSequence(v).getEncoded());
        byte[] sign = signature.sign();
        SES_SignInfo signInfo = new SES_SignInfo()
                .setCert(signCert)
                .setSignatureAlgorithm(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignData(sign);

        SESeal seal = new SESeal(sealInfo, signInfo);

        Files.write(out, seal.getEncoded());
    }


    @Test
    public void verify() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SignatureException {
//        Path path = Paths.get("target", "UserV1.esl");
        Path path = Paths.get("target", "2_980_1587284330714.es");

        SESeal seal = SESeal.getInstance(Files.readAllBytes(path));
        SES_SignInfo signInfo = seal.getSignInfo();

        ASN1OctetString cert = signInfo.getCert();
        CertificateFactory factory = new CertificateFactory();
        X509Certificate certificate = (X509Certificate) factory.engineGenerateCertificate(cert.getOctetStream());

        ASN1EncodableVector v = new ASN1EncodableVector(3);
        v.add(seal.getEsealInfo());
        v.add(cert);
        v.add(signInfo.getSignatureAlgorithm());

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initVerify(certificate);
        sg.update(new DERSequence(v).getEncoded());
        byte[] sigVal = signInfo.getSignData().getBytes();
        System.out.println(sg.verify(sigVal));
    }
}