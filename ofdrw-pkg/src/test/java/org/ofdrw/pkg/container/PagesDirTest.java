package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.content.PageContent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class PagesDirTest {

    @Test
    void collect() throws IOException {
        Path p = Paths.get("target/Pages");
        PagesDir ps = new PagesDir(p);
        ps.newPageDir().setContent(PageContent.page());
        ps.flush();
        System.out.println(p.toAbsolutePath().toString());
    }
}