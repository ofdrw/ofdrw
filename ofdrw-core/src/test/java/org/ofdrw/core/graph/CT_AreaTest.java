package org.ofdrw.core.graph;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Pos;
import org.ofdrw.core.graph.tight.method.*;
import org.ofdrw.core.graph.tight.CT_Area;

public class CT_AreaTest {

    public static CT_Area areaCase() {
        return new CT_Area()
                .setStart(new ST_Pos(0, 0))
                .next(new Move(250, 0))
                .next(new Line(250, 300))
                .next(new QuadraticBezier(
                        new ST_Pos(375, 150),
                        new ST_Pos(250, 600)
                ))
                .next(new Move(250, 300))
                .next(new CubicBezier(
                        new ST_Pos(190, 150),
                        new ST_Pos(130, 150),
                        new ST_Pos(60, 300)
                ))
                .next(new Move(250, 300))
                .next(new Arc()
                        .setSweepDirection(true)
                        .setLargeArc(false)
                        .setRotationAngle(180)
                        .setEllipseSize(100)
                        .setEndPoint(500, 300))
                .next(new Close());

    }

    @Test
    public void gen() {
        TestTool.genXml("Area", areaCase());
    }
}