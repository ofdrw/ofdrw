package org.ofdrw.core.structure.ofd;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.structure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.structure.ofd.docInfo.CT_DocInfoTest;
import org.ofdrw.core.structure.versions.VersionsTest;

import static org.junit.jupiter.api.Assertions.*;

public class DocBodyTest {

    public static DocBody docBodyCase() {
        DocBody docBody = new DocBody();
        docBody.setDocInfo(CT_DocInfoTest.docInfoCase())
                .setDocRoot(new ST_Loc("./Res/info.xml"))
                .setVersions(VersionsTest.versionsCase())
                .setSignatures(new ST_Loc("./Signs/Sign1"));
        return docBody;
    }

    @Test
    public void gen() {
        TestTool.genXml("DocBody", doc -> doc.add(docBodyCase()));
    }
}