package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class ResDirTest {

    @Test
    void collect() throws IOException {
        Path path = Paths.get("src/test/resources", "NotoSerifCJKsc-Medium.otf");
        Path p = Paths.get("target/ResTest");
        ResDir res = new ResDir(p)
                .add(path);
        res.flush();
        System.out.println("生成目录位置: " +res.getSysAbsPath());
    }
}