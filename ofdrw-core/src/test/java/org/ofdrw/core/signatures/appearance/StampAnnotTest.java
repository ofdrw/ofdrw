package org.ofdrw.core.signatures.appearance;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;

import static org.junit.jupiter.api.Assertions.*;

public class StampAnnotTest {
    public static StampAnnot stampAnnotCase() {
        return new StampAnnot()
                .setID("s004")
                .setBoundary(new ST_Box(0, 0, 50, 50))
                .setPageRef(ST_RefID.getInstance("1007"));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("StampAnnot", stampAnnotCase());
    }
}