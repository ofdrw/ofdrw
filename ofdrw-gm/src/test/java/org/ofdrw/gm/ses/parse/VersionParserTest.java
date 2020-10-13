package org.ofdrw.gm.ses.parse;

import org.bouncycastle.util.encoders.Base64;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-10-12 18:59:50
 */
class VersionParserTest {

    @Test
    void parseSES_SealVersion() throws IOException {
        Path userSealV1Path = Paths.get("src/test/resources", "UserV1.esl");
        Path userSealV4Path = Paths.get("src/test/resources", "UserV4.esl");

        SESVersionHolder holder = VersionParser.parseSES_SealVersion(Files.readAllBytes(userSealV1Path));
        assertEquals(holder.getVersion(), SESVersion.v1);
        assertTrue(holder.SealObject() instanceof org.ofdrw.gm.ses.v1.SESeal);

        SESVersionHolder holder1 = VersionParser.parseSES_SealVersion(Files.readAllBytes(userSealV4Path));
        assertEquals(holder1.getVersion(), SESVersion.v4);
        assertTrue(holder1.SealObject() instanceof org.ofdrw.gm.ses.v4.SESeal);

        if (holder1.getVersion() == SESVersion.v4) {
            // 提供一个期待的版本类型返还值
            org.ofdrw.gm.ses.v4.SESeal seal = holder1.SealObject();
            System.out.println(Base64.toBase64String(seal.getCert().getOctets()));
        }
    }


    @Test
    void parseSES_SignatureVersion() throws IOException {
        Path sESValueV4 = Paths.get("src/test/resources", "SignedValueV4.dat");
        SESVersionHolder holder = VersionParser.parseSES_SealVersion(Files.readAllBytes(sESValueV4));
        assertEquals(holder.getVersion(), SESVersion.v4);
        assertTrue(holder.SESObject() instanceof org.ofdrw.gm.ses.v4.SES_Signature);
        if (holder.getVersion() == SESVersion.v4) {
            // 提供一个期待的版本类型返还值
            org.ofdrw.gm.ses.v4.SES_Signature sesSig = holder.SESObject();
            System.out.println(Base64.toBase64String(sesSig.getCert().getOctets()));
        }
    }
}