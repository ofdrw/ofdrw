package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.ASN1Boolean;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.ses.v1.ExtData;
import org.ofdrw.gm.ses.v1.ExtensionDatas;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-10-11 20:31:05
 */
class TBS_SignTest {

    @Test
    void toASN1Primitive() throws IOException {
        Path userSealPath = Paths.get("src/test/resources", "UserV4.esl");

        SESeal seal = SESeal.getInstance(Files.readAllBytes(userSealPath));
        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V4)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date()))
                .setDataHash(new byte[32])
                .setPropertyInfo("/Doc_0/Signs/Sign_0/Signature.xml");
        ExtensionDatas eds = new ExtensionDatas();
        ExtData ed1 = new ExtData(new ASN1ObjectIdentifier("1.17.177.777"), ASN1Boolean.TRUE, new DEROctetString(new byte[]{0x01}));
        eds.add(ed1);
        toSign.setExtDatas(eds);

        byte[] encoded = toSign.toASN1Primitive().getEncoded();
        System.out.println(Base64.toBase64String(encoded));

        // 反序列化
        TBS_Sign copy = TBS_Sign.getInstance(encoded);
        assertNotNull(copy.getExtDatas());
    }
}