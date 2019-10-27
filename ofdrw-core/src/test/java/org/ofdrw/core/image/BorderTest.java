package org.ofdrw.core.image;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

import static org.junit.jupiter.api.Assertions.*;

public class BorderTest {

    public static Border borderCase(){
        return new Border()
                .setLineWidth(0.353)
                .setHorizonalCornerRadius(1d)
                .setVerticalCornerRadius(1d)
                .setDashOffset(11d)
                .setDashPattern(new ST_Array(1, 2, 3))
                .setBorderColor(CT_Color.rgb(0, 0, 0));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("border", borderCase());
    }
}