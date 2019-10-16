package org.ofdrw.core.graph;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.graph.tight.CT_Region;

public class CT_RegionTest {
    public static CT_Region regionCase(){
        return new CT_Region()
                .addArea(CT_AreaTest.areaCase())
                .addArea(CT_AreaTest.areaCase());
    }


    @Test
    public void gen(){
        TestTool.genXml("Region", regionCase());
    }
}