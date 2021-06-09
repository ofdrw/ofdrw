package org.ofdrw.core.pageDescription.color;

import org.dom4j.Element;
import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.BitsPerComponent;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.CV;
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
    public void testClone(){
        final CT_ColorSpace src = colorSpaceCase();
        final CT_ColorSpace clone = new CT_ColorSpace((Element) src.clone());
        clone.setProfile(ST_Loc.getInstance("./Res/MyColor.xml"));
        clone.getPalette().addCV(new CV(new ST_Array(new String[]{"255", "255", "0"})));

        TestTool.genXml("ColorSpace", src);
        TestTool.genXml("ColorSpace",clone);
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("ColorSpace", colorSpaceCase());
    }
}