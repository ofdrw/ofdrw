package org.ofdrw.pkg.container;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-04-18 14:23:23
 */
class SignsDirTest {

    @Test
    void newSignDir() throws IOException {
        Path root = Paths.get("target/Signs");
        if (Files.exists(root)) {
            FileUtils.deleteDirectory(root.toFile());
        }
        Files.createDirectory(root);
        SignsDir signsDir = new SignsDir(root);
        SignDir signDir = signsDir.newSignDir();

        assertEquals("Sign_0", signDir.getContainerName());

    }
}