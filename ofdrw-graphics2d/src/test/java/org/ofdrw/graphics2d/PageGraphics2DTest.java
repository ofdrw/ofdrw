package org.ofdrw.graphics2d;

import org.junit.jupiter.api.Test;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class PageGraphics2DTest {

    /**
     * 轴向渐变填充
     */
    @Test
    void linearGradientPaint() throws Exception {
        final Path dst = Paths.get("target/linearGradientPaint.ofd");
        try (GraphicsDocument doc = new GraphicsDocument(dst)) {
            PageGraphics2D g = doc.newPage(null);

            Point2D start = new Point2D.Float(0, 0);
            Point2D end = new Point2D.Float(50, 50);
            float[] dist = {0.0f, 0.2f, 1.0f};
            Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
            LinearGradientPaint p =
                    new LinearGradientPaint(start, end, dist, colors);
            g.setPaint(p);
            g.fillRect(10, 10, 60, 60);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 矩形描边
     */

    @Test
    void drawRoundRect() throws Exception {
        final Path dst = Paths.get("target/drawRoundRect.ofd");
        try (GraphicsDocument doc = new GraphicsDocument(dst)) {
            PageGraphics2D g = doc.newPage(null);
            g.drawRoundRect(10, 10, 20, 40, 5, 5);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 自定义图形和描边样式
     */
    @Test
    void draw() throws Exception {
        final Path dst = Paths.get("target/draw.ofd");
        try (GraphicsDocument doc = new GraphicsDocument(dst)) {
            PageGraphics2D g = doc.newPage(null);
            int[] x2Points = {0, 100, 0, 100};
            int[] y2Points = {0, 50, 50, 0};
            GeneralPath polyline = new GeneralPath(GeneralPath.WIND_EVEN_ODD, x2Points.length);
            polyline.moveTo(x2Points[0], y2Points[0]);
            for (int index = 1; index < x2Points.length; index++) {
                polyline.lineTo(x2Points[index], y2Points[index]);
            }
            polyline.closePath();

            g.draw(polyline);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 描边
     */
    @Test
    void drawStroke() throws Exception {
        final Path dst = Paths.get("target/drawStroke.ofd");
        try (GraphicsDocument doc = new GraphicsDocument(dst)) {
            PageGraphics2D g2 = doc.newPage(500, 500);
            final BasicStroke dashed =
                    new BasicStroke(1.0f,
                            BasicStroke.CAP_BUTT,
                            BasicStroke.JOIN_MITER,
                            10.0f, new float[]{10.0f}, 0.0f);
            g2.setStroke(dashed);
            g2.draw(new RoundRectangle2D.Double(10, 10, 400, 100, 10, 10));
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}