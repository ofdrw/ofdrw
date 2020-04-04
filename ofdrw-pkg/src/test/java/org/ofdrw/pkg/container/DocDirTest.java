package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.content.DocumentContent;
import org.ofdrw.pkg.container.content.PageContent;
import org.ofdrw.pkg.container.content.PublicResContent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

class DocDirTest {
    @Test
    void collect() throws IOException {
        Path p = Paths.get("target/Doc_0");
        Path font = Paths.get("src/test/resources", "NotoSerifCJKsc-Medium.otf");
        DocDir docDir = new DocDir(p)
                .addResource(font)
                .setPublicRes(PublicResContent.res())
                .setDocument(DocumentContent.doc());
        PagesDir ps = docDir.obtainPages();
        ps.newPageDir().setContent(PageContent.page());
        docDir.flush();
    }
    @Test
    void err(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Path p = Paths.get("target/Doc_AAA");
            DocDir docDir = new DocDir(p);
        });
    }
}