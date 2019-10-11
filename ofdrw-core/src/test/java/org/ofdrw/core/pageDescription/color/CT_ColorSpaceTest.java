package org.ofdrw.core.pageDescription.color;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;

public class CT_ColorSpaceTest {
    public static CT_ColorSpace colorSpaceCase() {
        return new CT_ColorSpace()
                .setType(OFDColorSpaceType.RGB)
                .setBitsPerComponent(BitsPerComponent.BIT_8)
                .setProfile(ST_Loc.getInstance("./Res/color.xml"))
                .setPalette(PaletteTest.paletteCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("ColorSpace", colorSpaceCase());
    }
}