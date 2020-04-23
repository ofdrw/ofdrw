package org.ofdrw.gm.ses.v4;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1GeneralizedTime;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.DERIA5String;
import org.bouncycastle.asn1.gm.GMObjectIdentifiers;
import org.bouncycastle.jcajce.provider.asymmetric.x509.CertificateFactory;
import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v1.SES_Header;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
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
        sg.update(toSign.getEncoded("DER"));
        final byte[] sigVal = sg.sign();
        SES_Signature signature = new SES_Signature()
                .setToSign(toSign)
                .setCert(useCert)
                .setSignatureAlgID(GMObjectIdentifiers.sm2sign_with_sm3)
                .setSignature(sigVal);

        Path out = Paths.get("target/SignedValueV4.dat");
        Files.write(out, signature.getEncoded("DER"));
        System.out.println(">> V4版本电子签章存储于: " + out.toAbsolutePath().toAbsolutePath());
    }


    @Test
    public void verify() throws IOException, NoSuchAlgorithmException, CertificateException, InvalidKeyException, SignatureException {

        Path path = Paths.get("src/test/resources", "SignedValue.dat");
        Path srcPath = Paths.get("src/test/resources", "Signature.xml");

//        Path path = Paths.get("target", "UserV4.esl");
        SES_Signature sesSignature = SES_Signature.getInstance(Files.readAllBytes(path));

        MessageDigest md = new SM3.Digest();
        byte[] digest = md.digest(Files.readAllBytes(srcPath));
        final ASN1BitString dataHash = sesSignature.getToSign().getDataHash();
        System.out.println(Arrays.equals(digest, dataHash.getOctets()));

        ASN1OctetString cert = sesSignature.getCert();
        CertificateFactory factory = new CertificateFactory();
        X509Certificate certificate = (X509Certificate) factory.engineGenerateCertificate(cert.getOctetStream());

        TBS_Sign toSign = sesSignature.getToSign();

        Signature sg = Signature.getInstance(
                sesSignature.getSignatureAlgID().toString()
                , new BouncyCastleProvider());
        sg.initVerify(certificate);
        sg.update(toSign.getEncoded("DER"));
        byte[] sigVal = sesSignature.getSignature().getBytes();

        System.out.println(sg.verify(sigVal));
    }

}