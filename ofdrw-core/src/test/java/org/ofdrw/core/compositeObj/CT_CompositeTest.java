package org.ofdrw.core.compositeObj;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;

import static org.junit.jupiter.api.Assertions.*;

public class CT_CompositeTest {

    public static CT_Composite compositeCase(){
        return new CT_Composite()
                .setBoundary(new ST_Box(40, 40, 50, 50))
                .setResourceID(ST_RefID.getInstance("1005"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Composite", compositeCase());
    }
}