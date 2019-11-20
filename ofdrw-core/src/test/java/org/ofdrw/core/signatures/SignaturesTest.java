package org.ofdrw.core.signatures;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class SignaturesTest {
    public static Signatures signaturesCase() {
        return new Signatures()
                .setMaxSignId("s002")
                .addSignature(SignatureTest.signatureCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Signatures", signaturesCase());
    }
}