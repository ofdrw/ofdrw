package org.ofdrw.converter.font;

import org.apache.commons.io.IOUtils;
import org.apache.fontbox.ttf.GlyphDescription;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author 权观宇
 * @since 2021-09-29 20:44:24
 */
class TrueTypeFontTest {

    /**
     * 样本 OFD 中裸 CFF 字体（0x01000402）的资源路径
     */
    private static final String BARE_CFF_FONT_ENTRY = "Doc_0/Res/font_3_3.ttf";

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

    /**
     * 字体应能从 hmtx 表读取字形字宽
     */
    @Test
    void getAdvanceWidth() throws IOException {
        Path fontPath = Paths.get("src/test/resources/font_13132_0_edit.ttf");
        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(Files.newInputStream(fontPath));

        // 汉字宽度为 1 个 em（unitsPerEm 为 256）
        assertEquals(256, trueTypeFont.getAdvanceWidth(2));
        // 字形索引越界以及负数索引没有字宽
        assertEquals(-1, trueTypeFont.getAdvanceWidth(-1));
        assertEquals(-1, trueTypeFont.getAdvanceWidth(1000000));
    }

    /**
     * 裸 CFF 数据没有 hmtx 表，按 GID 获取字宽应返回 -1 而不是抛出异常
     */
    @Test
    void getAdvanceWidthOfBareCFF() throws IOException {
        Path ofdPath = Paths.get("src/test/resources/signout.ofd");
        byte[] bareCFF;
        try (ZipFile ofd = new ZipFile(ofdPath.toFile());
             InputStream in = ofd.getInputStream(ofd.getEntry(BARE_CFF_FONT_ENTRY))) {
            bareCFF = IOUtils.toByteArray(in);
        }
        // 确认样本确为裸 CFF 数据：主版本号 1（0x01）、hdrSize 4、offSize 2
        assertEquals(0x01, bareCFF[0] & 0xff);
        assertEquals(0x04, bareCFF[2] & 0xff);

        final TrueTypeFont trueTypeFont = new TrueTypeFont().parse(bareCFF);
        assertEquals(-1, trueTypeFont.getAdvanceWidth(0));
        // 裸 CFF 按 CFF 字形数据获取轮廓仍然可用
        assertNotNull(trueTypeFont.getPath(0));
    }
}