package org.ofdrw.core.basicStructure.pageObj.layer;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObjectTest;
import org.ofdrw.core.basicType.ST_ID;

import static org.junit.jupiter.api.Assertions.*;

public class CT_LayerTest {
    public static CT_Layer layerCase() {
        CT_Layer layer = new CT_Layer();
        layer.addPageBlock(TextObjectTest.textObjectCase());
        layer.setObjID(new ST_ID(2));
        return layer;
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Layer", layerCase());
    }
}