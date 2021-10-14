package org.ofdrw.converter.font;

import org.apache.fontbox.cff.CFFFont;
import org.apache.fontbox.cff.CFFParser;
import org.apache.fontbox.ttf.GlyphDescription;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-09-29 20:44:24
 */
class TrueTypeFontTest {
    @Test
    public void cffParse() throws Exception {
        final Path path = Paths.get("C:\\Users\\pc\\Desktop\\ks\\Doc_0\\Res\\font_115.otf");
        final byte[] fileData = Files.readAllBytes(path);

        CFFParser cffParser = new CFFParser();
        CFFFont cffFont = cffParser.parse(fileData).get(0);
        System.out.println(cffFont);
    }

    @Test
    void parse() throws IOException {
        Path fontPath = Paths.get("C:\\Users\\pc\\Desktop\\ks\\Doc_0\\Res\\font_115.otf");
//        Path fontPath = Paths.get("src/test/resources/font_10.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);
    }

    @Test
    void parse2() throws IOException {
        Path fontPath = Paths.get("C:\\Users\\pc\\Desktop\\Latha.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);
        System.out.println(trueTypeFont.psName);
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

    @Test
    void getUnicodeCmapLookup() throws IOException {
        Path fontPath = Paths.get("src/test/resources/font_13132_0_edit.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);


        final int gid = trueTypeFont.getUnicodeCmapLookup().getGlyphId('/');
        assertEquals(402, gid);

        final GlyphData glyph = trueTypeFont.getGlyph(gid);
        GlyphDescription description = glyph.getDescription();
        int count = description.getPointCount();
        assertEquals(4, count);
    }

    @Test
    void getUnicodeGlyph() throws IOException {
        Path fontPath = Paths.get("src/test/resources/font_13132_0_edit.ttf");
        TTFDataStream dataStream = new MemoryTTFDataStream(Files.newInputStream(fontPath));
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(dataStream);

        final GlyphData glyph = trueTypeFont.getUnicodeGlyph('/');
        GlyphDescription description = glyph.getDescription();
        int count = description.getPointCount();
        assertEquals(4, count);
    }
}