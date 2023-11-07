package org.ofdrw.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FontLoaderTest {

    @Test
    void init() {
//        FontLoader.DEBUG = true;
        FontLoader instance = FontLoader.getInstance();
        String p = instance.getSystemFontPath("宋体", null);
        System.out.println(p);
    }

    @Test
    void testNullFamilyName() {
        FontLoader.DEBUG = true;
        FontLoader instance = FontLoader.getInstance();
        String p = instance.getSystemFontPath(null, "ThisFontNeverExistInYourSystem!!!");
        assertNull(p);
    }
}