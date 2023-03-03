package org.ofdrw.font;

import java.awt.*;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Map;

/**
 * 环境变量中的字体
 *
 * @author 权观宇
 * @since 2023-3-2 22:05:45
 */
public final class EnvFont {

    /**
     * 字体缓存
     */
    private static Map<String, java.awt.Font> fMap;

    /**
     * 字体渲染上线文
     */
    private static FontRenderContext frCtx;

    /**
     * 在当前环境中寻找指定名称的字体
     *
     * @param name 字体名
     * @return 指定名称字体，若不存在则返回空。
     */
    public static java.awt.Font getFont(String name) {
        if (fMap != null) {
            return fMap.get(name);
        }
        // 静态初始化锁防止多线程初始化字体映射异常
        synchronized (EnvFont.class) {
            fMap = new HashMap<>();
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            java.awt.Font[] allFonts = ge.getAllFonts();
            for (java.awt.Font font : allFonts) {
                fMap.put(font.getFontName(), font);
            }
            return fMap.get(name);
        }
    }

    /**
     * 在当前环境中寻找指定名称的字体
     *
     * @param name   字体名
     * @param family 替换字体名
     * @return 指定名称字体，若不存在则返回空。
     */
    public static java.awt.Font getFont(String name, String family) {
        Font res = getFont(name);
        if (res != null) {
            return res;
        }
        return getFont(family);
    }


    /**
     * 分析字符串大小在指定字号下所占空间大小
     *
     * @param name   字体名
     * @param family 替换字体名
     * @param str    待分析字符串
     * @param size   字体大小
     * @return 字符所占区域大小
     */
    public static Rectangle2D strBounds(String name, String family, String str, double size) {
        if (frCtx == null) {
            synchronized (EnvFont.class){
                frCtx = new FontRenderContext(new AffineTransform(), true, true);
            }
        }
        Font font = getFont(name, family);
        font = font.deriveFont((float) size);
        return font.getStringBounds(str, frCtx);
    }

}
