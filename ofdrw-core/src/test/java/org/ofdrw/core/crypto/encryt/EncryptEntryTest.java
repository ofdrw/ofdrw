package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-15 19:41:11
 */
class EncryptEntryTest {
    @Test
    void gen() {
        TestTool.genXml("EncryptEntry", new EncryptEntry("/OFD.xml", "/OFD.dat", "/myseed.dat"));
    }
}