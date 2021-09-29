package org.ofdrw.converter.font;

import org.apache.fontbox.ttf.GlyphDescription;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-09-29 20:44:24
 */
class TrueTypeFontTest {
    @Test
    void parse() throws IOException {
        Path fontPath = Paths.get("src/test/resources/font_10.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);
    }

    @Test
    void getGlyph() throws IOException {
        Path fontPath = Paths.get("src/test/resources/font_10.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);

        GlyphData glyph = trueTypeFont.getGlyph(1);
        GlyphDescription description = glyph.getDescription();
        int count = description.getPointCount();
        assertEquals(0, count);
        // 第一个不为空字符 句号 “。” 总共16个绘制点
        glyph = trueTypeFont.getGlyph(469);
        description = glyph.getDescription();
        count = description.getPointCount();
        assertEquals(16, count);

    }
}