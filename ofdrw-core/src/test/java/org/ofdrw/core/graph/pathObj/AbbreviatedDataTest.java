package org.ofdrw.core.graph.pathObj;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AbbreviatedDataTest {

    public static AbbreviatedData abbreviatedDataCase() {
        return new AbbreviatedData()
                .M(50, 50)
                .L(250, 50)
                .L(250, 200)
                .L(50, 200)
                .C();
    }
    @Test
    public void gen(){
        AbbreviatedData data = abbreviatedDataCase();

        System.out.println(data);
        assertTrue("M 50 50 L 250 50 L 250 200 L 50 200 C".equals(data.toString()));
    }
}