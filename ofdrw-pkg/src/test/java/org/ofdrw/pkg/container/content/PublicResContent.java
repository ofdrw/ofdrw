package org.ofdrw.pkg.container.content;

import org.junit.jupiter.api.Test;
import org.ofdrw.core.basicStructure.res.Res;
import org.ofdrw.core.basicStructure.res.resources.ColorSpaces;
import org.ofdrw.core.basicStructure.res.resources.Fonts;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.pageDescription.color.colorSpace.CT_ColorSpace;
import org.ofdrw.core.pageDescription.color.colorSpace.OFDColorSpaceType;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.pkg.container.TT;

public class PublicResContent {
    public static Res res() {
        CT_Font serifCJKsc = new CT_Font()
                .setFontName("NotoSerifCJKsc")
                .setFamilyName("Medium")
                .setID(4)
                .setFontFile(new ST_Loc("Res/NotoSerifCJKsc-Medium.otf"));

        Fonts fonts = new Fonts()
                .addFont(serifCJKsc);
        return new Res()
                .addResource(fonts);
    }

    public static Res resSysFont() {
        CT_Font serifCJKsc = new CT_Font()
                .setFontName("宋体")
                .setFamilyName("宋体")
                .setID(4);

        Fonts fonts = new Fonts()
                .addFont(serifCJKsc);
        return new Res()
                .addResource(fonts);
    }

    @Test
    public void printReview() throws Exception {
        Res res = res();
        TT.dumpToTmpReview(res);
    }
}
