package org.ofdrw.core.graph.pathObj;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.drawParam.LineCapType;
import org.ofdrw.core.pageDescription.drawParam.LineJoinType;

import static org.junit.jupiter.api.Assertions.*;

public class CT_PathTest {

    public static CT_Path pathCase() {
        return new CT_Path()
                .setStroke(true)
                .setFill(false)
                .setRule(Rule.NonZero)
                .setStrokeColor(CT_Color.rgb(0,0,0))
                .setAbbreviatedData(AbbreviatedDataTest.abbreviatedDataCase())
                .setBoundary(new ST_Box(47, 47, 153, 203))
                .setGraphicName("FirstPath")
                .setVisible(true)
                .setLineWidth(2d)
                .setCap(LineCapType.Round)
                .setJoin(LineJoinType.Round)
                .setAlpha(255);
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("Path", pathCase());
    }
}