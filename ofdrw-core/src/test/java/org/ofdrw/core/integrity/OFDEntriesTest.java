package org.ofdrw.core.integrity;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.gv.GlobalVar;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-28 20:04:16
 */
class OFDEntriesTest {
    @Test
    public void gen() {
        OFDEntries entries = new OFDEntries()
                .setID("10023")
                .setCreatorName("ofdrw-encrypt")
                .setVersion(GlobalVar.Version)
                .setCreationDate(LocalDateTime.now())
                .setFileList(FileListTest.fileListCase())
                .setSignedValueLoc(new ST_Loc("/sign.dat"));
        TestTool.genXml("FileList", entries);

    }
}