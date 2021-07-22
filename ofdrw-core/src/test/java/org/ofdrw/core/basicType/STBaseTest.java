package org.ofdrw.core.basicType;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-04-15 23:15:28
 */
class STBaseTest {

    @Test
    void toDouble() {
        Map<String, Double> arr = new HashMap<String, Double>() {
            {
                put("2333", 2333D);
                put("11", 11D);
                put("0", 0D);
                put("0.21", 0.21D);
                put("15\u202c", 15D);
                put("-15\u202c", -15D);
                put("-1.5\u202c", -1.5D);
                put("+1.5\u202c", 1.5D);
                put("\u202c+1.5", 1.5D);
            }
        };
        arr.forEach((k, v) -> {
            System.out.println(k);
            assertEquals(v, STBase.toDouble(k));
        });
    }

    @Test
    public void fmt() {
        assertEquals("9", ST_Box.fmt(9));
        assertEquals("9", ST_Box.fmt(9.0));
        assertEquals("112.00", ST_Box.fmt(111.999999));
        assertEquals("3.14", ST_Box.fmt(3.1415926));

    }
}