package org.ofdrw.pkg.dir;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PagesDirTest {

    public static final PagesDir pages(){
        return new PagesDir()
                .add(PageDirTest.pageDir());
    }

    @Test
    void collect() throws IOException {
        PagesDir ps = pages();
        TT.dumpToTmpReview(ps);
    }
}