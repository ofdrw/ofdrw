package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_ESPictrueInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * @author 权观宇
 * @since 2020-04-21 19:36:21
 */
public class ReaderImg {
    @Test
    public void certP() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SignatureException {
        final Path path = Paths.get("src/test/resources", "Seal.esl");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(path));
        Path out = Paths.get("target", "Seal.esl");
        ASN1BitString signedValue = seal.getSignedValue();
        byte[] octets = signedValue.getOctets();
        System.out.println(octets.length);
        octets[octets.length - 1] = 0;

        seal.setSignedValue(octets);
        Files.write(out, seal.getEncoded());
    }

    @Test
    void rootCert() throws IOException, GeneralSecurityException {
        final Path path = Paths.get("src/main/resources", "ROOT.p12");
        final Path out = Paths.get("target", "ROOT.crt");

        Certificate certificate = PKCS12Tools.ReadUserCert(path, "private", "123456");
        Files.write(out, certificate.getEncoded());
    }

    @Test
    void write() throws IOException {
        final Path path = Paths.get("src/test/resources", "Seal.esl");
        final Path out = Paths.get("target", "Seal.esl");

        SESeal seal = SESeal.getInstance(Files.readAllBytes(path));
        SES_SealInfo sealInfo = seal.geteSealInfo();
        sealInfo.setEsID("1234567");

        Files.write(out, seal.getEncoded());
    }
}
