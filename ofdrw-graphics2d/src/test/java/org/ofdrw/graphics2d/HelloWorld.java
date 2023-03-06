package org.ofdrw.graphics2d;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HelloWorld {

    public static void main(String[] args) throws IOException {
        final Path dst = Paths.get("HelloWorld.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);
            g.setColor(Color.BLACK);
            g.setFont(new Font("宋体", Font.PLAIN, 7));
            g.drawString("你好OFD Reader & Writer Graphics-2D", 40, 40);
        }
        System.out.println(">> " + dst.toAbsolutePath());
    }
}
