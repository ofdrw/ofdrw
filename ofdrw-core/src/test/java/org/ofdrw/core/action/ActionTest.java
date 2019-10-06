package org.ofdrw.core.action;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.action.actionType.actionGoto.Goto;
import org.ofdrw.core.action.actionType.actionGoto.GotoTest;
import org.ofdrw.core.graph.CT_RegionTest;


import static org.junit.jupiter.api.Assertions.*;

public class ActionTest {

    public static Action actionCase() {
        return new Action()
                .setEvent(EventType.CLICK)
                .setRegion(CT_RegionTest.regionCase())
                .setAction(GotoTest.gotoCase());
    }

    @Test
    public void gen(){
        TestTool.genXml("Action", actionCase());
    }
}