package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.signatures.appearance.SealTest;
import org.ofdrw.core.signatures.appearance.StampAnnotTest;
import org.ofdrw.core.signatures.range.ReferencesTest;

import static org.junit.jupiter.api.Assertions.*;

public class SignedInfoTest {
    public static SignedInfo signedInfoCase() {
        return new SignedInfo()
                .setProvider(ProviderTest.providerCase())
                .setReferences(ReferencesTest.referencesCase())
                .addStampAnnot(StampAnnotTest.stampAnnotCase())
                .setParameters(ParametersTest.ParametersCase())
                .setSeal(SealTest.sealCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("SignedInfo", signedInfoCase());
    }
}