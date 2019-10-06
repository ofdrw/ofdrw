package org.ofdrw.core.action;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.action.actionType.actionGoto.GotoTest;
import org.ofdrw.core.graph.CT_RegionTest;

public class CTActionTest {

    public static CT_Action actionCase() {
        return new CT_Action()
                .setEvent(EventType.CLICK)
                .setRegion(CT_RegionTest.regionCase())
                .setAction(GotoTest.gotoCase());
    }

    @Test
    public void gen(){
        TestTool.genXml("Action", actionCase());
    }
}