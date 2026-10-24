package org.ofdrw.converter;

import org.apache.fontbox.ttf.CmapLookup;
import org.ofdrw.converter.font.TTFDataStream;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.TextCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 渲染链路（AWTMaker、字体加载）公开 API 兼容性回归测试。
 * <p>
 * 与 {@code org.ofdrw.converter.font.ApiCompatibilityTest}（锁定 {@code org.ofdrw.converter.font} 包内类型签名与行为）
 * 分工：本类只锁定 {@link AWTMaker}、{@link FontLoader} 这两处渲染入口的公开/受保护签名与关键静态行为，
 * 二者内容不重叠。
 *
 * @author 权观宇
 * @since 2026-10-24
 */
class RendererApiCompatibilityTest {

    /**
     * 锁定 AWTMaker#writeText 的方法签名与 parseDelta 的偏移量解析行为
     * <p>
     * 阶段 3 起绘制下沉到 {@link org.ofdrw.converter.renderer.Renderer}，
     * 文字绘制入口的第一个参数由 Graphics2D 改为 Renderer，其余参数与返回类型保持不变。
     *
     * @throws Exception 方法查找失败
     */
    @Test
    @DisplayName("AWTMaker：writeText 签名与 parseDelta 行为")
    void awtMakerShouldKeepWriteTextSignature() throws Exception {
        // 文字绘制入口签名：参数列表变化会导致这里抛 NoSuchMethodException
        Method writeText = AWTMaker.class.getDeclaredMethod("writeText",
                org.ofdrw.converter.renderer.Renderer.class, TextObject.class,
                org.ofdrw.converter.style.StyleScope.class, org.ujmp.core.Matrix.class,
                org.ofdrw.converter.style.StyleResolver.class);
        assertFalse(writeText.isSynthetic());
        assertEquals(void.class, writeText.getReturnType());

        // 字符偏移量解析行为保持不变
        assertEquals(Arrays.asList(3.0, 3.0, 3.0), AWTMaker.parseDelta(new org.ofdrw.core.basicType.ST_Array(3, 3, 3)));
        assertTrue(AWTMaker.parseDelta(null).isEmpty());
    }

    /**
     * 锁定 FontLoader 的字体重载入方法签名（字体迁移到 ofdrw-font 后调用方不需要改动）
     *
     * @throws Exception 方法查找失败
     */
    @Test
    @DisplayName("FontLoader：字体重载方法签名")
    void fontLoaderShouldKeepLoadFontSignatures() throws Exception {
        Class<FontLoader> clazz = FontLoader.class;
        assertEquals(TrueTypeFont.class, clazz.getMethod("loadExternalFont", String.class).getReturnType());
        assertEquals(TrueTypeFont.class,
                clazz.getMethod("loadExternalFont", String.class, String.class, String.class).getReturnType());
        assertEquals(TrueTypeFont.class, clazz.getMethod("loadSystemFont", String.class, String.class).getReturnType());
        assertEquals(TrueTypeFont.class, clazz.getMethod("loadFont", org.ofdrw.reader.ResourceLocator.class,
                org.ofdrw.core.text.font.CT_Font.class).getReturnType());
        // 字体解析入口仍返回同一包内的 TrueTypeFont（迁移前后全限定名不变）
        assertEquals(CmapLookup.class, TrueTypeFont.class.getMethod("getUnicodeCmapLookup").getReturnType());
        assertEquals(TTFDataStream.class,
                TrueTypeFont.class.getMethod("parse", TTFDataStream.class).getParameterTypes()[0]);
    }
}
