package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTest {


    @Test
    public void pathCat() {
        Path c = Paths.get("foo/", "bar/", "/Content.xml");
        System.out.println(c.toAbsolutePath().toString());
//        Assert.assertTrue();
    }

    @Test
    public void CreateNew() throws Exception {
        Path c = Paths.get("target", "foo", "bar", "text.txt");
        // 清理上次测试遗留
        Files.deleteIfExists(c);
        Files.createDirectories(c.getParent());
        Files.createFile(c);
    }
}
