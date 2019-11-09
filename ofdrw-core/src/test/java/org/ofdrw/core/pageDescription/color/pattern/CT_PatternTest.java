package org.ofdrw.core.pageDescription.color.pattern;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class CT_PatternTest {

    public static CT_Pattern patternCase(){
        return new CT_Pattern()
                .setWidth(50d)
                .setHeight(50d)
                .setCellContent(CellContentTest.cellContentCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Pattern", patternCase());
    }
}