package org.ofdrw.core.text;

import org.junit.jupiter.api.Test;
import org.ofdrw.TestTool;
import org.ofdrw.core.basicType.ST_Array;

import static org.junit.jupiter.api.Assertions.*;

public class TextCodeTest {
    public static TextCode textCodeCase() {
        return new TextCode()
                .setX(0.6747)
                .setY(3.5101)
                .setDeltaX(new ST_Array(1.9472, 1.8201, 0.8467, 0.8467))
                .setContent("hello");
    }

    @Test
    public void gen() throws Exception {
        TestTool.genXml("TextCode", textCodeCase());
    }
}