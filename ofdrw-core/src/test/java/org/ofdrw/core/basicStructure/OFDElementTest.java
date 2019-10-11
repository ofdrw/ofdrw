package org.ofdrw.core.basicStructure;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.OFDSimpleTypeElement;

class OFDElementTest {


    @Test
    void addOFDEntity() {

        TestTool.genXml("AddST", doc -> {
            OFDSimpleTypeElement stEle = new OFDSimpleTypeElement("Name", "Value");
            stEle.addOFDEntity("SubName", "value2");
            doc.add(stEle);
        });
    }
}