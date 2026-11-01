package org.ofdrw.font;

import org.apache.fontbox.ttf.CmapLookup;
import org.ofdrw.converter.font.TrueTypeFont;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * 字体
 *
 * @author 权观宇
 * @since 2020-02-03 02:04:29
 */
public class Font {
    /**
     * 字体名称
     */
    private String name;

    /**
     * 字体族名称
     */
    private String familyName;

    /**
     * 字体文件路径
     */
    private Path fontFile;

    /**
     * 可打印字符宽度映射
     */
    private double[] printableAsciiWidthMap = null;
    /**
     * 缓存的AWT字体
     */
    private java.awt.Font fontObj;

    /**
     * 字体文件的字形度量数据（hmtx + cmap），按需解析并缓存
     * <p>
     * 为 null 表示尚未解析；解析失败后置为 {@link #GLYPH_METRICS_UNAVAILABLE} 以免重复尝试。
     */
    private volatile Object glyphMetrics;

    /**
     * 字符到 em 宽度比例的缓存，键为字符，值为占 em 的比例（1 表示 1 个 em）
     * <p>
     * 宽度比例与字号无关，因此同一字符在任意字号下都可以复用该缓存。
     */
    private final Map<Character, Double> charWidthScaleCache = new HashMap<>();

    /**
     * 字形度量不可用的标记值
     */
    private static final Object GLYPH_METRICS_UNAVAILABLE = new Object();

    /**
     * 是否可嵌入OFD文件包
     */
    private boolean embeddable = true;

    private Font() {
    }

    /**
     * @return 默认字体（Noto思源宋体）
     */
    public static Font getDefault() {
        return FontName.SimSun.font();
    }

    public Font(String name, String familyName, Path fontFile) {
        this.name = name;
        this.familyName = familyName;
        if (fontFile == null || Files.notExists(fontFile)) {
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }
        this.setFontFile(fontFile);
    }

    public Font(String name, String familyName) {
        this.name = name;
        this.familyName = familyName;
    }

    public Font(String name, Path fontFile) {
        this.name = name;
        if (fontFile == null || Files.notExists(fontFile)) {
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }
        this.setFontFile(fontFile);

    }

    /**
     * 创建字体并指定 可打印字符宽度缩放倍数
     *
     * @param name                   字形名
     * @param familyName             字族名
     * @param fontFile               字形文件路径
     * @param printableAsciiWidthMap 可打印字符映射表，用于处理字符的宽度
     */
    public Font(String name, String familyName, Path fontFile, double[] printableAsciiWidthMap) {
        this.name = name;
        this.familyName = familyName;
        this.fontFile = fontFile;
        this.printableAsciiWidthMap = printableAsciiWidthMap;
    }

    /**
     * 字体是否存在预设的字符宽度映射表
     *
     * @return true - 存在；false - 不存在
     */
    public boolean hasWidthMath() {
        return this.printableAsciiWidthMap != null && this.printableAsciiWidthMap.length > 0;
    }

    /**
     * 获取字符占比
     *
     * @param txt 字符
     * @return 0~1 占比
     */
    public double getCharWidthScale(char txt) {
        // 如果存在字符映射那么从字符映射中获取宽度占比
        if (printableAsciiWidthMap != null) {
            // 所有 ASCII码均采用半角
            if (txt >= 32 && txt <= 126) {
                // 根据可打印宽度比例映射表打印
                return printableAsciiWidthMap[txt - 32];
            } else {
                // 非英文字符
                return 1;
            }
        } else {
            // 不存在字符映射，那么认为是等宽度比例 ASCII 为 0.5 其他为 1
            return (txt >= 32 && txt <= 126) ? 0.5 : 1;
        }
    }

    /**
     * 设置可打印字符宽度映射表
     * <p>
     * 在使用操作系统字体时，默认采用ACSII 0.5 其余1的比例计算宽度，因此可能需要手动设置宽度比例才可以达到相应的效果
     *
     * @param map 映射比例表
     * @return this
     */
    public Font setPrintableAsciiWidthMap(double[] map) {
        this.printableAsciiWidthMap = map;
        return this;
    }

    /**
     * 是否可以从字体文件中读取真实字形宽度
     * <p>
     * 当字体绑定了字体文件，且该文件的 hmtx 表与 cmap 表可以被解析时返回 true。
     *
     * @return true - 可以读取真实字形宽度；false - 不可以
     */
    public boolean hasRealGlyphWidth() {
        return resolveGlyphMetrics() != null;
    }

