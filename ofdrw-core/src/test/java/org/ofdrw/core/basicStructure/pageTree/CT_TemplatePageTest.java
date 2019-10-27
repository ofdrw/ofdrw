package org.ofdrw.core.basicStructure.pageTree;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

public class CT_TemplatePageTest {

    public static CT_TemplatePage templatePageCase(){
        return new CT_TemplatePage()
                .setID(new ST_ID(777))
                .setTemplatePageName("NAME")
                .setZOrder(Type.Background)
                .setBaseLoc(ST_Loc.getInstance("./Res/Templates/page.xml"));

    }
    @Test
    public void gen(){
        TestTool.genXml("TemplatePage",templatePageCase());
    }

}