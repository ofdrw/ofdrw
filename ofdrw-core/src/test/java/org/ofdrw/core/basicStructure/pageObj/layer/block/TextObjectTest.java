package org.ofdrw.core.basicStructure.pageObj.layer.block;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.text.TextCode;
import org.ofdrw.core.text.text.CT_Text;

import static org.junit.jupiter.api.Assertions.*;

public class TextObjectTest {
    public static TextObject textObjectCase() {
        TextCode textCode = new TextCode()
                .setX(0.6747)
                .setY(3.5101)
                .setDeltaX(new ST_Array(1.9472, 1.8201, 0.8467, 0.8467))
                .setContent("hello");
        return new CT_Text().setBoundary(new ST_Box(31.0753, 25.9115, 8.5915, 4.4979))
                .setFont(ST_RefID.getInstance("4"))
                .setSize(3.6865)
                .addTextCode(textCode)
                .toObj(new ST_ID(3));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("TextObject", textObjectCase());
    }
}