package org.ofdrw.core.customTags;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Loc;

import static org.junit.jupiter.api.Assertions.*;

public class CustomTagTest {
    public static CustomTag customTagCase() {
        return new CustomTag()
                .setTypeID("SPEC_TYPE_ID")
                .setFileLoc(new ST_Loc("Res/customTags.xml"));
    }
    
    @Test
    public void gen() throws Exception {
        TestTool.genXml("CustomTag", customTagCase());
    }
}