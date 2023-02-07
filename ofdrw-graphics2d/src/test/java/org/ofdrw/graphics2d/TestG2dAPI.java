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

        g.translate(50, 50);
        g.setColor(Color.red);
        g.fillRect(0, 0, 20, 20);


        ImageIO.write(image, "png", new File("test.png"));
        System.out.println(">> "+ Paths.get("test.png").toAbsolutePath());
    }
}
