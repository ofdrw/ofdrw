package org.ofdrw.converter.utils;

import org.junit.jupiter.api.Test;

import java.io.File;

/**
 * @author 权观宇
 * @since 2021-04-12 19:42:58
 */
class FontUtilsTest {

    @Test
    public void testWinLoad() {
        FontUtils.scanFontDir(new File("C:/Windows/Fonts"));
        System.out.println(FontUtils.loadSystemFont("Microsoft YaHei", "MicrosoftYaHei"));
        System.out.println(FontUtils.loadSystemFont("Microsoft YaHei", "微软雅黑"));
        System.out.println(FontUtils.loadSystemFont("微软雅黑", "MicrosoftYaHei"));
        System.out.println(FontUtils.loadSystemFont("微软雅黑", "微软雅黑"));
    }
}