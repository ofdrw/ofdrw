package org.ofdrw.pkg.container.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.doc.CT_CommonData;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicStructure.doc.Document;
import org.ofdrw.core.basicStructure.pageTree.Page;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.TT;

public class DocumentContent {

    public static Document doc(){
        CT_PageArea pageArea = new CT_PageArea()
                .setPhysicalBox(0, 0, 210, 297)
                .setApplicationBox(0, 0, 210, 297)
                .setContentBox(0, 0, 210, 297);
        CT_CommonData cdata = new CT_CommonData()
                .setMaxUnitID(4)
                .setPageArea(pageArea)
                .setPublicRes(new ST_Loc("PublicRes.xml"));
        Pages pages = new Pages()
                .addPage(new Page(1, "Pages/Page_0/Content.xml"));
        return new Document()
                .setCommonData(cdata)
                .setPages(pages);
    }

    @Test
    public void printReview() throws Exception {
        Document doc = doc();
        TT.dumpToTmpReview(doc);
    }
}
