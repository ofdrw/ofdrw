package org.ofdrw.core.integrity;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-28 19:59:43
 */
class FileListTest {

    public static FileList fileListCase(){
        return new FileList()
                .addFile("10089", "/OFD.xml")
                .addFile("10090", "/Doc_0/Pages/Page_0/Content.xml");
    }
    @Test
    public void gen() throws Exception {
        TestTool.genXml("FileList", fileListCase());
    }
}