package org.ofdrw.sign;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-08-24 20:40:54
 */
class NumberFormatAtomicSignIDTest {
    @Test
    void constructNumberFormatAtomicSignID() {
        NumberFormatAtomicSignID nfas = new NumberFormatAtomicSignID("s001");
        String s = nfas.incrementAndGet();
        assertEquals("002", s);
    }

    @Test
    void setCurrentMaxSignId() {
        NumberFormatAtomicSignID val = new NumberFormatAtomicSignID();
        val.setCurrentMaxSignId("003");
        String actual = val.get();
        assertEquals("003", actual);
    }

    @Test
    void incrementAndGet() {
        NumberFormatAtomicSignID val = new NumberFormatAtomicSignID();
        String actual = val.incrementAndGet();
        assertEquals("001", actual);
    }

    @Test
    void get() {
        NumberFormatAtomicSignID val = new NumberFormatAtomicSignID();
        String actual = val.get();
        assertEquals("000", actual);
    }

    @Test
    void parse() {
        NumberFormatAtomicSignID val = new NumberFormatAtomicSignID();
        int actual = val.parse("079");
        assertEquals(79, actual);
    }
}