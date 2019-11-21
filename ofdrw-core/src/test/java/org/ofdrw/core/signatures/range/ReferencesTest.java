package org.ofdrw.core.signatures.range;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class ReferencesTest {
    public static References referencesCase() {
        return new References()
                .setCheckMethod(CheckMethod.SM3.toString())
                .addReference(ReferenceTest.referenceCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("References", referencesCase());
    }
}