package org.ofdrw.converter.font.type1;

import org.apache.fontbox.type1.Type1Font;
import org.junit.jupiter.api.Test;

import java.awt.geom.GeneralPath;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 解析Type1 CFF字体测试
 *
 * @author 权观宇
 * @since 2021-10-24 17:07:36
 */
class Type1SegSplitTest {

    @Test
    void parse() throws IOException {
        Path p = Paths.get("src/test/resources/type1_cff.otf");
        final byte[] raw = Files.readAllBytes(p);
        final byte[][] segments = Type1SegSplitParser.split(raw);
        assertNotNull(segments);
        assertEquals(1354, segments[0].length);
        final Type1Font type1Font = Type1SegSplitParser.parse(raw);
        final GeneralPath g38 = type1Font.getPath("G38");
        assertNotNull(g38);
    }
}