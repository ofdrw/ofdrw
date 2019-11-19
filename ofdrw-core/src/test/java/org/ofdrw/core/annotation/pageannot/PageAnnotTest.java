package org.ofdrw.core.annotation.pageannot;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class PageAnnotTest {
    public static PageAnnot pageAnnotCase(){
        return new PageAnnot()
                .addAnnot(AnnotTest.annotCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("PageAnnot", pageAnnotCase());
    }
}