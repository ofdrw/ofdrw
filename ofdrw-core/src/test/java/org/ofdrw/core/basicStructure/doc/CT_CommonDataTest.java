package org.ofdrw.core.basicStructure.doc;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageTree.CT_TemplatePageTest;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

public class CT_CommonDataTest {


    public static CT_CommonData commonDataCase(){
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
    public void gen(){
        TestTool.genXml("CommonData", commonDataCase());
    }
}