package org.ofdrw.pkg.container;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.container.content.DocumentContent;
import org.ofdrw.pkg.container.content.OFDContent;
import org.ofdrw.pkg.container.content.PageContent;
import org.ofdrw.pkg.container.content.PublicResContent;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;


class OFDDirTest {
    Path p = Paths.get("target/TestOFD");
    Path font = Paths.get("src/test/resources", "NotoSerifCJKsc-Medium.otf");

    OFDDir build() throws IOException {
        FileUtils.deleteDirectory(p.toFile());
        OFDDir ofdDir = new OFDDir(p)
                .setOfd(OFDContent.ofd());
        final DocDir docDir = ofdDir.newDoc()
                .addResource(font)
                .setPublicRes(PublicResContent.res())
                .setDocument(DocumentContent.doc());
        PagesDir ps = docDir.obtainPages();
        ps.newPageDir().setContent(PageContent.page());
        return ofdDir;
    }


    @Test
    void collect() throws IOException {
        build().flush();
        System.out.println(p.toAbsolutePath());
    }

    @Test
    void jar() throws IOException {
        final OFDDir ofdDir = build();
        Path res = Paths.get("target/helloworld.ofd");
        ofdDir.jar(res);
        ofdDir.clean();
        Assertions.assertTrue(Files.notExists(p));
    }
}