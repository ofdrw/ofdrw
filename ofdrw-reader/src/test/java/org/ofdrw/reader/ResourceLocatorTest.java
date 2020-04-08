package org.ofdrw.reader;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.OFDDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-08 21:48:36
 */
class ResourceLocatorTest {
    private Path src = Paths.get("src/test/resources/helloworld.ofd");


    @Test
    void testCd() throws IOException {
        try (OFDReader reader = new OFDReader(src)) {
            OFDDir ofdDir = reader.getOFDDir();

            ResourceLocator rl = new ResourceLocator(ofdDir);
            rl.cd("/Doc_0/Pages/");
            assertEquals("/Doc_0/Pages/", rl.pwd());
            rl.restWd();
            assertEquals("/", rl.pwd());

            rl.cd("../");
            assertEquals("/", rl.pwd());

            rl.cd("./Doc_0/../temo");
            assertEquals("/", rl.pwd());
        }
    }
}