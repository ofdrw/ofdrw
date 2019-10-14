package org.ofdrw.core.pageDescription.color.color;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;

import static org.junit.jupiter.api.Assertions.*;

public class CT_ColorTest {

    public static CT_Color colorCase() {
        return new CT_Color()
                .setValue(new ST_Array(new String[]{"255", "255", "255"}))
                .setAlpha(255);
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("color", colorCase());
    }

}