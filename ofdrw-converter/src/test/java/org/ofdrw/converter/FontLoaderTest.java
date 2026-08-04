package org.ofdrw.converter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FontLoaderTest {


    @Test
    void testNullFamilyName() {
        FontLoader.DEBUG = true;
        FontLoader instance = FontLoader.getInstance();
        String p = instance.getSystemFontPath(null, "ThisFontNeverExistInYourSystem!!!");
        assertNull(p);
    }
}