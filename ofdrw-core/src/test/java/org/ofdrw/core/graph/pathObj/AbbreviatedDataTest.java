package org.ofdrw.core.graph.pathObj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AbbreviatedDataTest {

    public static AbbreviatedData abbreviatedDataCase() {
        return new AbbreviatedData()
                .M(0, 0)
                .L(200, 0)
                .L(200, 150)
                .L(0, 150)
                .C();
    }
    @Test
    public void gen(){
        AbbreviatedData data = abbreviatedDataCase();

        System.out.println(data);
        assertTrue("M 0 0 L 200 0 L 200 150 L 0 150 C".equals(data.toString()));
    }
}