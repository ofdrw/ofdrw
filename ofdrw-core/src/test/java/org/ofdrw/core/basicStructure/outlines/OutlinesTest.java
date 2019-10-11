package org.ofdrw.core.basicStructure.outlines;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

public class OutlinesTest {

    public static Outlines outlinesCase() {
        return new Outlines()
                .addOutlineElem(CT_OutlineElemTest.outlineElemCase())
                .addOutlineElem(CT_OutlineElemTest.outlineElemCase());
    }


    @Test
    public void gen(){
        TestTool.genXml("Outlines", outlinesCase());
    }
}