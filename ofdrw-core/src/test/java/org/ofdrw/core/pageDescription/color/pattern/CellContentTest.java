package org.ofdrw.core.pageDescription.color.pattern;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.image.CT_ImageTest;

import static org.junit.jupiter.api.Assertions.*;

public class CellContentTest {
    public static CellContent cellContentCase(){
        return new CellContent()
                .addPageBlock(CT_ImageTest.imageCase().toObj(new ST_ID(79)));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CellContent", cellContentCase());
    }
}