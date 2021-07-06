package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.crypto.ProtectionCaseID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-23 19:18:49
 */
class DecyptSeedTest {

    @Test
    void addUserInfo() {
        DecyptSeed decyptSeed = new DecyptSeed()
                .setID("1")
                .setEncryptCaseId(ProtectionCaseID.EncryptGMCert)
                .addUserInfo(UserInfoTest.UserInfoCase())
                .addUserInfo(UserInfoTest.UserInfoCase().setUserName("李四").setUserType(null))
                .setExtendParams(new ExtendParams().addParameter("KEY", "Value"));

        TestTool.genXml("DecyptSeed", decyptSeed);
    }
}