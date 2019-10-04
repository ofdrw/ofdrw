package org.ofdrw.core.basicStructure.ofd.docInfo;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import java.time.LocalDate;


public class CT_DocInfoTest {

    public static CT_DocInfo docInfoCase(){
        CT_DocInfo docInfo = new CT_DocInfo();
        CustomDatas customDatas = new CustomDatas()
                .addCustomData("key1", "value1")
                .addCustomData("key2", "value2");

        docInfo.randomDocID()
                .setTile("Title")
                .setAuthor("Cliven")
                .setSubject("Subject")
                .setAbstract("This is abstract")
                .setCreationDate(LocalDate.now().minusDays(1L))
                .setModDate(LocalDate.now())
                .setDocUsage(DocUsage.Normal)
                .setCover(new ST_Loc("./Res/Cover.xml"))
                .addKeyword("111")
                .addKeyword("222")
                .setCreator("ofdrw")
                .setCreatorVersion("1.0.0-SNAPSHOT")
                .setCustomDatas(customDatas);
        return docInfo;
    }

    @Test
    public void genDocInfo(){
        TestTool.genXml("DocInfo", doc -> {
            CT_DocInfo docInfo = docInfoCase();
            doc.add(docInfo);
        });
    }

}