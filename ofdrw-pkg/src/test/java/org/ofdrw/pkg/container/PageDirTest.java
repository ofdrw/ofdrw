package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.AssertionsKt;
import org.junit.jupiter.api.Test;
import org.ofdrw.core.annotation.pageannot.PageAnnot;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.pkg.container.content.PageContent;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

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

    @Test
    void addAnnot() throws IOException {
        Path p = Paths.get("target/Page_0");
        PageAnnot pa = new PageAnnot();
        PageDir pd1 = new PageDir(p);
        pd1.clean();

        pd1 = new PageDir(p);
        ST_Loc annotLoc = pd1.addAnnot(pa);
        // test case 1 不存在
        pd1.flush();
        Assertions.assertEquals(annotLoc.getFileName(),"Annot_0.xml");
        pd1.clean();

        // test case 2 不规则文件名，如 Annot_Text.xml
        pd1 = new PageDir(p);
        pd1.putObj("Annot_Text.xml", pa);
        pd1.flush();
        annotLoc = pd1.addAnnot(pa);
        ST_Loc   annotLoc2 = pd1.addAnnot(pa);
        pd1.flush();
        Assertions.assertEquals(annotLoc.getFileName(),"Annot_0.xml");
        Assertions.assertEquals(annotLoc2.getFileName(),"Annot_1.xml");

    }

}