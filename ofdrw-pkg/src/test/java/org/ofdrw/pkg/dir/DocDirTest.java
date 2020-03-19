package org.ofdrw.pkg.dir;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.dir.content.DocumentContent;
import org.ofdrw.pkg.dir.content.PublicResContent;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

class DocDirTest {

    public static final DocDir docDir() {
        Path font = Paths.get("src/test/resources", "NotoSerifCJKsc-Medium.otf");
        return new DocDir()
                .addResource(font)
                .setPublicRes(PublicResContent.res())
                .setDocument(DocumentContent.doc())
                .setPages(PagesDirTest.pages());
    }

    @Test
    void collect() throws IOException {
        DirCollect docDir = docDir();
        TT.dumpToTmpReview(docDir);
    }
}