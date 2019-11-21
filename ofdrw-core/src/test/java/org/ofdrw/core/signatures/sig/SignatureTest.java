package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class SignatureTest {
    public static Signature signatureCase() {
        return new Signature()
                .setSignedInfo(SignedInfoTest.signedInfoCase())
                .setSignedValue(new ST_Loc("/Doc_0/Signs/Sign_0/SignedValue.dat"));
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Signature", signatureCase());
    }
}