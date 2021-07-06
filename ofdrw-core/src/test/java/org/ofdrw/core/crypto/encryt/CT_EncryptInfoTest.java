package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.signatures.sig.Parameters;
import org.ofdrw.gv.GlobalVar;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-24 20:18:19
 */
class CT_EncryptInfoTest {


    @Test
    public void caseTest() throws Exception {
        Provider provider = new Provider();
        provider.setProviderName("ofdrw-encrypt")
                .setCompany("ofdrw")
                .setVersion(GlobalVar.Version)
                .setExtendData(new byte[16]);
        CT_EncryptInfo info = new CT_EncryptInfo()
                .setID("2")
                .setRelative("1")
                .setProvider(provider)
                .setEncryptScope("All")
                .setParameters(new Parameters()
                        .addParameter("KEY1", "String", "VALUE")
                        .addParameter("KEY2", "VALUE2")
                        .addParameter("KEY3", null))
                .setEncryptDate(LocalDateTime.now())
                .setDecryptSeedLoc("/decryotseed.dat")
                .setEntriesMapLoc("/entriesmap.dat");

        TestTool.genXml("EncryptInfo", info);

    }
}