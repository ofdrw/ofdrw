package org.ofdrw.core.signatures.sig;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.gv.GlobalVar;

import static org.junit.jupiter.api.Assertions.*;

public class ProviderTest {
    public static Provider providerCase(){
        return new Provider()
                .setCompany("ofdrw.org")
                .setProviderName("ofdrw")
                .setVersion(GlobalVar.Version)
                .setProtocolVer("1")
                .setExtendData(new byte[]{0x01,0x02});
    }
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Provider", providerCase());
    }
}