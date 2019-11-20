package org.ofdrw.core.signatures;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class SignatureTest {
    public static Signature signatureCase(){
        return new Signature()
                .setID("s001")
                .setType(SigType.Seal)
                .setBaseLoc(ST_Loc.getInstance("Doc_N/Signs/Sign_N/Signature.xml"));
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Signature", signatureCase());
    }
}