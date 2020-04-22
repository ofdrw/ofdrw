import org.bouncycastle.jcajce.provider.digest.SM3;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.pkg.container.SignDir;
import org.ofdrw.pkg.container.SignsDir;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.Certificate;

public class Sm3DigestTest {

    @Test
    void testName() {
        MessageDigest md = new SM3.Digest();
        System.out.println(md.getAlgorithm());
    }

    @Test
    void testStreamCalculate() throws IOException {
        MessageDigest md = new SM3.Digest();
        Path path = Paths.get("src/test/resources", "helloworld.ofd");
        byte[] mulDigest;
        try (final InputStream inputStream = Files.newInputStream(path);
             DigestInputStream dis = new DigestInputStream(inputStream, md)) {
            byte[] buffer = new byte[1024];
            while (dis.read(buffer) != -1) ;
            mulDigest = md.digest();
        }

        md.reset();

        byte[] srcBin = Files.readAllBytes(path);
        final byte[] digest = md.digest(srcBin);

        Assertions.assertArrayEquals(mulDigest, digest);
    }


    @Test
    void signTest() throws GeneralSecurityException, IOException {
        Path path = Paths.get("src/test/resources", "USER.p12");
        Certificate cert = PKCS12Tools.ReadUserCert(path, "private", "777777");
        PrivateKey prv = PKCS12Tools.ReadPrvKey(path, "private", "777777");

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(prv);
        sg.update(new byte[32]);
        final byte[] sign = sg.sign();

        sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initVerify(cert);
        sg.update(new byte[32]);
        Assertions.assertTrue(sg.verify(sign));
    }

    @Test
    void testSign() throws GeneralSecurityException, IOException {
        Path ks = Paths.get("src/test/resources", "USER.p12");
        Path in = Paths.get("target/DigitalSign/Doc_0/Signs/Sign_0/Signature.xml");
        Path out = Paths.get("target/DigitalSign/Doc_0/Signs/Sign_0/", SignDir.SignedValueFileName);

        Certificate cert = PKCS12Tools.ReadUserCert(ks, "private", "777777");
        PrivateKey prv = PKCS12Tools.ReadPrvKey(ks, "private", "777777");

        Signature sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initSign(prv);
        sg.update(Files.readAllBytes(in));
        byte[] sign = sg.sign();
        Files.write(out, sign);

        sg = Signature.getInstance("SM3WithSM2", new BouncyCastleProvider());
        sg.initVerify(cert);
        sg.update(Files.readAllBytes(in));
        System.out.println(sg.verify(sign));
    }
}