    /**
     * 获取字符的真实字形宽度占 em 的比例
     * <p>
     * 宽度取自字体文件 hmtx 表中该字符对应字形的 advance 宽度，除以字体每 em 的字体单位数换算为 em 比例，
     * 因此与字号无关，可直接乘以字号（单位毫米）得到字符宽度（单位毫米）。
     * <p>
     * 字形数据不可用、或字符在字体文件中缺少对应字形（gid 为 0，即 .notdef）时返回 -1，
     * 由调用方回退到字符宽度映射表或 AWT 测量，以保持不完整字体（如子集字体）的排版行为不变。
     *
     * @param c 字符
     * @return 字符宽度占 em 的比例，1 表示 1 个 em；不可用时返回 -1
     */
    public double getRealCharWidthScale(char c) {
        Double cached = charWidthScaleCache.get(c);
        if (cached != null) {
            return cached;
        }
        double scale = -1;
        GlyphMetrics metrics = resolveGlyphMetrics();
        if (metrics != null) {
            int gid = metrics.lookup.getGlyphId(c);
            // gid 为 0 表示字体中没有该字符的字形（.notdef），此时不使用字体的缺省字形宽度
            if (gid > 0) {
                int advance = metrics.font.getAdvanceWidth(gid);
                if (advance > 0) {
                    scale = advance / (double) metrics.unitsPerEm;
                }
            }
        }
        charWidthScaleCache.put(c, scale);
        return scale;
    }

    /**
     * 解析并缓存字体文件的字形度量数据
     *
     * @return 字形度量数据；字体未绑定文件或解析失败时返回 null
     */
    private GlyphMetrics resolveGlyphMetrics() {
        Object cached = glyphMetrics;
        if (cached != null) {
            return cached == GLYPH_METRICS_UNAVAILABLE ? null : (GlyphMetrics) cached;
        }
        synchronized (this) {
            cached = glyphMetrics;
            if (cached != null) {
                return cached == GLYPH_METRICS_UNAVAILABLE ? null : (GlyphMetrics) cached;
            }
            GlyphMetrics resolved = parseGlyphMetrics();
            glyphMetrics = resolved == null ? GLYPH_METRICS_UNAVAILABLE : resolved;
            return resolved;
        }
    }

    /**
     * 从字体文件解析字形度量数据
     *
     * @return 字形度量数据；字体未绑定文件或解析失败时返回 null
     */
    private GlyphMetrics parseGlyphMetrics() {
        if (fontFile == null || Files.notExists(fontFile)) {
            return null;
        }
        try {
            // parse 返回解析后的新实例，必须使用其返回值
            TrueTypeFont ttf = new TrueTypeFont().parse(fontFile);
            float unitsPerEm = ttf.getUnitsPerEm();
            if (unitsPerEm <= 0) {
                return null;
            }
            CmapLookup lookup = ttf.getUnicodeCmapLookup();
            if (lookup == null) {
                return null;
            }
            return new GlyphMetrics(ttf, lookup, unitsPerEm);
        } catch (Exception e) {
            // 字体文件缺少 hmtx 或 cmap 表时无法获取字形宽度，退化为映射表方式
            return null;
        }
    }

    /**
     * 字体文件的字形度量数据
     */
    private static final class GlyphMetrics {
        /**
         * 解析后的字体，用于读取 hmtx 表中的字形宽度
         */
        private final TrueTypeFont font;
        /**
         * Unicode 到字形索引的映射表
         */
        private final CmapLookup lookup;
        /**
         * 每 em 的字体单位数，单位为字体单位
         */
        private final float unitsPerEm;

        private GlyphMetrics(TrueTypeFont font, CmapLookup lookup, float unitsPerEm) {
            this.font = font;
            this.lookup = lookup;
            this.unitsPerEm = unitsPerEm;
        }
    }

    /**
     * 获取字体全名
     *
     * @return 字体全名
     */
    public String getCompleteFontName() {
        if (familyName == null) {
            return name;
        } else {
            return name + "-" + familyName;
        }
    }

    public String getName() {
        return name;
    }

    public Font setName(String name) {
        this.name = name;
        return this;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Font setFamilyName(String familyName) {
        this.familyName = familyName;
        return this;
    }

    public Path getFontFile() {
        return fontFile;
    }

    /**
     * 获取字体文件名称
     *
     * @return 字体文件名称
     */
    public String getFontFileName() {
        return fontFile.getFileName().toString();
    }

    /**
     * 设置字体文件
     *
     * @param fontFile 字体文件
     * @return this
     */
    public Font setFontFile(Path fontFile) {
        this.fontFile = fontFile;
        // 字体文件变更后，此前解析的字形宽度数据不再有效
        this.glyphMetrics = null;
        this.charWidthScaleCache.clear();
        try (InputStream in = Files.newInputStream(fontFile)) {
            this.fontObj = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, in);
        } catch (FontFormatException | IOException e) {
            throw new IllegalArgumentException("字体文件(fontFile)格式错误");
        }
        return this;
    }

    /**
     * 获取AWT字体对象，该对象可能为空（当前没有提供字体路径时为空）
     *
     * @return 字体对象 或  null
     */
    public java.awt.Font getFontObj() {
        return this.fontObj;
    }

    /**
     * 获取字体嵌入标识
     *
     * @return true-代表字体文件会被嵌入到OFD文件中，false-表示不嵌入
     */
    public boolean isEmbeddable() {
        return embeddable;
    }

    /**
     * 是否可嵌入OFD文件包。 如果设置为false,则字体文件仅在生成OFD文档的过程中被引用，不会包包含到OFD文件内。
     * 默认值为true。
     *
     * @param embeddable true-嵌入，false-不嵌入
     * @return this
     */
    public Font setEmbeddable(boolean embeddable) {
        this.embeddable = embeddable;
        return this;
    }
}
