package org.ofdrw.pkg.dir.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.ColorSpaces;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.dir.TT;

public class PublicResContent {
    public static Res res() {
        ColorSpaces cs = new ColorSpaces()
                .addColorSpace(new CT_ColorSpace(OFDColorSpaceType.RGB, 5));
        CT_Font calibri = new CT_Font()
                .setFontName("NotoSerifCJKsc")
                .setFamilyName("Medium")
                .setID(4)
                .setFontFile(new ST_Loc("Res/NotoSerifCJKsc-Medium.otf"));

        Fonts fonts = new Fonts()
                .addFont(calibri);
        return new Res()
                .addResource(cs)
                .addResource(fonts);
    }

    @Test
    public void printReview() throws Exception {
        Res res = res();
        TT.dumpToTmpReview(res);
    }
}
