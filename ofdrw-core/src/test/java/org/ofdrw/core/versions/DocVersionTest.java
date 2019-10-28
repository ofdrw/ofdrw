package org.ofdrw.core.versions;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class DocVersionTest {
    public static DocVersion docVersionCase(){
        FileList fileList = new FileList()
                .addFile("12", new ST_Loc("./Res/VersionFile_1.xml"))
                .addFile("13", new ST_Loc("./Res/VersionFile_2.xml"));

        return new DocVersion()
                .setID("77")
                .setVersion("Version")
                .setDocVersionName("MyVer")
                .setCreationDate(LocalDate.now())
                .setFileList(fileList)
                .setDocRoot(ST_Loc.getInstance("./Res/DocRoot.xml"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("DocVersion", docVersionCase());
    }

}