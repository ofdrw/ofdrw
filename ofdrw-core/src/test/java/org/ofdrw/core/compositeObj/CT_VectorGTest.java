package org.ofdrw.core.compositeObj;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObject;
import org.ofdrw.core.basicStructure.pageObj.layer.block.PathObjectTest;
import org.ofdrw.core.graph.pathObj.CT_PathTest;

import static org.junit.jupiter.api.Assertions.*;

public class CT_VectorGTest {
    public static CT_VectorG vectorGCase() {
        return new CT_VectorG()
                .setWidth(132d)
                .setHeight(32d)
                .addContent(PathObjectTest.pathObjectCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_VectorG", vectorGCase());
    }
}