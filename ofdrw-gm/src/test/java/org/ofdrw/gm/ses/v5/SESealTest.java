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

class SESealTest {

    /**
     * 构建V5印章的辅助方法
     */
    private SESeal buildV5Seal(boolean withTimeStamp) throws GeneralSecurityException, IOException {
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

        SESeal seal = new SESeal()
                .seteSealInfo(sealInfo)
                .setCert(sealerCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignatureValue(sigVal);

        if (withTimeStamp) {
            seal.setTimeStamp(new DERBitString(new byte[]{0x01, 0x02, 0x03}));
        }
        return seal;
    }

    /**
     * V5印章构建-编码-解码往返测试（不带timeStamp）
     */
    @Test
    void buildEncodeDecodeRoundTrip() throws GeneralSecurityException, IOException {
        SESeal seal = buildV5Seal(false);
        byte[] encoded = seal.getEncoded("DER");
        assertNotNull(encoded);
        assertTrue(encoded.length > 0);

        // 解码
        SESeal decoded = SESeal.getInstance(encoded);
        assertNotNull(decoded);

        // 字段断言
        assertEquals(
                seal.geteSealInfo().getHeader().getVersion().getValue(),
                decoded.geteSealInfo().getHeader().getVersion().getValue()
        );
        assertEquals(
                seal.geteSealInfo().getEsID().getString(),
                decoded.geteSealInfo().getEsID().getString()
        );
        assertEquals(
                seal.getSignatureAlgID().getId(),
                decoded.getSignatureAlgID().getId()
        );
        assertArrayEquals(
                seal.getCert().getOctets(),
                decoded.getCert().getOctets()
        );
        assertArrayEquals(
                seal.getSignatureValue().getOctets(),
                decoded.getSignatureValue().getOctets()
        );
        // 无timeStamp
        assertNull(decoded.getTimeStamp());

        // 写出文件供后续测试使用
        Path out = Paths.get("target/UserV5.esl");
        Files.write(out, encoded);
        System.out.println(">> V5印章(无timeStamp)存储于: " + out.toAbsolutePath());
    }

    /**
     * V5印章timeStamp可选字段测试 - 带timeStamp
     */
    @Test
    void buildEncodeDecodeWithTimeStamp() throws GeneralSecurityException, IOException {
        SESeal seal = buildV5Seal(true);
        byte[] encoded = seal.getEncoded("DER");

        SESeal decoded = SESeal.getInstance(encoded);
        assertNotNull(decoded);
        assertNotNull(decoded.getTimeStamp());
        assertArrayEquals(
                new byte[]{0x01, 0x02, 0x03},
                decoded.getTimeStamp().getOctets()
        );

        // 其他字段也要正确
        assertEquals(
                seal.geteSealInfo().getHeader().getVersion().getValue(),
                decoded.geteSealInfo().getHeader().getVersion().getValue()
        );
        assertEquals(
                seal.getSignatureAlgID().getId(),
                decoded.getSignatureAlgID().getId()
        );

        Path out = Paths.get("target/UserV5_ts.esl");
        Files.write(out, encoded);
        System.out.println(">> V5印章(带timeStamp)存储于: " + out.toAbsolutePath());
    }

    /**
     * V5印章timeStamp可选字段测试 - 不带timeStamp
     */
    @Test
    void buildEncodeDecodeWithoutTimeStamp() throws GeneralSecurityException, IOException {
        SESeal seal = buildV5Seal(false);
        byte[] encoded = seal.getEncoded("DER");

        SESeal decoded = SESeal.getInstance(encoded);
        assertNotNull(decoded);
        assertNull(decoded.getTimeStamp());

        // 确认序列长度为4（无可选字段）
        ASN1Sequence seq = ASN1Sequence.getInstance(encoded);
        assertEquals(4, seq.size());
    }
}
