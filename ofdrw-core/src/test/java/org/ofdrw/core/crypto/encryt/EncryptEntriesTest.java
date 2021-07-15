package org.ofdrw.core.crypto.encryt;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-15 19:46:34
 */
class EncryptEntriesTest {
    @Test
    public void gen() throws Exception {
        final EncryptEntries entries = new EncryptEntries()
                .setID("1")
                .addEncryptEntry("/OFD.xml", "/OFD.dat")
                .addEncryptEntry("/Doc_0/Document.xml", "/Doc_0/Document.dat");
        TestTool.genXml("EncryptEntries", entries);
    }
}