package org.ofdrw.graphics2d;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

class OFDGraphicsDocumentTest {

    @Test
    void newPage() throws Exception {
        final Path dst = Paths.get("target/2d.ofd");
        try (OFDGraphicsDocument doc = new OFDGraphicsDocument(dst)) {
            OFDPageGraphics2D g = doc.newPage(null);

        }
        System.out.println(">> "+ dst.toAbsolutePath());
    }
}