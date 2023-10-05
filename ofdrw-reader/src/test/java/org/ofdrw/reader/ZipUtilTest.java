package org.ofdrw.reader;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class ZipUtilTest {

    @Test
    void unZipFiles() throws Exception {
        Path src = Paths.get("src/test/resources/DOC_0.zip");
        Path dst = Paths.get("target", "DOC_0_copy");
        ZipUtil.setDefaultCharset("GBK");
        ZipUtil.unZipFiles(src.toFile(), dst.toAbsolutePath().getFileName().toString());
    }
}