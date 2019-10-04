package org.ofdrw.core.structure.ofd;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class OFDTest {
    public static OFD ofdCase(){
       return new OFD()
                .addDocBody(DocBodyTest.docBodyCase())
                .addDocBody(DocBodyTest.docBodyCase());
    }

    @Test
    public void gen(){
        TestTool.genXml("OFD", doc ->doc.add(ofdCase()));
    }
}