package org.ofdrw.sign;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class StandFormatAtomicSignIDTest {
    @Test
    public void testPattern() {
        String str = "s001";
        Matcher m = StandFormatAtomicSignID.IDPattern.matcher(str);
        if (m.find()) {
            String idNumStr = m.group(1);
            System.out.println(idNumStr);
            System.out.println(Integer.parseInt(idNumStr));
        }
    }

    @Test
    public void incrementAndGet() {
        StandFormatAtomicSignID standFormatAtomicSignID = new StandFormatAtomicSignID();
        String s = standFormatAtomicSignID.incrementAndGet();
        assertEquals("s001", s);
    }

    @Test
    public void constructTest(){
        StandFormatAtomicSignID standFormatAtomicSignID = new StandFormatAtomicSignID("s776");
        String s = standFormatAtomicSignID.incrementAndGet();
        assertEquals("s777", s);
    }
}