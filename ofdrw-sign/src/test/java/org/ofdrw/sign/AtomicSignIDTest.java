package org.ofdrw.sign;

import org.junit.jupiter.api.Test;

import java.util.regex.Matcher;

import static org.junit.jupiter.api.Assertions.*;

class AtomicSignIDTest {
    @Test
    public void testPattern() {
        String str = "s001";
        Matcher m = AtomicSignID.IDPattern.matcher(str);
        if (m.find()) {
            String idNumStr = m.group(1);
            System.out.println(idNumStr);
            System.out.println(Integer.parseInt(idNumStr));
        }
    }

    @Test
    public void incrementAndGet() {
        AtomicSignID atomicSignID = new AtomicSignID();
        String s = atomicSignID.incrementAndGet();
        assertEquals("s001", s);
    }

    @Test
    public void constructTest(){
        AtomicSignID atomicSignID = new AtomicSignID("s776");
        String s = atomicSignID.incrementAndGet();
        assertEquals("s777", s);
    }
}