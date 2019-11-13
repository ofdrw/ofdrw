package org.ofdrw.core.basicStructure.res;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.res.resources.ColorSpaces;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.text.font.CT_Font;

import static org.junit.jupiter.api.Assertions.*;

public class ResTest {

    public static Res resCase(){
        ColorSpaces colorSpaces = new ColorSpaces()
                .addColorSpace(new CT_ColorSpace().setID(new ST_ID(5)).setType(OFDColorSpaceType.RGB));

        Fonts fonts = new Fonts()
                .addFont(new CT_Font().setFamilyName("Calibri")
                        .setFontName("Calibri")
                        .setID(new ST_ID(4))
                        .setFontFile(ST_Loc.getInstance("Font4.ttf")));

        return new Res()
                .setBaseLoc(ST_Loc.getInstance("Res"))
                .addResource(colorSpaces)
                .addResource(fonts);
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("PublicRes", resCase());
    }

}