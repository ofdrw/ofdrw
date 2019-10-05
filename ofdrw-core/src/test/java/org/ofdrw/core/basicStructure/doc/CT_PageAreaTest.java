package org.ofdrw.core.basicStructure.doc;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import static org.junit.jupiter.api.Assertions.*;


public class CT_PageAreaTest {

    public static CT_PageArea pageAreaCase(){
        return new CT_PageArea()
                .setPhysicalBox(new ST_Box(0, 0, 540, 700))
                .setApplicationBox(new ST_Box(10, 10, 520, 680))
                .setContentBox(new ST_Box(20, 20, 500, 660))
                .setBleedBox(new ST_Box(30, 30, 480, 640));
    }

    @Test
    public void get(){
        TestTool.validate("PageArea", element -> {
            CT_PageArea pageArea = new CT_PageArea(element);
            ST_Box pb = pageArea.getPhysicalBox();
            assertEquals(pb.getTopLeftX(), 0);
            assertEquals(pb.getTopLeftY(), 0);
            assertEquals(pb.getWidth(), 540);
            assertEquals(pb.getHeight(), 700);
        });
    }

    @Test
    public void gen(){
        TestTool.genXml("PageArea", pageAreaCase());
    }
}