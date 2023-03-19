package org.ofdrw.graphics2d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestG2dAPI {
    public static void main(String[] args) throws IOException {
        BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
//        g.setPaint(Color.RED);
//
//
//        int[] xpoints = {25, 145, 25, 145, 25};
//        int[] ypoints = {25, 25, 145, 145, 25};
//        int npoints = 5;
//        g.fillPolygon(xpoints, ypoints, npoints);


//        Path file = Paths.get("ofdrw-graphics2d/src/test/resources", "empty.png");
//        BufferedImage img1 = ImageIO.read(file.toFile());
//
////        g.drawImage(img1, 10, 10,400,400, Color.red, null);
//        g.drawImage(img1, 0, 0, 200, 200, 200, 200, 400, 400, Color.RED,null);
//
//

        g.setColor(Color.blue);
        g.fillRect(0, 0, 1000, 1000);

//        g.setColor(Color.RED);
//        g.scale(2, 2);
//        g.drawRect(0, 0, 10, 10);
//        g.scale(2, 2);
//        g.drawRect(0, 0, 10, 10);
//        g.fillRoundRect(150, 50, 100, 100, 50, 25);
//        g.shear(10 * Math.PI / 180d, 10 * Math.PI / 180d);
//        g.translate(100, 100);
//        g.rotate(45 * Math.PI / 180d);
//        g.fillRect(0, 0, 50, 50);



        g.setColor(Color.RED);
        g.translate(100,100);
        g.clipRect(0,0, 100,100);
        g.fillRect(0, 0, 100, 100);
        g.rotate(45 * Math.PI / 180d);
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, 100, 100);
//        g.scale(2, 2);
//        g.drawRect(0, 0, 10, 10);
//        g.fillRoundRect(150, 50, 100, 100, 50, 25);
//        g.shear(10 * Math.PI / 180d, 10 * Math.PI / 180d);
//        g.translate(100, 100);
//        g.rotate(45 * Math.PI / 180d);
//        g.fillRect(0, 0, 50, 50);


//        g.rotate(10 * Math.PI / 180d);
//        g.transform(null);

//        g.translate(10,10);
//        g.setTransform(new AffineTransform(
//                0.83, 0.83,
//                -0.58, 0.58,
//                117.45, 117.45));
//        AffineTransform tx = g.getTransform();
//
//        String var = String.format("%.2f %.2f %.2f %.2f %.2f %.2f",
//                tx.getScaleX(), tx.getShearY(),
//                tx.getShearX(), tx.getScaleY(),
//                tx.getTranslateX(), tx.getTranslateY()
//        );
//        System.out.println(var);


//        g.translate(100,100);
//        g.setClip(new Rectangle2D.Double(0, 0, 200, 200));
//        g.fillArc(0,0, 400, 400, 0, 360);
//        Path file = Paths.get("ofdrw-graphics2d/src/test/resources", "eg_tulip.jpg");
//        BufferedImage img1 = ImageIO.read(file.toFile());
//        g.drawImage(img1, 0, 0,  null);

//        g.drawString("你好OFD R&W Hello ", 40, 120);



//        Color[] colors = { Color.red, Color.green, Color.blue };
//        float[] dist = {0.0f, 0.5f, 1.0f };
//        Point2D center = new Point2D.Float(0.5f * 500, 0.5f * 500);
//
//        RadialGradientPaint p =
//                new RadialGradientPaint(center, 0.5f * 500, dist, colors);
//        g.setPaint(p);
//        g.fillRect(0, 0, 500, 500);

        Path path = Paths.get("ofdrw-graphics2d/target/test.png");
        ImageIO.write(image, "png", path.toFile());
        System.out.println(">> " + path.toAbsolutePath());
    }
}
