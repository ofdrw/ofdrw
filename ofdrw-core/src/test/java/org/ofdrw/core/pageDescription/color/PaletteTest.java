package org.ofdrw.core.pageDescription.color;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;
import org.ofdrw.core.pageDescription.color.colorSpace.CV;
import org.ofdrw.core.pageDescription.color.colorSpace.Palette;

public class PaletteTest {
    public static Palette paletteCase() {
        return new Palette()
                .addCV(new CV(new ST_Array(new String[]{"0", "0", "0"})))
                .addCV(new CV(new ST_Array(new String[]{"255", "0", "0"})));
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("Palette", paletteCase());
    }
}