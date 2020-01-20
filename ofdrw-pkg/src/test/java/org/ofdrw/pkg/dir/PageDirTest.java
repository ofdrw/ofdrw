package org.ofdrw.pkg.dir;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.dir.content.PageContent;

import java.io.IOException;

class PageDirTest {

    public static final PageDir pageDir(){
       return new PageDir()
                .setContent(PageContent.page());
    }

    @Test
    void collect() throws IOException {

        TT.dumpToTmpReview(pageDir());
    }
}