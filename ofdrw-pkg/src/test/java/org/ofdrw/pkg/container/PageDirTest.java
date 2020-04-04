package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AssertionsKt;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.content.PageContent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

class PageDirTest {
    @Test
    void collect() throws IOException {
        Path p = Paths.get("target/Page_0");
        PageDir pageDir = new PageDir(p)
                .setContent(PageContent.page());
        pageDir.flush();
    }

    @Test
    void err(){
        Path p = Paths.get("target/Page_AA");
        Assertions.assertThrows( IllegalArgumentException.class, () -> {
            PageDir pageDir = new PageDir(p)
                    .setContent(PageContent.page());
            pageDir.flush();
        });
        Assertions.assertTrue(Files.notExists(p));
    }
}