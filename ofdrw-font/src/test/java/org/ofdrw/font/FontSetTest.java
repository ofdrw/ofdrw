package org.ofdrw.font;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2020-03-18 22:13:59
 */
class FontSetTest {

    @Test
    public void get() throws IOException {
         Font font = FontSet.get(FontName.NotoSans);
        final Path fontFile = font.getFontFile();
        assertNotNull(fontFile);
        long size = Files.size(fontFile);
        assertEquals(size, 16489616);
    }
}