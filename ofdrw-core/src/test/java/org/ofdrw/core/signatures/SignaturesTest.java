package org.ofdrw.core.signatures;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.OFDElement;

import java.util.List;

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

    @Test
    void setMaxSignId() {
        Signatures elem = signaturesCase();
        elem.setMaxSignId("s002");
        List<OFDElement> maxSignId = elem.getOFDElements("MaxSignId", OFDElement::new);
        assertEquals(maxSignId.size(), 1);
        assertEquals(maxSignId.get(0).getText(), "s002");
    }
}