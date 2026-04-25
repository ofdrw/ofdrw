package org.ofdrw.sign.signContainer;

import org.junit.jupiter.api.Test;
import org.ofdrw.gm.cert.PKCS12Tools;
import org.ofdrw.gm.ses.v5.SESeal;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.sign.NumberFormatAtomicSignID;
import org.ofdrw.sign.OFDSigner;
import org.ofdrw.sign.SignMode;
import org.ofdrw.sign.stamppos.NormalStampPos;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SESV5ContainerTest {

    @Test
    public void sign() throws GeneralSecurityException, IOException {
        Path userP12Path = Paths.get("src/test/resources", "USER.p12");
        Path sealPath = Paths.get("src/test/resources", "UserV5.esl");

        PrivateKey prvKey = PKCS12Tools.ReadPrvKey(userP12Path, "private", "777777");
        Certificate signCert = PKCS12Tools.ReadUserCert(userP12Path, "private", "777777");
        SESeal seal = SESeal.getInstance(Files.readAllBytes(sealPath));

        Path src = Paths.get("src/test/resources", "helloworld.ofd");
        Path out = Paths.get("target/SESV5SignDoc.ofd");

        try (OFDReader reader = new OFDReader(src);
             OFDSigner signer = new OFDSigner(reader, out, new NumberFormatAtomicSignID())) {
            SESV5Container signContainer = new SESV5Container(prvKey, seal, signCert);
            signer.setSignMode(SignMode.ContinueSign);
            signer.setSignContainer(signContainer);
            signer.addApPos(new NormalStampPos(1, 50, 50, 40, 40));
            signer.exeSign();
        }
        assertTrue(Files.exists(out));
        assertTrue(Files.size(out) > 0);
        System.out.println(">> V5签章文件: " + out.toAbsolutePath());
    }
}
