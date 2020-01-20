package org.ofdrw.pkg.dir;

import org.junit.jupiter.api.Test;
import org.ofdrw.pkg.dir.content.OFDContent;


import java.io.IOException;


class OFDDirTest {

    public static final OFDDir ofdDir(){
        return new OFDDir()
                .add(DocDirTest.docDir())
                .setOfd(OFDContent.ofd());
    }

    @Test
    void collect() throws IOException {
        TT.dumpToTmpReview(ofdDir());
    }

    @Test
    void jar() throws IOException {
        OFDDir ofdDir = ofdDir();
        ofdDir.jar("target/hello.ofd");
    }



}