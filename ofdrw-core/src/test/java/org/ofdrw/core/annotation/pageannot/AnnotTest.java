package org.ofdrw.core.annotation.pageannot;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObjectTest;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_ID;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class AnnotTest {

    public static Annot annotCase() {
        Appearance appearance = new Appearance(new ST_Box(50, 50, 200, 200))
                .addPageBlock(PathObjectTest.pathObjectCase());

        return new Annot()
                .setID(new ST_ID(19))
                .setType(AnnotType.Path)
                .setCreator("ofdrw")
                .setLastModDate(LocalDate.now())
                .setRemark("这是一段说明内容")
                .addParameter("Key", "Value")
                .addParameter("Key", "Value2")
                .addParameter("Key2", "Value3")
                .setAppearance(appearance);
    }
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Annot", annotCase());
    }
}