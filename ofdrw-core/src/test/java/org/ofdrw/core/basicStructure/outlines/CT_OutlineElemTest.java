package org.ofdrw.core.basicStructure.outlines;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.action.ActionsTest;

import static org.junit.jupiter.api.Assertions.*;

class CT_OutlineElemTest {

    public static CT_OutlineElem outlineElemCase() {
        return new CT_OutlineElem("TestOutlineElem")
                .setCount(2)
                .setExpanded(true)
                .setActions(ActionsTest.actionsCase())
                .addOutlineElem(new CT_OutlineElem("SubOutlineElem1"))
                .addOutlineElem(new CT_OutlineElem("SubOutlineElem2"));
    }

    @Test
    public void gen(){
        TestTool.genXml("OutlineElem", outlineElemCase());
    }
}