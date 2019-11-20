package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderTest {
    public static Provider providerCase(){
        return new Provider()
                .setCompany("ofdrw.org")
                .setProviderName("ofdrw")
                .setVersion("1.0.0-SNAPSHOT");
    }
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Provider", providerCase());
    }
}