package org.ofdrw.core.signatures.appearance;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class SealTest {
    public static Seal sealCase() {
        return new Seal(new ST_Loc("/Doc_0/Signs/Sign_0/Seal.esl"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Seal", sealCase());

        Seal seal = sealCase().setImageLoc(new ST_Loc("/Doc_0/Signs/Sign_0/Seal.png"));
        TestTool.genXml("SealWithImage",seal);

        seal = sealCase().setImageLoc(new ST_Loc("/Doc_0/Signs/Sign_0/Seal.png"));
        seal.setImageLoc(null);
        TestTool.genXml("SealDeleteImage",seal);
    }
}