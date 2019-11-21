package org.ofdrw.core.signatures.range;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.PageTest;
import org.ofdrw.core.basicType.ST_Loc;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class ReferenceTest {
    public static Reference referenceCase() {
        Page res  = PageTest.pageCase();
        ST_Loc fileRef = new ST_Loc("/Doc_0/Pages/Page_0/Content.xml");
        try {
            MessageDigest sm3 = MessageDigest.getInstance("SM3", new BouncyCastleProvider());
            byte[] plainText = TestTool.xmlByte(res);
            sm3.update(plainText);
            byte[] checkValue = sm3.digest();
            return new Reference(fileRef, checkValue);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Reference", referenceCase());
    }
}