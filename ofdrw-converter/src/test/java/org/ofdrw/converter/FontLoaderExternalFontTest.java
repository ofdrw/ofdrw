package org.ofdrw.converter;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.font.TrueTypeFont;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 外部字体加载测试
 * <p>
 * 验证 {@link FontLoader#loadExternalFont(String, String, String)} 按文件内容（魔法值）而不是仅按扩展名识别字体格式。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class FontLoaderExternalFontTest {

    /**
     * 裸 CFF 字体（首4字节 0x01000402），PostScript 名 UCTIWJ+MinionPro-Regular，体积 684 字节
     */
    private static final String FONT_BARE_CFF = "src/test/resources/font_bare_cff_0402.cff";

    /**
     * 裸 CFF 字体对应的 PostScript 名
     */
    private static final String BARE_CFF_PS_NAME = "UCTIWJ+MinionPro-Regular";

    /**
     * 标准 TrueType sfnt 字体（首4字节 0x00010000）
     */
    private static final String FONT_SFNT = "src/test/resources/font_10.ttf";

    /**
     * 标准 TrueType sfnt 字体对应的 PostScript 名
     */
    private static final String SFNT_PS_NAME = "ZJPTDA+FZXBSJW--GB1-0";

    @Test
    void shouldLoadBareCffWithCffExtension() throws IOException {
        FontLoader loader = FontLoader.getInstance();
        TrueTypeFont font = loader.loadExternalFont(Paths.get(FONT_BARE_CFF).toAbsolutePath().toString(), null, null);

        assertNotNull(font, ".cff 扩展名的裸 CFF 字体应能加载成功");
        assertEquals(BARE_CFF_PS_NAME, font.psName);
        // 裸 CFF 的字形轮廓可正常读取
        assertNotNull(font.getPath(0));
    }

    @Test
    void shouldLoadBareCffWithUnknownOrMissingExtension() throws IOException {
        FontLoader loader = FontLoader.getInstance();
        Path source = Paths.get(FONT_BARE_CFF);
        Path target = Files.createTempFile("ofdrw-bare-cff-", ".ofdres");
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            // 扩展名无法表达字体格式时，仍应按内容识别为裸 CFF
            TrueTypeFont font = loader.loadExternalFont(target.toAbsolutePath().toString(), null, null);
            assertNotNull(font, "未知扩展名的裸 CFF 字体应能按内容加载成功");
            assertEquals(BARE_CFF_PS_NAME, font.psName);
        } finally {
            Files.deleteIfExists(target);
        }
    }

    @Test
    void shouldLoadSfntFontWithTtfExtension() {
        FontLoader loader = FontLoader.getInstance();
        TrueTypeFont font = loader.loadExternalFont(Paths.get(FONT_SFNT).toAbsolutePath().toString(), null, null);

        assertNotNull(font);
        assertEquals(SFNT_PS_NAME, font.psName);
    }

    @Test
    void shouldReturnNullForUnparsableFontFile() throws IOException {
        FontLoader loader = FontLoader.getInstance();
        Path target = Files.createTempFile("ofdrw-broken-", ".ttf");
        try {
            Files.write(target, "this is not a font".getBytes(StandardCharsets.US_ASCII));
            // 无法解析的字体文件应返回 null，而不是抛出异常
            assertNull(loader.loadExternalFont(target.toAbsolutePath().toString(), null, null));
        } finally {
            Files.deleteIfExists(target);
        }
    }
}
