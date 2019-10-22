package org.ofdrw.core.text.text;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.text.TextCode;

import static org.junit.jupiter.api.Assertions.*;

public class CT_TextTest {
    public static CT_Text textCase(){
        CT_Text res = new CT_Text();
        TextCode fontTxt = new TextCode()
                .setX(0d)
                .setY(25d)
                .setDeltaX(new ST_Array(14, 14, 14))
                .setContent("Font");
        TextCode fontCN = new TextCode()
                .setX(60d)
                .setY(25d)
                .setDeltaX(new ST_Array(25))
                .setContent("字形");
        return res.setFont(ST_RefID.getInstance("2"))
                .setSize(25.4d)
                .setBoundary(new ST_Box(50, 20, 112, 26))
                .addTextCode(fontTxt)
                .addTextCode(fontCN);

    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Text", textCase());
    }
}