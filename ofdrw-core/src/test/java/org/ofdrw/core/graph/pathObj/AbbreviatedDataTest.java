package org.ofdrw.core.graph.pathObj;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedList;

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
    public void gen() throws IOException {
        AbbreviatedData data = abbreviatedDataCase().flush();

        System.out.println(data);
        assertTrue("M 50 50 L 250 50 L 250 200 L 50 200 C".equals(data.toString()));
    }

    @Test
    void parse() {
        final LinkedList<OptVal> optVals = AbbreviatedData.parse("M 50 50 L 250 50 L 250 200 L 50 200 C");
        final AbbreviatedData data = new AbbreviatedData(optVals);
        assertEquals("M 50 50 L 250 50 L 250 200 L 50 200 C", data.toString());

        LinkedList<OptVal> optVals2 = AbbreviatedData.parse("M 50 50");
        AbbreviatedData data2 = new AbbreviatedData(optVals2);
        assertEquals("M 50 50", data2.toString());

        optVals2 = AbbreviatedData.parse("M   50   50");
        data2 = new AbbreviatedData(optVals2);
        assertEquals("M 50 50", data2.toString());

        optVals2 = AbbreviatedData.parse("M   NN   50");
        data2 = new AbbreviatedData(optVals2);
        assertEquals("M 0 50", data2.toString());
    }
}