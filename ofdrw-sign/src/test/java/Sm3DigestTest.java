import org.bouncycastle.jcajce.provider.digest.SM3;
import org.junit.jupiter.api.Test;

import java.security.MessageDigest;

public class Sm3DigestTest {

    @Test
    void testName() {
        MessageDigest md = new SM3.Digest();
        System.out.println(md.getAlgorithm());
    }
}
