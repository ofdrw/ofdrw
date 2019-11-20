package org.ofdrw.core.extensions;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class ExtensionsTest {
    public static Extensions extensionsCase(){
        return new Extensions()
                .addExtension(CT_ExtensionTest.extensionCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Extensions", extensionsCase());
    }
}