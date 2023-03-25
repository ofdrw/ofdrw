package org.ofdrw.graphics2d;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.tool.ElemCup;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OFDPageGraphics2DTest {

    /**
     * 轴向渐变填充
     */
    @Test
    void linearGradientPaint() throws Exception {
        final Path dst = Paths.get("target/linearGradientPaint.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);

            Point2D start = new Point2D.Float(0, 0);
            Point2D end = new Point2D.Float(50, 50);
            float[] dist = {0.0f, 0.2f, 1.0f};
            Color[] colors = {Color.RED, Color.WHITE, Color.BLUE};
            LinearGradientPaint p =
                    new LinearGradientPaint(start, end, dist, colors);
            g.setPaint(p);
            g.fillRect(0, 0, 50, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 矩形描边
     */

    @Test
    void drawRoundRect() throws Exception {
        final Path dst = Paths.get("target/drawRoundRect.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.drawRoundRect(10, 10, 20, 40, 5, 5);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 填充圆角矩形
     */
    @Test
    void fillRoundRect() throws Exception {
        final Path dst = Paths.get("target/fillRoundRect.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.fillRoundRect(150, 50, 100, 100, 50, 25);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    /**
     * 自定义图形和描边样式
     */
    @Test
    void draw() throws Exception {
        final Path dst = Paths.get("target/draw.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
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
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g2 = doc.newPage(500, 500);
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

    /**
     * 圆弧填充
     */
    @Test
    void drawArc() throws Exception {
        final Path dst = Paths.get("target/drawArc.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            g.setPaint(Color.RED);
            g.fillArc(20, 20, 200, 200, 200, 200);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 绘制圆弧
     */
    @Test
    void fillArc() throws Exception {
        final Path dst = Paths.get("target/fillArc.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            g.fillArc(10, 10, 200, 200, 50, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 清空指定区域
     */
    @Test
    void clearRect() throws Exception {
        final Path dst = Paths.get("target/clearRect.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            g.fillArc(0, 0, 100, 100, 0, 360);
            g.clearRect(30, 30, 10, 10);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 填充椭圆
     */
    @Test
    void fillOval() throws Exception {
        final Path dst = Paths.get("target/fillOval.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            g.fillOval(25, 25, 120, 60);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 描边椭圆
     */
    @Test
    void drawOval() throws Exception {
        final Path dst = Paths.get("target/drawOval.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            g.drawOval(25, 25, 120, 60);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 折线
     */
    @Test
    void drawPolyline() throws Exception {
        final Path dst = Paths.get("target/drawPolyline.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            int[] xs = {25, 75, 125, 85, 125, 75, 25, 65};
            int[] ys = {50, 90, 50, 100, 150, 110, 150, 100};
            g.drawPolyline(xs, ys, 8);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 自定义多边形描边
     */
    @Test
    void drawPolygon() throws Exception {
        final Path dst = Paths.get("target/drawPolygon.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            int[] xpoints = {25, 145, 25, 145, 25};
            int[] ypoints = {25, 25, 145, 145, 25};
            int npoints = 5;

            g.drawPolygon(xpoints, ypoints, npoints);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 自定义多边形填充
     */
    @Test
    void fillPolygon() throws Exception {
        final Path dst = Paths.get("target/fillPolygon.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setPaint(Color.RED);
            int[] xpoints = {25, 145, 25, 145, 25};
            int[] ypoints = {25, 25, 145, 145, 25};
            int npoints = 5;

            g.fillPolygon(xpoints, ypoints, npoints);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    @Test
    void drawImage() throws Exception {
        final Path dst = Paths.get("target/drawImage.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
            BufferedImage img1 = ImageIO.read(file.toFile());

            int width = img1.getWidth(null);
            int height = img1.getHeight(null);
            g.drawImage(img1, 10, 10, width, height, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void drawImage2() throws Exception {
        final Path dst = Paths.get("target/drawImage2.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, 10, 10, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void drawImageAffineTransform() throws Exception {
        final Path dst = Paths.get("target/drawImageAffineTransform.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, null, 10, 10);
            g.setPaint(Color.RED);
            g.fillRect(0, 0, 30, 30);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    /**
     * 背景颜色填充透明图片
     */
    @Test
    void drawImageBackground() throws Exception {
        final Path dst = Paths.get("target/drawImageBackground.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "empty.png");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, 10, 10, 400, 400, Color.red, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void drawImageBackground2() throws Exception {
        final Path dst = Paths.get("target/drawImageBackground2.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "empty.png");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, 10, 10, Color.red, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 绘制部分图片到页面
     */
    @Test
    void drawImagePiece() throws Exception {
        final Path dst = Paths.get("target/drawImagePiece.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "empty.png");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, 0, 0, 200, 200, 200, 200, 400, 400, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void drawImagePieceColor() throws Exception {
        final Path dst = Paths.get("target/drawImagePieceColor.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "empty.png");
            BufferedImage img1 = ImageIO.read(file.toFile());

            g.drawImage(img1, 0, 0, 200, 200, 200, 200, 400, 400, Color.RED, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 平移
     */
    @Test
    void translate() throws Exception {
        final Path dst = Paths.get("target/translate.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.translate(50, 50);
            g.setColor(Color.red);
            g.fillRect(0, 0, 20, 20);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 旋转
     */
    @Test
    void rotate() throws Exception {
        final Path dst = Paths.get("target/rotate.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.rotate(45 * Math.PI / 180d);
            g.fillRect(0, 0, 20, 20);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 饶某点旋转画布
     */
    @Test
    void rotate2() throws Exception {
        final Path dst = Paths.get("target/rotate2.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.rotate(45 * Math.PI / 180d, 100, 100);
            g.fillRect(100, 100, 50, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void translateRotate() throws Exception {
        final Path dst = Paths.get("target/translateRotate.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.translate(100, 100);
            g.rotate(45 * Math.PI / 180d);
            g.fillRect(0, 0, 50, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 缩放
     */
    @Test
    void scale() throws Exception {
        final Path dst = Paths.get("target/scale.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.scale(2, 2);
            g.drawRect(0, 0, 10, 10);
            g.scale(2, 2);
            g.drawRect(0, 0, 10, 10);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    /**
     * 切变
     */
    @Test
    void shear() throws Exception {
        final Path dst = Paths.get("target/shear.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.red);
            g.shear(10 * Math.PI / 180d, 10 * Math.PI / 180d);
            g.fillRect(0, 0, 50, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 获取但前变换矩阵
     */
    @Test
    void getTransform() throws Exception {
        final Path dst = Paths.get("target/getTransform.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);
            g.setColor(Color.RED);

            g.shear(10 * Math.PI / 180d, 10 * Math.PI / 180d);
            g.translate(100, 100);
            g.rotate(45 * Math.PI / 180d);
            g.fillRect(0, 0, 50, 50);

            AffineTransform tx = g.getTransform();
            String actual = String.format("%.2f %.2f %.2f %.2f %.2f %.2f",
                    tx.getScaleX(), tx.getShearY(),
                    tx.getShearX(), tx.getScaleY(),
                    tx.getTranslateX(), tx.getTranslateY()
            );
            String expect = "0.83 0.83 -0.58 0.58 117.45 117.45";
            System.out.println(actual);
            assertEquals(expect, actual);
        }
    }

    /**
     * 应用变换矩阵
     */
    @Test
    void transform() throws Exception {
        final Path dst = Paths.get("target/transform.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(200, 200);

            g.translate(10, 10);
            g.transform(new AffineTransform(
                    0.83, 0.83,
                    -0.58, 0.58,
                    117.45, 117.45));
            AffineTransform tx = g.getTransform();
            String actual = String.format("%.2f %.2f %.2f %.2f %.2f %.2f",
                    tx.getScaleX(), tx.getShearY(),
                    tx.getShearX(), tx.getScaleY(),
                    tx.getTranslateX(), tx.getTranslateY()
            );
            String expect = "0.83 0.83 -0.58 0.58 127.45 127.45";
            System.out.println(actual);
            assertEquals(expect, actual);
        }
    }

    /**
     * 裁剪区域
     */
    @Test
    void setClip() throws Exception {
        final Path dst = Paths.get("target/clip.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
//            Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
//            g.translate(100,100);
            g.setPaint(Color.RED);
            g.translate(100, 100);
            g.setClip(new Rectangle2D.Double(0, 0, 200, 200));
            g.fillArc(0, 0, 400, 400, 0, 360);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 文字绘制测试
     */
    @Test
    void drawString() throws Exception {
        final Path dst = Paths.get("target/drawString.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);

            g.setPaint(Color.RED);
//            g.setFont(new Font("src/main/resources/simsun.ttf", Font.PLAIN, 15));
            g.drawString("你好OFD R&W Hello ", 40, 120);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 径向渐变测试
     */
    @Test
    void setPaintRadialGradientPaint() throws Exception {
        final Path dst = Paths.get("target/setPaintRadialGradientPaint.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);

            Color[] colors = {Color.red, Color.green, Color.blue};
            float[] dist = {0.0f, 0.5f, 1.0f};
            Point2D center = new Point2D.Float(0.5f * 500, 0.5f * 500);

            RadialGradientPaint p =
                    new RadialGradientPaint(center, 0.5f * 500, dist, colors);
            g.setPaint(p);
            g.fillRect(0, 0, 500, 500);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 裁剪区域的变换 与 图元变换
     */
    @Test
    void clipAndCTM() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        final Path dst = Paths.get("target/clipAndCTM.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);


            g.setColor(Color.WHITE);
            g.fillRect(0, 0, 500, 500);

            g.setColor(Color.RED);
            g.translate(100, 100);
            g.clipRect(0, 0, 100, 100);
            g.fillRect(0, 0, 100, 100);

            g.rotate(45 * Math.PI / 180d);
            g.setColor(Color.YELLOW);
            g.fillRect(0, 0, 100, 100);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    /**
     * 裁剪区域的变换 与 图元变换
     */
    @Test
    void clips() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        final Path dst = Paths.get("target/clips.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);

            g.setColor(Color.RED);
            g.translate(100, 100);
            g.fillRect(0, 0, 100, 100);
            g.clipRect(0, 0, 100, 100);

            g.setColor(Color.BLUE);
            g.translate(-50, -50);
            g.fillRect(0, 0, 100, 100);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    /**
     * 裁剪区域的变换 与 图元变换
     */
    @Test
    void complexCTMSetAndReset() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        final Path dst = Paths.get("target/complexCTMSetAndReset.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);

            g.rotate(90 * Math.PI / 180d);
            g.translate(100, 0);
            g.setColor(Color.RED);
            g.fillRect(0, -50, 100, 50);

            g.setTransform(new AffineTransform());
            g.translate(100, 0);
            g.rotate(90 * Math.PI / 180d);
            g.setColor(Color.BLUE);
            g.fillRect(0, -50, 100, 50);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void drawImageWithCTM() throws Exception {
        final Path dst = Paths.get("target/drawImageWithCTM.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
            BufferedImage img1 = ImageIO.read(file.toFile());
            g.translate(400, 0);
            g.rotate(90 * Math.PI / 180d);
            g.drawImage(img1, 10, 10, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }


    @Test
    void clipImg() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        final Path dst = Paths.get("target/clipImg.ofd");
        final Path file = Paths.get("src/test/resources", "eg_tulip.jpg");
        BufferedImage img1 = ImageIO.read(file.toFile());

        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);
            g.setClip(100, 100, 100, 100);
            g.rotate(10 * Math.PI / 180d);
            g.drawImage(img1, 100, 100, null);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }



    @Test
    void clip() throws Exception {
        ElemCup.ENABLE_DEBUG_PRINT = true;
        final Path dst = Paths.get("target/clipLarge.ofd");

        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(500, 500);

            // 处于裁剪区内部
            g.setClip(0, 0, 300,300);
            g.setColor(Color.BLUE);
            g.fillRect(10, 10, 50, 50);

            // 与裁剪区相交
            g.setClip(null);
            g.setClip(0, 100, 100,100);
            g.setColor(Color.RED);
            g.fillRect(50, 150, 200, 200);

            // 与裁剪区不相交
            g.setClip(null);
            g.setClip(0, 300, 100,100);
            g.setColor(Color.RED);
            g.fillRect(400, 400, 50, 50);

        }
        System.out.println(">> " + dst.toAbsolutePath());
    }

    @Test
    void intersects() {
        Rectangle2D.Double large = new Rectangle2D.Double(0, 0, 200, 200);
        Rectangle2D.Double small = new Rectangle2D.Double(50, 50, 50, 50);
        Rectangle2D.Double a2 = new Rectangle2D.Double(100, 0, 200, 200);
        Rectangle2D.Double remote = new Rectangle2D.Double(1000, 1000, 10, 10);

//        System.out.println(large.intersects(remote));
        System.out.println(large.intersects(remote));
//        System.out.println(large.intersects(small));
//        System.out.println(large.contains(small));
//        System.out.println(large.intersects(a2));
    }

}