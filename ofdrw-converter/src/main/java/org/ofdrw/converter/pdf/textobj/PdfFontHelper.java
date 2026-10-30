package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.GlyphData;
import org.apache.fontbox.ttf.GlyphTable;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.fontbox.util.BoundingBox;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.font.encoding.GlyphList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * PDF 字体相关的通用取数工具
 * <p>
 * 负责字形索引取数、Unicode 取数与字形名反查，均以只读方式访问 {@link PDFont}，不缓存字体程序。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfFontHelper {

    private static final Logger logger = LoggerFactory.getLogger(PdfFontHelper.class);

    /**
     * Adobe Glyph List（延迟初始化），缺 ToUnicode 时用于字形名反查
     */
    private static volatile GlyphList GLYPH_LIST = null;

    /**
     * 每个 PDF 字体的码位 -> 字形名 缓存，供 AGL 反查与 warning 使用
     */
    private static final Map<PDFont, Map<Integer, String>> GLYPH_NAME_CACHE = new ConcurrentHashMap<>();

    private PdfFontHelper() {
    }

    /**
     * 判断字体是否为 TrueType 轮廓、可提取字体程序的类型
     * <p>
     * 支持 {@link PDTrueTypeFont}、{@link PDType0Font}（其下游为 {@link PDCIDFontType2}）。
     * Type3、CFF（{@link PDType1CFont}/{@link PDCIDFontType0}）、Type1 由调用方回退路径。
     *
     * @param font PDF 字体
     * @return true - 可尝试对象化
     */
    public static boolean isTrueTypeLike(PDFont font) {
        if (font instanceof PDTrueTypeFont) {
            return true;
        }
        if (font instanceof PDType0Font) {
            PDCIDFont descendant = ((PDType0Font) font).getDescendantFont();
            return descendant instanceof PDCIDFontType2;
        }
        return false;
    }

    /**
     * 取字形在字体程序中的原始 GID
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @return 字形索引，取不到时返回 -1
     */
    public static int codeToGid(PDFont font, int code) {
        try {
            if (font instanceof PDType0Font) {
                return ((PDType0Font) font).codeToGID(code);
            }
            if (font instanceof PDTrueTypeFont) {
                return ((PDTrueTypeFont) font).codeToGID(code);
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("取字形索引失败：{} code={}", fontName(font), code, e);
        }
        return -1;
    }

    /**
     * 取字体中的 TrueType 字体程序
     *
     * @param font PDF 字体
     * @return 字体程序；字体不是 TrueType 或程序缺失/损坏时返回 null
     */
    public static TrueTypeFont trueTypeFont(PDFont font) {
        try {
            if (font instanceof PDTrueTypeFont) {
                return ((PDTrueTypeFont) font).getTrueTypeFont();
            }
            if (font instanceof PDType0Font) {
                PDCIDFont descendant = ((PDType0Font) font).getDescendantFont();
                if (descendant instanceof PDCIDFontType2) {
                    return ((PDCIDFontType2) descendant).getTrueTypeFont();
                }
            }
        } catch (RuntimeException e) {
            logger.debug("字体程序解析失败：{}", fontName(font), e);
        }
        return null;
    }

    /**
     * 取字体单位 em 的字形数（unitsPerEm），用于字形包围盒换算
     *
     * @param font PDF 字体
     * @return 每 em 单位数，取不到时返回 1000
     */
    public static float unitsPerEm(PDFont font) {
        TrueTypeFont ttf = trueTypeFont(font);
        if (ttf != null) {
            try {
                if (ttf.getHeader() != null && ttf.getHeader().getUnitsPerEm() > 0) {
                    return ttf.getHeader().getUnitsPerEm();
                }
            } catch (IOException | RuntimeException e) {
                logger.debug("取 unitsPerEm 失败：{}", fontName(font), e);
            }
        }
        return 1000f;
    }

    /**
     * 取字符对应的 Unicode 文本
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @return Unicode 文本；无法确定时返回 null
     */
    public static String toUnicode(PDFont font, int code) {
        try {
            return font.toUnicode(code);
        } catch (RuntimeException e) {
            logger.debug("toUnicode 失败：{} code={}", fontName(font), code, e);
            return null;
        }
    }

    /**
     * 通过字形名反查 Unicode（Adobe Glyph List）
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @return 反查得到的 Unicode 文本；无法反查时返回 null
     */
    public static String unicodeFromGlyphName(PDFont font, int code) {
        String name = glyphName(font, code);
        if (name == null || name.isEmpty() || ".notdef".equals(name)) {
            return null;
        }
        GlyphList list = adobeGlyphList();
        if (list != null) {
            String uni = list.toUnicode(name);
            if (uni != null) {
                return uni;
            }
        }
        // uniXXXX / uXXXX[XX] 形式的字形名
        if (name.length() > 3 && name.regionMatches(true, 0, "uni", 0, 3)) {
            String hex = name.substring(3);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i + 4 <= hex.length(); i += 4) {
                try {
                    sb.append((char) Integer.parseInt(hex.substring(i, i + 4), 16));
                } catch (NumberFormatException e) {
                    return null;
                }
            }
            return sb.length() == 0 ? null : sb.toString();
        }
        if (name.length() > 1 && (name.charAt(0) == 'u' || name.charAt(0) == 'U')) {
            String hex = name.substring(1);
            try {
                return new String(Character.toChars(Integer.parseInt(hex, 16)));
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        // 单字符字形名（如 "A"、"space" 未收录时）
        if (name.length() == 1) {
            return name;
        }
        return null;
    }

    /**
     * 取字符在 PDF 字体中对应的字形名
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @return 字形名，取不到返回 null
     */
    public static String glyphName(PDFont font, int code) {
        Map<Integer, String> cache = GLYPH_NAME_CACHE.computeIfAbsent(font, k -> new ConcurrentHashMap<>());
        String cached = cache.get(code);
        if (cached != null) {
            return cached.isEmpty() ? null : cached;
        }
        String name = null;
        try {
            name = glyphNameOfCode(font, code);
        } catch (IOException | RuntimeException e) {
            logger.debug("取字形名失败：{} code={}", fontName(font), code, e);
        }
        cache.put(code, name == null ? "" : name);
        return name;
    }

    /**
     * 按字符编码取字形名
     * <p>
     * 简单字体取编码表的字形名；复合字体的字符编码是 CID，没有对应的字形名，返回 null
     * （这类字体由 ToUnicode 或 CGTransform 决定字形，通常不需要反查）。
     *
     * @param font PDF 字体
     * @param code PDF 字符编码
     * @return 字形名，取不到返回 null
     * @throws IOException PDF 解析异常
     */
    private static String glyphNameOfCode(PDFont font, int code) throws IOException {
        if (font instanceof PDSimpleFont) {
            org.apache.pdfbox.pdmodel.font.encoding.Encoding encoding =
                    ((PDSimpleFont) font).getEncoding();
            return encoding == null ? null : encoding.getName(code);
        }
        if (font instanceof PDType0Font) {
            PDCIDFont descendant = ((PDType0Font) font).getDescendantFont();
            if (descendant != null) {
                return descendant.getName();
            }
        }
        return null;
    }

    /**
     * 字体的展示名（去掉子集前缀）
     *
     * @param font PDF 字体，可为 null
     * @return 字体名，无法确定时返回 "unknown"
     */
    public static String fontName(PDFont font) {
        if (font == null) {
            return "unknown";
        }
        try {
            String name = font.getName();
            if (name != null && !name.isEmpty()) {
                return stripSubsetPrefix(name);
            }
        } catch (RuntimeException e) {
            logger.debug("取字体名失败", e);
        }
        return font.getClass().getSimpleName();
    }

    /**
     * 去掉 PDF 子集字体名前缀（形如 "ABCDEF+"）
     *
     * @param name 字体名
     * @return 去掉前缀后的字体名
     */
    public static String stripSubsetPrefix(String name) {
        if (name == null) {
            return null;
        }
        if (name.length() > 7 && name.charAt(6) == '+' && name.substring(0, 6).matches("[A-Z]{6}")) {
            return name.substring(7);
        }
        return name;
    }

    /**
     * 取字符在字体程序中的字形轮廓包围盒（字体设计单位）
     *
     * @param font PDF 字体
     * @param gid  字形索引
     * @return {xMin, yMin, xMax, yMax}，字形无轮廓或取不到时返回 null
     */
    public static float[] gidBBox(PDFont font, int gid) {
        if (gid < 0) {
            return null;
        }
        TrueTypeFont ttf = trueTypeFont(font);
        if (ttf == null) {
            return null;
        }
        try {
            GlyphTable glyphTable = ttf.getGlyph();
            if (glyphTable == null) {
                return null;
            }
            /*
             * 复合字形（composite）的 BoundingBox 只保存组件及其变换，真实轮廓范围必须展开组件才能得到；
             * fontbox 的 GlyphData#getBoundingBox 对复合字形返回的是组件框，直接使用会让 Boundary 偏小。
             * 这里对复合字形按组件递归展开（变换作用于组件设计坐标），简单字形直接取原始框。
             */
            float[] expanded = compositeBBox(ttf, glyphTable.getGlyph(gid));
            if (expanded != null) {
                return expanded;
            }
            GlyphData glyph = glyphTable.getGlyph(gid);
            BoundingBox bb = glyph == null ? null : glyph.getBoundingBox();
            if (bb == null) {
                return null;
            }
            return new float[]{(float) bb.getLowerLeftX(), (float) bb.getLowerLeftY(),
                    (float) bb.getUpperRightX(), (float) bb.getUpperRightY()};
        } catch (IOException | RuntimeException e) {
            logger.debug("取字形包围盒失败：{} gid={}", fontName(font), gid, e);
            return null;
        }
    }

    /**
     * 计算复合字形（composite）展开后的包围盒
     * <p>
     * 复合字形的每个组件带 2×2 变换与平移，组件坐标以字体设计单位给出；本方法按组件递归展开，
     * 把所有组件的包围盒并集换算回复合字形自身的坐标系。
     *
     * @param ttf   字体
     * @param glyph 字形数据
     * @return {xMin, yMin, xMax, yMax}；简单字形或取不到组件时返回 null
     * @throws IOException 字体解析异常
     */
    private static float[] compositeBBox(TrueTypeFont ttf, GlyphData glyph) throws IOException {
        if (glyph == null || glyph.getDescription() == null) {
            return null;
        }
        Object description = glyph.getDescription();
        if (!(description instanceof org.apache.fontbox.ttf.GlyfCompositeDescript)) {
            return null;
        }
        org.apache.fontbox.ttf.GlyfCompositeDescript composite =
                (org.apache.fontbox.ttf.GlyfCompositeDescript) description;
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE;
        for (org.apache.fontbox.ttf.GlyfCompositeComp comp : composite.getComponents()) {
            float[] sub = comp.getGlyphIndex() >= 0
                    ? gidBBoxRaw(ttf, comp.getGlyphIndex()) : null;
            if (sub == null) {
                continue;
            }
            double[][] pts = {{sub[0], sub[1]}, {sub[2], sub[1]}, {sub[0], sub[3]}, {sub[2], sub[3]}};
            for (double[] p : pts) {
                // 2×2 仿射：x' = xScale·x + scale01·y + dx，y' = scale10·x + yScale·y + dy
                double x = comp.getXScale() * p[0] + comp.getScale01() * p[1] + comp.getXTranslate();
                double y = comp.getScale10() * p[0] + comp.getYScale() * p[1] + comp.getYTranslate();
                minX = (float) Math.min(minX, x);
                minY = (float) Math.min(minY, y);
                maxX = (float) Math.max(maxX, x);
                maxY = (float) Math.max(maxY, y);
            }
        }
        if (minX > maxX || minY > maxY) {
            return null;
        }
        return new float[]{minX, minY, maxX, maxY};
    }

    /**
     * 取字形原始包围盒（不做复合展开），供 {@link #compositeBBox} 递归使用
     *
     * @param ttf 字体
     * @param gid 字形索引
     * @return {xMin, yMin, xMax, yMax}，取不到时返回 null
     * @throws IOException 字体解析异常
     */
    private static float[] gidBBoxRaw(TrueTypeFont ttf, int gid) throws IOException {
        GlyphData g = ttf.getGlyph().getGlyph(gid);
        if (g == null || g.getDescription() == null) {
            return null;
        }
        return compositeBBox(ttf, g) == null ? simpleBBox(g) : compositeBBox(ttf, g);
    }

    /**
     * 简单字形的原始包围盒
     *
     * @param g 字形数据
     * @return {xMin, yMin, xMax, yMax}，取不到时返回 null
     */
    private static float[] simpleBBox(GlyphData g) {
        BoundingBox bb = g.getBoundingBox();
        if (bb == null) {
            return null;
        }
        return new float[]{(float) bb.getLowerLeftX(), (float) bb.getLowerLeftY(),
                (float) bb.getUpperRightX(), (float) bb.getUpperRightY()};
    }

    /**
     * 取字体垂直度量（字体设计单位，y 向上）
     * <p>
     * 优先使用 hhea 的 ascender/descender（与 TrueType 渲染器的基线定位一致），
     * 取不到时退回 OS/2 的 sTypoAscender/sTypoDescender，仍取不到时按“上下各 1 em”处理。
     *
     * @param font PDF 字体
     * @return {ascender, descender}；descender 为负值，单位：字体设计单位
     */
    public static float[] verticalMetrics(PDFont font) {
        TrueTypeFont ttf = trueTypeFont(font);
        if (ttf != null) {
            try {
                org.apache.fontbox.ttf.HorizontalHeaderTable hhea = ttf.getHorizontalHeader();
                if (hhea != null && hhea.getAscender() != 0) {
                    return new float[]{hhea.getAscender(), hhea.getDescender()};
                }
                org.apache.fontbox.ttf.OS2WindowsMetricsTable os2 = ttf.getOS2Windows();
                if (os2 != null && os2.getTypoAscender() != 0) {
                    return new float[]{os2.getTypoAscender(), os2.getTypoDescender()};
                }
            } catch (IOException | RuntimeException e) {
                logger.debug("取字体垂直度量失败：{}", fontName(font), e);
            }
        }
        // 兜底：以 1 em 作为上伸部与下伸部
        float upem = unitsPerEm(font);
        return new float[]{upem, -upem};
    }

    /**
     * 延迟初始化 Adobe Glyph List
     *
     * @return glyphlist.txt 内容，加载失败时返回 null（此时仅使用 uniXXXX 规则反查）
     */
    private static GlyphList adobeGlyphList() {
        GlyphList list = GLYPH_LIST;
        if (list != null) {
            return list;
        }
        synchronized (PdfFontHelper.class) {
            if (GLYPH_LIST == null) {
                try {
                    GLYPH_LIST = GlyphList.getAdobeGlyphList();
                } catch (RuntimeException e) {
                    logger.warn("Adobe Glyph List 加载失败，缺 ToUnicode 时将只按 uniXXXX 规则反查", e);
                    GLYPH_LIST = null;
                }
            }
            return GLYPH_LIST;
        }
    }
}
