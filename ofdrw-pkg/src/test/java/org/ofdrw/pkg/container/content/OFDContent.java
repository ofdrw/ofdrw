package org.ofdrw.pkg.container.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.ofd.DocBody;
import org.ofdrw.core.basicStructure.ofd.OFD;
import org.ofdrw.core.basicStructure.ofd.docInfo.CT_DocInfo;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.TT;

import java.time.LocalDate;
import java.util.UUID;

public class OFDContent {
    public static OFD ofd(){
        CT_DocInfo docInfo = new CT_DocInfo()
                .setDocID(UUID.randomUUID())
                .setAuthor("权观宇")
                .setCreationDate(LocalDate.now())
                .setCreator("ofd r&w")
                .setCreatorVersion("1.0.0-SNAPSHOT");
        DocBody docBody = new DocBody()
                .setDocInfo(docInfo)
                .setDocRoot(new ST_Loc("Doc_0/Document.xml"));
        return new OFD()
                .addDocBody(docBody);
    }

    @Test
    public void printReview() throws Exception {
        OFD ofd = ofd();
        TT.dumpToTmpReview(ofd);
    }
}
