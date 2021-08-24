package org.ofdrw.crypto.integrity;

import org.dom4j.DocumentException;
import org.junit.jupiter.api.Test;
import org.ofdrw.sign.verify.container.GBT35275ValidateContainer;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 测试验证OFD完整性
 *
 * @author 权观宇
 * @since 2021-08-24 19:45:25
 */
class OFDIntegrityVerifierTest {

    @Test
    void integrity() throws GeneralSecurityException, IOException, DocumentException {
        Path in = Paths.get("src/test/resources/hello-integrity.ofd");
        final OFDIntegrityVerifier ofdIntegrityVerifier = new OFDIntegrityVerifier();
        GMProtectVerifier gmProtectVerifier = new GMProtectVerifier(new GBT35275ValidateContainer());
        final boolean integrity = ofdIntegrityVerifier.integrity(in, gmProtectVerifier);
        System.out.println(integrity);
    }

}