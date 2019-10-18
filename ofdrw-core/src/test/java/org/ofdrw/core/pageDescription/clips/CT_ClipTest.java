package org.ofdrw.core.pageDescription.clips;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class CT_ClipTest {
    public static CT_Clip clipCase() {
        Area area1 = ClipAreaTest.areaCase();
        Area area2 = ClipAreaTest.areaCase2();
        return new CT_Clip()
                .addArea(area1)
                .addArea(area2);
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Path", clipCase());
    }
}