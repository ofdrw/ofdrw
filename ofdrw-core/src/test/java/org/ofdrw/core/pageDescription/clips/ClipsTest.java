package org.ofdrw.core.pageDescription.clips;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;

import static org.junit.jupiter.api.Assertions.*;

public class ClipsTest {
    public static Clips clipsCase(){
        return new Clips()
                .addClip(CT_ClipTest.clipCase());
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Clips", clipsCase());
    }
}