package org.ofdrw.core.basicStructure.res.resources;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicStructure.res.CT_MultiMediaTest;

import static org.junit.jupiter.api.Assertions.*;

public class MultiMediasTest {

    public static MultiMedias multiMediasCase(){
        return new MultiMedias()
                .addMultiMedia(CT_MultiMediaTest.multiMediaCase());
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("MultiMedias", multiMediasCase());
    }
}