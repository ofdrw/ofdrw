package org.ofdrw.core.basicStructure.pageObj;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.layer.CT_LayerTest;

import static org.junit.jupiter.api.Assertions.*;

public class ContentTest {
    public static Content contentCase(){
        return new Content()
                .addLayer(CT_LayerTest.layerCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Content", contentCase());
    }
}