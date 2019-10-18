package org.ofdrw.core.text.font;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class CT_FontTest {

    public static  CT_Font fontCase(){
        return new CT_Font()
                .setFontName("MySong")
                .setFamilyName("Song")
                .setCharset(Charset.unicode)
                .setItalic(false)
                .setBold(false)
                .setSerif(false)
                .setFixedWidth(false)
                .setFontFile(ST_Loc.getInstance("./Res/mysong.font"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Font", fontCase());
    }
}