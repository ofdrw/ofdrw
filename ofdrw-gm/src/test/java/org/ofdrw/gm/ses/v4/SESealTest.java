package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SESealTest {


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
        sg.update(sesSealInfo.getEncoded());
        byte[] sigVal = sg.sign();

        SESeal seal = new SESeal()
                .seteSealInfo(sesSealInfo)
                .setCert(new DEROctetString(sealerCert.getEncoded()))
                .setSignAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignedValue(sigVal);

        Path out = Paths.get("target/UserV4.esl");
        Files.write(out, seal.getEncoded());
        System.out.println(">> V4版本印章存储于: " + out.toAbsolutePath().toAbsolutePath());
    }

}