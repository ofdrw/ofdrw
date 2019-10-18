package org.ofdrw.core.pageDescription.clips;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;
import org.ofdrw.core.graph.pathObj.AbbreviatedData;
import org.ofdrw.core.graph.pathObj.AbbreviatedDataTest;
import org.ofdrw.core.graph.pathObj.CT_Path;
import org.ofdrw.core.graph.pathObj.CT_PathTest;
import org.ofdrw.core.pageDescription.color.color.CT_Color;

import static org.junit.jupiter.api.Assertions.*;

public class ClipAreaTest {

    public static Area areaCase(){
        return new Area()
                .setDrawParam(ST_RefID.getInstance("11"))
                .setCTM(ST_Array.unitCTM())
                .setClipObj(CT_PathTest.pathCase());
    }

    public static Area areaCase2(){
        CT_Path path2 = CT_PathTest.pathCase()
                .setBoundary(new ST_Box(47, 97, 156, 206))
                .setGraphicName("Second")
                .setAbbreviatedData( new AbbreviatedData()
                        .M(150, 100)
                        .L(300, 50)
                        .L(300, 200)
                        .C());
        return new Area()
                .setDrawParam(ST_RefID.getInstance("75"))
                .setCTM(ST_Array.unitCTM())
                .setClipObj(path2);
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("ClipArea", areaCase());
    }
}