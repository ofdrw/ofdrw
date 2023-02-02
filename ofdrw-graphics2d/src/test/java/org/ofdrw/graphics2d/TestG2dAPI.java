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
        g.drawArc (10, 10, 200, 200,50,50);


        ImageIO.write(image, "png", new File("test.png"));
    }
}
