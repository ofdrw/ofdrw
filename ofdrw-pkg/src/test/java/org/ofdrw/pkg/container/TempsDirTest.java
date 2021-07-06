package org.ofdrw.pkg.container;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-06-17 18:58:51
 */
class TempsDirTest {


    @Test
    public void testRegx() throws Exception {
        String c1 = "Temp_0.xml";
        Matcher m = TempsDir.TempFileNameRegex.matcher(c1);
        assertTrue(m.find());
        System.out.println(m.group(1));

        String c2 = "Temp_NN.xml";
        m = TempsDir.TempFileNameRegex.matcher(c2);
        assertFalse(m.find());

        String c3 = "some_page.xml";
        m = TempsDir.TempFileNameRegex.matcher(c3);
        assertFalse(m.find());
    }
}