import org.bouncycastle.jcajce.provider.digest.SM3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;

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
            while (dis.read(buffer) != -1);
            mulDigest = md.digest();
        }

        md.reset();

        byte[] srcBin = Files.readAllBytes(path);
        final byte[] digest = md.digest(srcBin);

        Assertions.assertArrayEquals(mulDigest, digest);
    }
}
