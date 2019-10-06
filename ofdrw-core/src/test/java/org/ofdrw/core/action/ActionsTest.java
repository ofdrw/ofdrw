package org.ofdrw.core.action;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class ActionsTest {
    public static Actions actionsCase() {
        return new Actions()
                .addAction(CTActionTest.actionCase())
                .addAction(CTActionTest.actionCase());
    }

    @Test
    public void gen(){
        TestTool.genXml("Actions", actionsCase());
    }
}