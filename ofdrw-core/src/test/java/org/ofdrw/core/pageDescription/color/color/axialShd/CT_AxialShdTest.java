package org.ofdrw.core.pageDescription.color.color.axialShd;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.pageDescription.color.color.CT_Color;
import org.ofdrw.core.pageDescription.color.color.MapType;
import org.ofdrw.core.pageDescription.color.color.Segment;

public class CT_AxialShdTest {
    public static CT_AxialShd axialShdCase() {
        CT_AxialShd res = new CT_AxialShd();
        res.setObjID(new ST_ID("10007"));
        return res.setStartPoint(ST_Pos.getInstance(0, 0))
                .setEndPoint(ST_Pos.getInstance(140d, 0))
                .addSegment(new Segment(CT_Color.rgb(255, 255, 0)))
                .addSegment(new Segment(CT_Color.rgb(0, 0, 255)));
    }

    public static CT_AxialShd axialShdReflectCase() {
        CT_AxialShd res = new CT_AxialShd();
        res.setObjID(new ST_ID("10007"));
        return res.setStartPoint(ST_Pos.getInstance(0, 0))
                .setEndPoint(ST_Pos.getInstance(30d, 0))
                .setMapType(MapType.Reflect)
                .setMapUnit(30d)
                .addSegment(new Segment(CT_Color.rgb(255, 255, 0)))
                .addSegment(new Segment(CT_Color.rgb(0, 0, 255)));
    }

    public static CT_AxialShd axialShdRepeatCase() {
        CT_AxialShd res = new CT_AxialShd();
        res.setObjID(new ST_ID("10007"));
        return res.setStartPoint(ST_Pos.getInstance(0, 0))
                .setEndPoint(ST_Pos.getInstance(140d, 0))
                .setMapType(MapType.Repeat)
                .setMapUnit(30d)
                .addSegment(new Segment(CT_Color.rgb(255, 255, 0)))
                .addSegment(new Segment(CT_Color.rgb(0, 0, 255)));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("CT_AxialShd", axialShdCase());
    }
}