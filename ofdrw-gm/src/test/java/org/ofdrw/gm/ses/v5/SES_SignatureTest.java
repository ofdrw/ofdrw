package org.ofdrw.gm.ses.v5;

import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;

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

class SES_SignatureTest {

    /**
     * 构建一个V5印章用于签章测试
     */
    private SESeal buildV5Seal() throws GeneralSecurityException, IOException {
        Path sealerPath = Paths.get("src/test/resources", "SealBuilder.p12");
        Path userPath = Paths.get("src/test/resources", "USER.p12");
        Path picturePath = Paths.get("src/test/resources", "StampImg.png");

        Certificate sealerCert = PKCS12Tools.ReadUserCert(sealerPath, "private", "777777");
        Certificate userCert = PKCS12Tools.ReadUserCert(userPath, "private", "777777");

        ASN1EncodableVector cv = new ASN1EncodableVector(1);
        cv.add(new DEROctetString(userCert.getEncoded()));

        SES_Header header = new SES_Header(SES_Header.V5, new DERIA5String("OFDR&WTestV5"));
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.YEAR, 2);

        SES_ESPropertyInfo propertyInfo = new SES_ESPropertyInfo()
                .setType(new ASN1Integer(3))
                .setName(new DERUTF8String("OFD R&W V5测试用印章"))
                .setCertListType(SES_ESPropertyInfo.CertListType)
                .setCertList(SES_CertList.getInstance(SES_ESPropertyInfo.CertListType, new DERSequence(cv)))
                .setCreateDate(new ASN1GeneralizedTime(new Date()))
                .setValidStart(new ASN1GeneralizedTime(new Date()))
                .setValidEnd(new ASN1GeneralizedTime(cal.getTime()));

        SES_ESPictrueInfo pictrueInfo = new SES_ESPictrueInfo()
                .setType("PNG")
                .setData(Files.readAllBytes(picturePath))
                .setWidth(40)
                .setHeight(40);

        SES_SealInfo sealInfo = new SES_SealInfo()
                .setHeader(header)
                .setEsID(new DERIA5String(UUID.randomUUID().toString().replace("-", "")))
                .setProperty(propertyInfo)
                .setPicture(pictrueInfo);

        PrivateKey sealerPrvKey = PKCS12Tools.ReadPrvKey(sealerPath, "private", "777777");
        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(sealerPrvKey);
        sg.update(sealInfo.getEncoded("DER"));
        byte[] sigVal = sg.sign();

        return new SESeal()
                .seteSealInfo(sealInfo)
                .setCert(sealerCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignatureValue(sigVal);
    }

    /**
     * 构建V5签章数据的辅助方法
     */
    private SES_Signature buildV5Signature(boolean withTimeStamp) throws GeneralSecurityException, IOException {
        Path userP12 = Paths.get("src/test/resources", "USER.p12");
        SESeal seal = buildV5Seal();

        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V5)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date()))
                .setDataHash(new byte[32])
                .setPropertyInfo("/Doc_0/Signs/Sign_0/Signature.xml");

        Certificate useCert = PKCS12Tools.ReadUserCert(userP12, "private", "777777");
        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12, "private", "777777");

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(prvKey);
        sg.update(toSign.getEncoded("DER"));
        byte[] sigVal = sg.sign();

        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(useCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignatureValue(sigVal);

        if (withTimeStamp) {
            signature.setTimeStamp(new DERBitString(new byte[]{0x0A, 0x0B, 0x0C}));
        }
        return signature;
    }

    /**
     * V5签章构建-编码-解码往返测试（不带timeStamp）
     */
    @Test
    void buildEncodeDecodeRoundTrip() throws GeneralSecurityException, IOException {
        SES_Signature signature = buildV5Signature(false);
        byte[] encoded = signature.getEncoded("DER");
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        // 解码
        SES_Signature decoded = SES_Signature.getInstance(encoded);
        assertNotNull(decoded);

        // 字段断言
        TBS_Sign toSign = decoded.getToSign();
        assertNotNull(toSign);
        assertEquals(5, toSign.getVersion().getValue().intValue());
        assertEquals(
                "/Doc_0/Signs/Sign_0/Signature.xml",
                toSign.getPropertyInfo().getString()
        );
        assertArrayEquals(new byte[32], toSign.getDataHash().getOctets());

        assertEquals(
                GMObjectIdentifiers.sm2sign_with_sm3.getId(),
                decoded.getSignatureAlgID().getId()
        );
        assertArrayEquals(
                signature.getCert().getOctets(),
                decoded.getCert().getOctets()
        );
        assertArrayEquals(
                signature.getSignatureValue().getOctets(),
                decoded.getSignatureValue().getOctets()
        );
        assertNull(decoded.getTimeStamp());

        Path out = Paths.get("target/SignedValueV5.dat");
        Files.write(out, encoded);
        System.out.println(">> V5签章数据(无timeStamp)存储于: " + out.toAbsolutePath());
    }

    /**
     * V5签章timeStamp可选字段测试 - 带timeStamp
     */
    @Test
    void buildEncodeDecodeWithTimeStamp() throws GeneralSecurityException, IOException {
        SES_Signature signature = buildV5Signature(true);
        byte[] encoded = signature.getEncoded("DER");

        SES_Signature decoded = SES_Signature.getInstance(encoded);
        assertNotNull(decoded);
        assertNotNull(decoded.getTimeStamp());
        assertArrayEquals(
                new byte[]{0x0A, 0x0B, 0x0C},
                decoded.getTimeStamp().getOctets()
        );

        // 其他字段也正确
        assertEquals(5, decoded.getToSign().getVersion().getValue().intValue());
        assertEquals(
                GMObjectIdentifiers.sm2sign_with_sm3.getId(),
                decoded.getSignatureAlgID().getId()
        );

        Path out = Paths.get("target/SignedValueV5_ts.dat");
        Files.write(out, encoded);
        System.out.println(">> V5签章数据(带timeStamp)存储于: " + out.toAbsolutePath());
    }

    /**
     * V5签章timeStamp可选字段测试 - 不带timeStamp
     */
    @Test
    void buildEncodeDecodeWithoutTimeStamp() throws GeneralSecurityException, IOException {
        SES_Signature signature = buildV5Signature(false);
        byte[] encoded = signature.getEncoded("DER");

        SES_Signature decoded = SES_Signature.getInstance(encoded);
        assertNotNull(decoded);
        assertNull(decoded.getTimeStamp());

        // 确认序列长度为4（无可选字段）
        ASN1Sequence seq = ASN1Sequence.getInstance(encoded);
        assertEquals(4, seq.size());
    }
}
