package org.ofdrw.core.graph;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

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