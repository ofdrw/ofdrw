package org.ofdrw.core.structure;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import javax.management.openmbean.SimpleType;

import static org.junit.jupiter.api.Assertions.*;

class OFDElementTest {


    @Test
    void addOFDEntity() {

        TestTool.genXml("AddST", doc -> {
            SimpleTypeElement stEle = new SimpleTypeElement("Name", "Value");
            stEle.addOFDEntity("SubName", "value2");
            doc.add(stEle);
        });
    }
}