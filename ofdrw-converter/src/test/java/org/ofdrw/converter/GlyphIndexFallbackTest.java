package org.ofdrw.converter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.ofdrw.converter.font.FontWrapper;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.OFDReader;
import org.ofdrw.reader.ResourceManage;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 字形索引有效性（GID 降级）判定测试
 * <p>
 * 覆盖字体来源与 GID 分支的四种组合：
 * <ol>
 *     <li>内嵌字体加载成功：字形索引有效，正常绘制；</li>
 *     <li>内嵌字体存在但加载失败：字形索引失效，跳过该段字形并告警；</li>
 *     <li>文档未内嵌字体、系统同名/相似字体匹配成功：字形索引有效，正常绘制；</li>
 *     <li>文档未内嵌字体、系统也无该字体回落到默认字体：字形索引失效，跳过该段字形并告警。</li>
 * </ol>
 * 测试输入全部来自本仓库 {@code src/test/resources}，运行期不访问任何外部样本目录。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class GlyphIndexFallbackTest {

    /**
     * 内嵌了裸 CFF 字体的文档
     */
    private static final String OFD_EMBEDDED = "src/test/resources/intro-数科.ofd";

    /**
     * 未内嵌字体、依赖宿主系统字体的文档
     */
    private static final String OFD_NO_EMBEDDED = "src/test/resources/999.ofd";

    /**
     * 渲染分辨率，单位：像素/毫米
     */
    private static final double PPM = 4;

    @Test
    @DisplayName("a) 内嵌字体加载成功时字形索引有效")
    void embeddedFontLoadedShouldKeepGlyphIndex() throws Exception {
        assertGlyphIndexOfEmbeddedFont(OFD_EMBEDDED);
    }

    @Test
    @DisplayName("b) 内嵌字体存在但加载失败时字形索引失效")
    void brokenEmbeddedFontShouldInvalidateGlyphIndex() throws Exception {
        try (OFDReader reader = new OFDReader(Paths.get(OFD_NO_EMBEDDED))) {
            // 把字体文件指向不存在的路径，模拟“内嵌字体存在但加载失败”
            CT_Font broken = new CT_Font()
                    .setFontName("宋体")
                    .setFamilyName("宋体")
                    .setFontFile("Res/not-exist-font.ttf");
            FontWrapper<TrueTypeFont> wrapper = FontLoader.getInstance()
                    .loadFontSimilar(reader.getResourceLocator(), broken);

            assertNotNull(wrapper);
            assertTrue(wrapper.isEmbedded(), "声明了 FontFile 应视为内嵌字体");
            assertTrue(wrapper.isEnableSimilarFontReplace(), "内嵌字体加载失败应标记为已替换");
            assertTrue(wrapper.isUseSimulatedGlyphIndex(), "内嵌字体加载失败后字形索引应失效");
        }
    }

    @Test
    @DisplayName("c) 未内嵌字体但系统字体匹配成功时字形索引有效")
    void nonEmbeddedFontMatchedBySystemShouldKeepGlyphIndex() throws Exception {
        try (OFDReader reader = new OFDReader(Paths.get(OFD_NO_EMBEDDED))) {
            ResourceManage resourceManage = reader.getResMgt();
            // 字体映射到本机可用的等宽字体，确定性地模拟“系统字体匹配成功”
            FontLoader.getInstance().addSystemFontMapping("GlyphIndexTestMono", systemMonoFontPath());
            CT_Font ctFont = new CT_Font().setID(9999)
                    .setFontName("GlyphIndexTestMono")
                    .setFamilyName("GlyphIndexTestMono");
            assertNull(ctFont.getFontFile(), "该字体未内嵌");

            FontWrapper<TrueTypeFont> wrapper = FontLoader.getInstance()
                    .loadFontSimilar(reader.getResourceLocator(), ctFont);

            assertNotNull(wrapper);
            assertFalse(wrapper.isEmbedded());
            // 既有语义保持不变：非内嵌字体仍标记为已替换
            assertTrue(wrapper.isEnableSimilarFontReplace());
            // 但字形索引有效，渲染时不应跳过 GID 字形
            assertFalse(wrapper.isUseSimulatedGlyphIndex(), "未内嵌但系统匹配成功时字形索引应有效");
            assertNotNull(wrapper.getFont());
            assertTrue(wrapper.isReplacementSupportsText(), "系统匹配成功时不涉及文字覆盖降级");
        }
    }

    @Test
    @DisplayName("d) 未内嵌字体且系统无该字体时回落到默认字体并失效字形索引")
    void nonEmbeddedFontFallsBackToDefaultShouldInvalidateGlyphIndex() throws Exception {
        try (OFDReader reader = new OFDReader(Paths.get(OFD_NO_EMBEDDED))) {
            CT_Font ctFont = new CT_Font().setID(9998)
                    .setFontName("GlyphIndexTestNeverExistsFont")
                    .setFamilyName("GlyphIndexTestNeverExistsFont");
            assertNull(ctFont.getFontFile());

            FontWrapper<TrueTypeFont> wrapper = FontLoader.getInstance()
                    .loadFontSimilar(reader.getResourceLocator(), ctFont);

            assertNotNull(wrapper);
            assertFalse(wrapper.isEmbedded());
            assertTrue(wrapper.isEnableSimilarFontReplace());
            assertTrue(wrapper.isUseSimulatedGlyphIndex(), "回落到默认字体后字形索引应失效");
            assertSame(FontLoader.getInstance().loadDefaultFont(), wrapper.getFont());
            // 字体名与字族名均为拉丁文时探测不到汉字，无法据此判定缺字，按“可覆盖”处理
            assertTrue(wrapper.isReplacementSupportsText(), "纯拉丁字体名无法探测汉字，应保持既有绘制行为");
        }
    }

    @Test
    @DisplayName("d) 字族名为汉字且回落到无汉字默认字体时标记为无法覆盖文字")
    void chineseFamilyNameWithLatinDefaultFontShouldNotSupportText() throws Exception {
        try (OFDReader reader = new OFDReader(Paths.get(OFD_NO_EMBEDDED))) {
            CT_Font ctFont = new CT_Font().setID(9997)
                    .setFontName("GlyphIndexTestNeverExistsFont")
                    .setFamilyName("不存在的测试字族");
            FontWrapper<TrueTypeFont> wrapper = FontLoader.getInstance()
                    .loadFontSimilar(reader.getResourceLocator(), ctFont);

            assertTrue(wrapper.isUseSimulatedGlyphIndex(), "回落到默认字体后字形索引应失效");
            boolean cjk = supportsChinese(wrapper.getFont());
            assertEquals(cjk, wrapper.isReplacementSupportsText(),
                    "是否可覆盖文档文字应取决于默认字体是否具备汉字字形");
        }
    }

    @Test
    @DisplayName("c) 未内嵌字体的文档渲染后仍有文字内容（不丢字）")
    void nonEmbeddedDocumentShouldStillRenderText() throws Exception {
        Path source = Paths.get(OFD_NO_EMBEDDED);
        try (OFDReader reader = new OFDReader(source)) {
            BufferedImage image = new org.ofdrw.converter.canvas.CanvasImageMaker(reader, PPM).makePage(0);
            assertTrue(image.getWidth() > 0 && image.getHeight() > 0);
            // 未内嵌字体的文档依赖宿主系统字体，若判为“替换失效”会整页只剩背景
            // 999.ofd 第 1 页为黑底深色文字，若 GID 分支整段跳过，页面只剩纯黑背景与二维码
            assertTrue(countTextLikePixels(image) > 500, "未内嵌字体的文档渲染后应包含文字内容："
                    + countTextLikePixels(image) + " 个文字像素");
        }
    }

    /**
     * 断言指定文档中存在内嵌字体，且该内嵌字体加载成功、字形索引有效
     *
     * @param ofdPath 文档路径
     * @throws Exception 文档解析异常
     */
    private static void assertGlyphIndexOfEmbeddedFont(String ofdPath) throws Exception {
        try (OFDReader reader = new OFDReader(Paths.get(ofdPath))) {
            ResourceManage resourceManage = reader.getResMgt();
            FontWrapper<TrueTypeFont> embedded = null;
            for (int i = 1; i < 400 && embedded == null; i++) {
                CT_Font ctFont = resourceManage.getFont(String.valueOf(i));
                if (ctFont == null || ctFont.getFontFile() == null) {
                    continue;
                }
                FontWrapper<TrueTypeFont> wrapper = FontLoader.getInstance()
                        .loadFontSimilar(reader.getResourceLocator(), ctFont);
                if (wrapper.getFont() != null) {
                    embedded = wrapper;
                }
            }
            assertNotNull(embedded, "文档中应存在可正常加载的内嵌字体");
            assertTrue(embedded.isEmbedded());
            assertFalse(embedded.isUseSimulatedGlyphIndex(), "内嵌字体加载成功时字形索引应有效");
        }
    }

    /**
     * 统计位图中“既不是透明、也不是纯黑”的像素数量，用于间接判断文字是否绘制
     * <p>
     * 测试样本 {@code 999.ofd} 的页面底色为黑色，文字与表格为深色，
     * 若字形被整段跳过，页面将只剩背景与二维码等少量图元。
     *
     * @param image 页面位图
     * @return 文字类像素数量，单位：像素
     */
    private static int countTextLikePixels(BufferedImage image) {
        int textLike = 0;
        int[] row = new int[image.getWidth()];
        for (int y = 0; y < image.getHeight(); y++) {
            image.getRGB(0, y, image.getWidth(), 1, row, 0, image.getWidth());
            for (int rgb : row) {
                if (rgb != 0x00000000 && rgb != 0xFF000000) {
                    textLike++;
                }
            }
        }
        return textLike;
    }

    /**
     * 判断字体是否具备汉字字形（以“重”字探测）
     *
     * @param font 字体
     * @return true - 字体具备汉字字形
     * @throws Exception 字体解析异常
     */
    private static boolean supportsChinese(TrueTypeFont font) {
        try {
            return font.getUnicodeCmapLookup().getGlyphId('重') != 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 获取本机存在的等宽字体路径，用于构造“系统字体匹配成功”的场景
     *
     * @return 字体绝对路径
     */
    private static String systemMonoFontPath() {
        Path font = Paths.get("/usr/share/fonts/truetype/ubuntu/UbuntuMono[wght].ttf");
        assertTrue(java.nio.file.Files.exists(font), "测试依赖本机 UbuntuMono 字体: " + font);
        return font.toAbsolutePath().toString();
    }
}
