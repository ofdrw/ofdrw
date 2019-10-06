package org.ofdrw.core.action.actionType.actionGoto;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_RefID;

import static org.junit.jupiter.api.Assertions.*;

public class GotoTest {

    public static Goto gotoCase() {
        return new Goto()
                .setDest(new CT_Dest()
                        .setType(DestType.FitR)
                        .setPageID(ST_RefID.getInstance("1"))
                        .setLeft(0)
                        .setRight(100)
                        .setTop(0)
                        .setBottom(100)
                        .setZoom(0.54));
    }

    @Test
    public void gen(){
        TestTool.genXml("Goto", gotoCase());

        TestTool.genXml("Goto2", document -> {
            Goto goto2 = new Goto("FirstPage");
            document.add(goto2);
        });
    }
}