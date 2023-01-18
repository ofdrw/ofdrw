package org.ofdrw.graphics2d;

import org.junit.jupiter.api.Test;

import java.awt.geom.Line2D;
import java.nio.file.Path;
import java.nio.file.Paths;

class GraphicsDocumentTest {

    @Test
    void newPage() throws Exception {
        final Path dst = Paths.get("target/2d.ofd");
        try (GraphicsDocument doc = new GraphicsDocument(dst)) {
            PageGraphics2D g = doc.newPage(null);

        }
        System.out.println(">> "+ dst.toAbsolutePath());
    }
}