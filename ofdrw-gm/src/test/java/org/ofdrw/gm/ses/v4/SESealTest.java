package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SESealTest {

    @Test
    void signVerify() throws GeneralSecurityException, IOException {
        Path sealerPath = Paths.get("src/test/resources", "SealBuilder.p12");

        PrivateKey sealerPrvKey = PKCS12Tools.ReadPrvKey(sealerPath, "private", "777777");
        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(sealerPrvKey);
        sg.update(new byte[32]);
        byte[] sigVal = sg.sign();
        System.out.println(sigVal.length);

        Certificate certificate = PKCS12Tools.ReadUserCert(sealerPath, "private", "777777");

        sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initVerify(certificate);
        sg.update(new byte[32]);
        System.out.println(sg.verify(sigVal));
    }

    @Test
    public void build() throws GeneralSecurityException, IOException {
        Path sealerPath = Paths.get("src/test/resources", "SealBuilder.p12");
        Path userPath = Paths.get("src/test/resources", "USER.p12");
        Path picturePath = Paths.get("src/test/resources", "StampImg.png");

        Certificate sealerCert = PKCS12Tools.ReadUserCert(sealerPath, "private", "777777");
        Certificate userCert = PKCS12Tools.ReadUserCert(userPath, "private", "777777");

        ASN1EncodableVector v = new ASN1EncodableVector(1);
        v.add(new DEROctetString(userCert.getEncoded()));

        SES_Header header = new SES_Header(SES_Header.V4, new DERIA5String("OFDR&WTest"));
        Calendar now = Calendar.getInstance();
        now.add(Calendar.YEAR, 2);
        Date then = now.getTime();
        SES_ESPropertyInfo propertyInfo = new SES_ESPropertyInfo()
                .setType(new ASN1Integer(3))
                .setName(new DERUTF8String("OFD R&W 测试用印章"))
                .setCertListType(SES_ESPropertyInfo.CertListType)
                .setCertList(SES_CertList.getInstance(SES_ESPropertyInfo.CertListType, new DERSequence(v)))
                .setCreateDate(new ASN1GeneralizedTime(new Date()))
                .setValidStart(new ASN1GeneralizedTime(new Date()))
                .setValidEnd(new ASN1GeneralizedTime(then));

        SES_ESPictrueInfo pictrueInfo = new SES_ESPictrueInfo()
                .setType("PNG")
                .setData(Files.readAllBytes(picturePath))
                .setWidth(40)
                .setHeight(40);

        SES_SealInfo sesSealInfo = new SES_SealInfo()
                .setHeader(header)
                .setEsID(new DERIA5String(UUID.randomUUID().toString().replace("-", "")))
                .setProperty(propertyInfo)
                .setPicture(pictrueInfo);

        PrivateKey sealerPrvKey = PKCS12Tools.ReadPrvKey(sealerPath, "private", "777777");
        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(sealerPrvKey);
        sg.update(sesSealInfo.getEncoded("DER"));
        byte[] sigVal = sg.sign();

        SESeal seal = new SESeal()
                .seteSealInfo(sesSealInfo)
                .setCert(sealerCert)
                .setSignAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignedValue(sigVal);

        Path out = Paths.get("target/UserV4.esl");
        Files.write(out, seal.getEncoded("DER"));
        System.out.println(">> V4版本印章存储于: " + out.toAbsolutePath().toAbsolutePath());

    }


    @Test
    public void verify() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SignatureException {
        Path path = Paths.get("src/test/resources", "UserV4.esl");
//        Path path = Paths.get("target", "UserV4.esl");
//        Path path = Paths.get("src/test/resources", "Seal.esl");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(path));

        ASN1OctetString cert = seal.getCert();
        CertificateFactory factory = new CertificateFactory();
        X509Certificate certificate = (X509Certificate) factory.engineGenerateCertificate(cert.getOctetStream());

        SES_SealInfo ses_sealInfo = seal.geteSealInfo();

        Signature sg = Signature.getInstance(seal.getSignAlgID().toString()
                , new BouncyCastleProvider());
        sg.initVerify(certificate);
        sg.update(ses_sealInfo.getEncoded("DER"));
        byte[] sigVal = seal.getSignedValue().getBytes();

        System.out.println(sg.verify(sigVal));
    }

}