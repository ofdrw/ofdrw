package org.ofdrw.crypto;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-07-19 19:19:41
 */
class ContainerPathTest {

    @Test
    void createEncryptedFile() throws IOException {

        Path src = Paths.get("target/Doc_0/Page_0/Content.xml");
        Files.createDirectories(src.getParent());
        try (OutputStream out = Files.newOutputStream(src);) {
            out.write("Hello".getBytes(StandardCharsets.UTF_8));
        }

        ContainerPath cp = new ContainerPath("/Doc_0/Page_0/Content.xml", src);
        final ContainerPath actual = cp.createEncryptedFile();
        Path encryptedFile = actual.getAbs();
        assertTrue(Files.exists(encryptedFile));
        assertEquals("/Doc_0/Page_0/content.dat", actual.getPath());
        System.out.println(actual.getPath());
        System.out.println(encryptedFile.toAbsolutePath());
    }


    @Test
    public void resolve() throws Exception {
        Path src = Paths.get("");
        final Path resolve = src.resolve("not/exist/dir");
        System.out.println(Files.notExists(resolve));
        System.out.println(resolve.toAbsolutePath());
    }
}