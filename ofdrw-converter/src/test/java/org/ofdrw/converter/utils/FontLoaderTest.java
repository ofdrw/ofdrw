package org.ofdrw.converter.utils;

import org.junit.jupiter.api.Test;
import org.ofdrw.converter.FontLoader;

import java.io.File;
import java.io.IOException;


/**
 * @author 权观宇
 * @since 2021-04-12 19:42:58
 */
class FontLoaderTest {

    @Test
    public void testWinLoad() {
        FontLoader.getInstance().scanFontDir(new File("C:/Windows/Fonts"));
        System.out.println(FontLoader.getInstance().loadSystemFont("Microsoft YaHei", "MicrosoftYaHei"));
        System.out.println(FontLoader.getInstance().loadSystemFont("Microsoft YaHei", "微软雅黑"));
        System.out.println(FontLoader.getInstance().loadSystemFont("微软雅黑", "MicrosoftYaHei"));
        System.out.println(FontLoader.getInstance().loadSystemFont("微软雅黑", "微软雅黑"));
    }

    @Test
    public void testFontLoad() throws IOException {
//        final Path path = Paths.get("src/test/resources/font_10.ttf");
//        final TrueTypeFont parse = new TTFParser(true).parse(path.toFile());
//        final FontProgram font = FontProgramFactory.createFont("C:\\Users\\pc\\Desktop\\font_85_85.ttf");

    }
}