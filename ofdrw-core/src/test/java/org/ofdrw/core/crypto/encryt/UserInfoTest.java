package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-23 19:10:25
 */
class UserInfoTest {

    public static UserInfo UserInfoCase() {
        return new UserInfo()
                .setUserName("张三")
                .setUserType(UserInfo.UserTypeOwner)
                .setUserCert(new byte[32])
                .setEncryptedWK(new byte[64])
                .setIVValue(new byte[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15});
    }

    @Test
    public void testCase() {
        TestTool.genXml("UserInfo", UserInfoCase());
    }
}