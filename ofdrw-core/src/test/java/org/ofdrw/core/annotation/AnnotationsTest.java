package org.ofdrw.core.annotation;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.annotation.pageannot.AnnPage;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

public class AnnotationsTest {

    public static Annotations annotationsCase(){
        return new Annotations()
                .addPage(new AnnPage().setPageID(new ST_ID(57)).setFileLoc(ST_Loc.getInstance("./Annotations.xml")));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Annotations", annotationsCase());
    }
}