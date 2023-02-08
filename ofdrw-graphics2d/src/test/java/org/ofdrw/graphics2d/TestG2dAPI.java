package org.ofdrw.graphics2d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TestG2dAPI {
    public static void main(String[] args) throws IOException {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_ARGB);
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
        g.fillRect(0, 0, 200, 200);

        g.setColor(Color.RED);
//        g.scale(2, 2);
//        g.drawRect(0, 0, 10, 10);
//        g.scale(2, 2);
//        g.drawRect(0, 0, 10, 10);
//        g.fillRoundRect(150, 50, 100, 100, 50, 25);
        g.shear(45 * Math.PI / 180d, 45 * Math.PI / 180d);
        g.fillRect(0, 0, 50, 50);

        Path path = Paths.get("ofdrw-graphics2d/target/test.png");
        ImageIO.write(image, "png", path.toFile());
        System.out.println(">> " + path.toAbsolutePath());
    }
}
