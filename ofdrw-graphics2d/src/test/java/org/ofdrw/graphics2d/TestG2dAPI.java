package org.ofdrw.graphics2d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class TestG2dAPI {
    public static void main(String[] args) throws IOException {
        BufferedImage image = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setPaint(Color.RED);


        int[] xpoints = {25, 145, 25, 145, 25};
        int[] ypoints = {25, 25, 145, 145, 25};
        int npoints = 5;
        g.fillPolygon(xpoints, ypoints, npoints);


        ImageIO.write(image, "png", new File("test.png"));
    }
}
