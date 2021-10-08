package org.ofdrw.core.basicStructure.doc;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageTree.CT_TemplatePageTest;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

import java.util.List;

public class CT_CommonDataTest {


    public static CT_CommonData commonDataCase() {
        return new CT_CommonData()
                .setMaxUnitID(ST_ID.getInstance("777"))
                .setPageArea(CT_PageAreaTest.pageAreaCase())
                .setPublicRes(ST_Loc.getInstance("./Res"))
                .setDocumentRes(ST_Loc.getInstance("./Res"))
                .addTemplatePage(CT_TemplatePageTest.templatePageCase())
                .addTemplatePage(CT_TemplatePageTest.templatePageCase())
                .setDefaultCS(ST_RefID.getInstance("123"));
    }

    @Test
    public void gen() {
        TestTool.genXml("CommonData", commonDataCase());
    }

    @Test
    void addDocumentRes() {
        final CT_CommonData commonData = new CT_CommonData().addPublicRes(ST_Loc.getInstance("PublicRes.xml"))
                .addPublicRes(ST_Loc.getInstance("PublicRes_1.xml"))
                .addPublicRes(ST_Loc.getInstance(" "));
        TestTool.genXml("CommonData", commonData);
        final List<ST_Loc> list = commonData.getPublicResList();
        Assertions.assertEquals(2, list.size());
    }

    @Test
    void testAddDocumentRes() {
        final CT_CommonData commonData = new CT_CommonData()
                .addDocumentRes(ST_Loc.getInstance("DocumentRes.xml"))
                .addDocumentRes(ST_Loc.getInstance("DocumentRes_1.xml"))
                .addDocumentRes(ST_Loc.getInstance(" "));
        TestTool.genXml("CommonData", commonData);
        final List<ST_Loc> list = commonData.getDocumentResList();
        Assertions.assertEquals(2, list.size());
    }
}