package org.ofdrw.core.pageDescription.color.color.radialShd;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.CT_RadialShd;
import org.ofdrw.core.pageDescription.color.color.Extend;
import org.ofdrw.core.pageDescription.color.color.Segment;

public class CT_RadialShdTest {

    public static CT_RadialShd radialShdCase(){
        return new CT_RadialShd()
                .setStartPoint(ST_Pos.getInstance(40, 70))
                .setStartRadius(10d)
                .setEndPoint(ST_Pos.getInstance(140, 70))
                .setEndRadius(50d)
                .addSegment(new Segment(CT_Color.rgb(255, 255, 0)))
                .addSegment(new Segment(CT_Color.rgb(0, 0, 255)));
    }

    public static CT_RadialShd radialShdCase2(){
        return new CT_RadialShd()
                .setStartPoint(ST_Pos.getInstance(40, 70))
                .setStartRadius(10d)
                .setEndPoint(ST_Pos.getInstance(140, 70))
                .setEndRadius(50d)
                .setExtend(Extend._1)
                .addSegment(new Segment(CT_Color.rgb(255, 255, 0)).setPosition(0d))
                .addSegment(new Segment(CT_Color.rgb(0, 0, 255)).setPosition(1d));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_Radial", radialShdCase());
    }
}