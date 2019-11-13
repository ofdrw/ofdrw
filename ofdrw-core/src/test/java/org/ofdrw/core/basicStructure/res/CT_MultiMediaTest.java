package org.ofdrw.core.basicStructure.res;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class CT_MultiMediaTest {
    public static CT_MultiMedia multiMediaCase() {
        return new CT_MultiMedia()
                .setType(MediaType.Image)
                .setFormat("PNG")
                .setMediaFile(ST_Loc.getInstance("/Res/img.PNG"))
                .setID(new ST_ID(10051));

    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_MultiMedia", multiMediaCase());
    }
}