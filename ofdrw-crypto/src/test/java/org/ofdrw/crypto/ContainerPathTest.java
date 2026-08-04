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
        // 清理上次测试遗留的 .dat 文件，避免递增序号
        Files.createDirectories(src.getParent());
        Files.deleteIfExists(src.getParent().resolve("content.dat"));
        try (OutputStream out = Files.newOutputStream(src);) {
            out.write("Hello".getBytes(StandardCharsets.UTF_8));
        }

        ContainerPath cp = new ContainerPath("/Doc_0/Page_0/Content.xml", src);
        final ContainerPath actual = cp.createEncryptedFile();
        Path encryptedFile = actual.getAbs();
        assertTrue(Files.exists(encryptedFile));
        assertEquals("/Doc_0/Page_0/content.dat", actual.getPath());
        // 清理遗留文件
        Files.deleteIfExists(encryptedFile);
        Files.deleteIfExists(src);
    }


    @Test
    public void resolve() throws Exception {
        Path src = Paths.get("");
        final Path resolve = src.resolve("not/exist/dir");
        System.out.println(Files.notExists(resolve));
        System.out.println(resolve.toAbsolutePath());
    }
}