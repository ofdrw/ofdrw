package org.ofdrw.core.basicStructure.pageObj;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicType.ST_Box;

import static org.junit.jupiter.api.Assertions.*;

public class PageTest {
    public static Page pageCase(){

        CT_PageArea area = new CT_PageArea()
                .setPhysicalBox(new ST_Box(0, 0, 210, 297));
        return new Page()
                .setArea(area)
                .setContent(ContentTest.contentCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Page", pageCase());
    }
}