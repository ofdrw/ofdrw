package org.ofdrw.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.canvas.CanvasImageMaker;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.reader.OFDReader;

import java.awt.image.BufferedImage;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 默认字体与中文族名匹配测试
 * <p>
 * 验证 {@link FontLoader} 在系统装有中文字体时：
 * <ol>
 *     <li>默认字体优先选择能覆盖汉字的字体，避免无内嵌字体的中文文档渲染成方块；</li>
 *     <li>“宋体/黑体/楷体/仿宋/微软雅黑”等中文族名及其英文名能匹配到系统字体；</li>
 *     <li>未内嵌字体的中文文档能渲染出真实中文字形（不是 .notdef 方块）。</li>
 * </ol>
 * 本机未安装中文字体时相关用例自动跳过（{@link org.junit.jupiter.api.Assumptions}）。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class DefaultFontChineseTest {

    /**
     * 无内嵌字体、依赖宿主系统中文字体的文档
     */
    private static final String OFD_NO_EMBEDDED = "src/test/resources/999.ofd";

    /**
     * 渲染分辨率，单位：像素/毫米
     */
    private static final double PPM = 4;

    @Test
    @DisplayName("默认字体应包含中文字形")
    void defaultFontShouldContainChineseGlyphs() {
        TrueTypeFont defaultFont = FontLoader.getInstance().loadDefaultFont();
        assertNotNull(defaultFont, "默认字体不应为空");
        int glyphId = glyphIdOf(defaultFont, '中');
        org.junit.jupiter.api.Assumptions.assumeTrue(glyphId > 0,
                "当前系统未安装中文字体，跳过：默认字体为 " + defaultFont.psName);
        assertTrue(FontLoader.supportsChinese(defaultFont), "默认字体应覆盖常用汉字: " + defaultFont.psName);
        assertTrue(glyphIdOf(defaultFont, '国') > 0, "默认字体应能映射“国”字");
    }

    @Test
    @DisplayName("中文字族名与英文名应匹配到同一系统字体")
    void chineseFamilyNameShouldMatchSystemFont() {
        FontLoader loader = FontLoader.getInstance();
        String byChineseName = loader.getReplaceSimilarFontPath("宋体", "宋体");
        org.junit.jupiter.api.Assumptions.assumeTrue(byChineseName != null,
                "当前系统未安装中文字体，跳过中文族名匹配用例");
        String byEnglishName = loader.getReplaceSimilarFontPath("SimSun", "SimSun");
        assertNotNull(byEnglishName, "英文族名 SimSun 应能匹配到系统字体");
        assertNotNull(byChineseName, "中文族名“宋体”应能匹配到系统字体");

        // 其他常见中文族名（系统未安装时允许为 null，但一旦命中必须是可用字体）
        for (String family : new String[]{"黑体", "SimHei", "楷体", "KaiTi", "仿宋", "FangSong", "微软雅黑", "Microsoft YaHei"}) {
            String path = loader.getReplaceSimilarFontPath(family, family);
            if (path != null) {
                assertTrue(new java.io.File(path).isFile(), "匹配到的字体文件应存在: " + family + " -> " + path);
            }
        }
    }

    @Test
    @DisplayName("未内嵌字体的中文文档应渲染出真实汉字而非方块")
    void nonEmbeddedChineseDocumentShouldRenderRealGlyphs() throws Exception {
        TrueTypeFont defaultFont = FontLoader.getInstance().loadDefaultFont();
        org.junit.jupiter.api.Assumptions.assumeTrue(defaultFont != null && glyphIdOf(defaultFont, '中') > 0,
                "当前系统未安装中文字体，跳过");

        try (OFDReader reader = new OFDReader(Paths.get(OFD_NO_EMBEDDED))) {
            BufferedImage image = new CanvasImageMaker(reader, PPM).makePage(0);
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
            // 页面底色为黑色，汉字字形以文字色叠加；存在文字色像素即说明字形被真实绘制
            assertTrue(hasTextPixels(image), "未内嵌字体的中文文档应渲染出文字");
        }
    }

    /**
     * 获取字符在字体 cmap 中的字形索引
     *
     * @param font 字体
     * @param c    字符
     * @return 字形索引，0 表示 .notdef 或字体无 cmap
     */
    private static int glyphIdOf(TrueTypeFont font, char c) {
        try {
            return font.getUnicodeCmapLookup().getGlyphId(c);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 判断位图中是否存在非背景、非纯黑的文字像素
     *
     * @param image 页面位图
     * @return true - 存在文字像素
     */
    private static boolean hasTextPixels(BufferedImage image) {
        int background = image.getRGB(0, 0);
        int[] row = new int[image.getWidth()];
        int count = 0;
        for (int y = 0; y < image.getHeight(); y++) {
            image.getRGB(0, y, image.getWidth(), 1, row, 0, image.getWidth());
            for (int rgb : row) {
                if (rgb != background && rgb != 0xFF000000) {
                    count++;
                }
            }
        }
        return count > 500;
    }
}
