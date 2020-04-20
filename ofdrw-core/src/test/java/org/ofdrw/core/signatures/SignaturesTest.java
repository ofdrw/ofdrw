package org.ofdrw.core.signatures;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class SignaturesTest {
    public static Signatures signaturesCase() {
        return new Signatures()
                .setMaxSignId("s001")
                .addSignature(SignatureTest.signatureCase());
    }

    @Test
    public void gen() throws Exception {
        Signatures element = signaturesCase();
        assertEquals("s001", element.getMaxSignId());
        TestTool.genXml("Signatures", element);
    }
}