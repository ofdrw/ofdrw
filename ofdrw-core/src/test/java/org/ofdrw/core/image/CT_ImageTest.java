package org.ofdrw.core.image;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;

import static org.junit.jupiter.api.Assertions.*;

public class CT_ImageTest {
    public static CT_Image imageCase(){
        return new CT_Image()
                .setBoundary(new ST_Box(0, 0, 100, 100))
                .setResourceID(ST_RefID.getInstance("15"))
                .setSubstitution(ST_RefID.getInstance("11"))
                .setImageMask(ST_RefID.getInstance("12"))
                .setBorder(BorderTest.borderCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Image", imageCase());
    }
}