package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class SES_SignatureTest {

    @Test
    void build() throws IOException, GeneralSecurityException {
        Path userSealPath = Paths.get("src/test/resources", "UserV4.esl");
        Path userP12 = Paths.get("src/test/resources", "USER.p12");


        SESeal seal = SESeal.getInstance(Files.readAllBytes(userSealPath));
        TBS_Sign toSign = new TBS_Sign()
                .setVersion(SES_Header.V4)
                .setEseal(seal)
                .setTimeInfo(new ASN1GeneralizedTime(new Date()))
                .setDataHash(new byte[32])
                .setPropertyInfo("/Doc_0/Signs/Sign_0/Signature.xml");

        Certificate useCert = PKCS12Tools.ReadUserCert(userP12, "private", "777777");
        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12, "private", "777777");

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(prvKey);
        sg.update(toSign.getEncoded());
        final byte[] sigVal = sg.sign();
        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(useCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignature(sigVal);

        Path out = Paths.get("target/SignedValueV4.dat");
        Files.write(out, signature.getEncoded());
        System.out.println(">> V4版本电子签章存储于: " + out.toAbsolutePath().toAbsolutePath());
    }
}