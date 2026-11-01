package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDFontDescriptor;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.ofdrw.converter.font.FontDescriptor;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.core.text.font.Charset;
import org.ofdrw.graphics2d.OFDGraphicsDocument;
import org.ofdrw.pkg.container.ResDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * PDF 字体 → OFD 字体资源注册表（设计文档 5.5.2、5.6）
 * <p>
 * 两遍式：渲染阶段只做“登记”（解析字体程序、分配资源 ID、记录实际使用的 GID 与 Unicode），
 * {@link #flush()} 时统一写出字体文件并回填 {@code CT_Font.FontFile}；{@link #close()} 等价于 {@link #flush()}，
 * 必须在 {@code OFDGraphicsDocument#close()} 之前调用。
 * <p>
 * 字体来源优先级：
 * <ol>
 *     <li><b>PDF 内嵌字体程序原样嵌入</b>（默认策略）：{@code FontFile2}（TrueType）或
 *     {@code FontFile3}（Subtype=OpenType / Type1C）逐字节写入 {@code Res/<FontName>.ttf|.otf}，
 *     GID 与 PDF 完全一致，cmap 原样保留；</li>
 *     <li>字体集合（TTC）：提取指定字体重建 sfnt 后原样嵌入；裸 CFF：包装为最小 OpenType 后嵌入；</li>
 *     <li>字体程序不可用（Type1 PFB、非 CIDFontType2 的复合字体、数据损坏）：按字体名在系统/配置字体目录
 *     <b>精确匹配</b>后嵌入，并记录 warning；</li>
 *     <li>仍不可用：{@link #registerFont(PDFont)} 返回 null，调用方回退路径绘制；</li>
 *     <li>可选体积优化：{@link #setSubsetFont(boolean)} 开启后，完整 CJK 字体走 {@link FontSubsetTool}
 *     子集化 + 表裁剪（默认关闭，见设计文档 5.6.3/5.6.4）。</li>
 * </ol>
 * 去重：以“字体程序字节 SHA-256 + TTC 索引 + 是否子集”为键，同一字体程序只产生一个 {@code CT_Font}；
 * 同一字体程序的不同 PDF 编码（Identity-H / WinAnsi）共享同一资源，字符↔字形关系由 CGTransform 表达。
 * <p>
 * 失败一律不抛异常：子集化失败回退整字体嵌入，整字体也不可用时返回 null，所有失败按“原因 + 字体名”聚合为 warning。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class PdfFontRegistry implements AutoCloseable {

    private static final Logger logger = LoggerFactory.getLogger(PdfFontRegistry.class);

    /**
     * 子集化判定倍数：字形总数超过“使用字形数 × 该倍数”时才子集化
     * <p>
     * 取 4：CJK 字体即使只用了常用字，字形总数也会远超使用数的 4 倍，收益明显；
     * 与 {@link #SUBSET_MIN_SIZE} 一起决定“子集化是否有收益”（二者满足其一即可）。
     */
    private static final int SUBSET_GLYPH_RATIO = 4;

    /**
     * 子集化判定阈值：字体文件最小体积，单位：字节（512 KB）
     */
    private static final int SUBSET_MIN_SIZE = 512 * 1024;

    /**
     * 超大字体提示阈值，单位：字节（20 MB）
     * <p>
     * 默认策略下字体一律原样嵌入；超过该阈值时只记录聚合告警（便于定位体积问题），
     * 不做任何隐式子集化——子集化必须由调用方显式 {@link #setSubsetFont(boolean)} 开启。
     */
    private static final int LARGE_FONT_WARN_BYTES = 20 * 1024 * 1024;

    /**
     * 扫描字体目录的最大深度，单位：层
     */
    private static final int FONT_DIR_SCAN_DEPTH = 4;

    /**
     * 聚合告警的最大条数，单位：条（超出后只计数）
     */
    private static final int MAX_WARNING_KEYS = 64;

    /**
     * 参与精确匹配的字体文件扩展名
     */
    private static final String[] FONT_SUFFIXES = {".ttf", ".otf", ".ttc", ".otc"};

    /**
     * 字体资源写入目标文档
     */
    private final OFDGraphicsDocument doc;

    /**
     * 是否开启字体子集化（默认关闭：内嵌字体原样嵌入）
     */
    private volatile boolean subsetFont = false;

    /**
     * 自定义字体目录（可为 null，只做精确匹配）
     */
    private volatile Path fontDir;

    /**
     * PDF 字体对象标识 → 登记项
     */
    private final Map<String, Registered> byFont = new ConcurrentHashMap<>();

    /**
     * 字体去重键 → 登记项
     */
    private final Map<String, Registered> byKey = new ConcurrentHashMap<>();

    /**
     * 字体名（去子集前缀）→ 已登记的字体资源 ID
     * <p>
     * 用于页面之外的资源域（例如平铺底纹单元的 CellContent）复用页面上已登记的同一字体，
     * 避免单元内字体因缺少内嵌程序而无法单独登记。
     */
    private final Map<String, ST_ID> byName = new ConcurrentHashMap<>();

    /**
     * 回退路径绘制的字体名（去子集前缀），仅用于来源汇总报告
     */
    private final Set<String> fallbackFonts = Collections.synchronizedSet(new LinkedHashSet<>());

    /**
     * 聚合告警：键（原因|字体名）→ 出现次数
     */
    private final Map<String, Integer> warnings = new LinkedHashMap<>();

    /**
     * 是否已写出字体文件
     */
    private boolean flushed = false;

    /**
     * 系统字体索引缓存（小写名称 → 字体文件）
     */
    private Map<String, List<FontEntry>> systemFontIndex;

    /**
     * 创建字体注册表
     *
     * @param doc OFD 图形文档，字体资源写入其 DocDir 的 Res 目录
     */
    public PdfFontRegistry(OFDGraphicsDocument doc) {
        if (doc == null) {
            throw new IllegalArgumentException("OFD 图形文档（doc）不能为空");
        }
        this.doc = doc;
    }

    /**
     * 设置是否开启字体子集化（可选体积优化，默认关闭）
     * <p>
     * 开启后，TrueType 轮廓且“字形总数大于使用字形数 × 4”的字体走 {@link FontSubsetTool} 子集化并裁剪表，
     * 其余字体仍原样嵌入。此时调用方必须在 {@link #flush()} 之后用 {@link #subsetGid(PDFont, int)}
     * 而不是 {@link #glyphId(PDFont, int)} 写 CGTransform。
     *
     * <p>
     * <b>限制</b>：开启后字体的 GID 会按子集重排，CGTransform 必须使用 {@link #subsetGid(PDFont, int)}
     * 给出的最终 GID；按字符编码取码位的<b>简单字体</b>还会同时开启 PDFBox 的字体子集化，
     * 为避免编码与 GID 不一致，简单字体（PDTrueTypeFont）在子集化模式下仍按原样嵌入。
     *
     * @param subsetFont true 开启子集化；false（默认）原样嵌入
     */
    public void setSubsetFont(boolean subsetFont) {
        this.subsetFont = subsetFont;
    }

    /**
     * 是否处于子集化模式
     * <p>
     * 子集化后字体的 GID 会重排，CGTransform 必须在字体写出后用 {@link #subsetGid(PDFont, int)}
     * 改写为最终 GID；调用方据此决定是否登记待改写的 CGTransform 节点。
     *
     * @return true 表示已开启子集化
     */
    public boolean isSubsetMode() {
        return subsetFont;
    }

    /**
     * 设置字体查找目录（用于 PDF 未内嵌字体时的精确匹配，<b>默认关闭</b>）
     * <p>
     * 默认行为：只使用 PDF 自带的字体程序；没有内嵌程序时直接回退路径绘制（符合“PDF 内嵌字体直接作为
     * OFD 内嵌字体”的需求）。只有显式调用本方法才启用系统字体匹配，且<b>只扫描传入的目录</b>，
     * 不会自动附加任何系统字体目录；传入 {@code null} 等于关闭匹配。
     * <p>
     * 匹配只做“字体名完全相等（去子集前缀、忽略大小写与空格）”，不做相似匹配与风格推断；
     * 匹配到的字体同样原样嵌入（不子集化）。
     *
     * @param fontDir 字体查找目录；{@code null} 表示关闭系统字体匹配
     */
    public void setFontDir(Path fontDir) {
        this.fontDir = fontDir;
        this.systemFontIndex = null;
    }

    /**
     * 字体来源类型（供验证脚手架与报告消费）
     */
    public enum FontSourceType {
        /**
         * PDF 内嵌字体原样嵌入（含 TrueType、OpenType/CFF、TTC 提取）
         */
        EMBEDDED_VERBATIM("embedded-verbatim"),
        /**
         * 内嵌字体为 TTC 集合，已提取其中一个字体后嵌入
         */
        TTC_EXTRACTED("ttc-extracted"),
        /**
         * 非内嵌字体，按字体目录精确匹配到系统字体后嵌入
         */
        SYSTEM_MATCHED("system-matched"),
        /**
         * 无可嵌入字体，回退路径绘制
         */
        PATH_FALLBACK("path-fallback");

        /**
         * 日志/报告中的来源标签
         */
        private final String label;

        FontSourceType(String label) {
            this.label = label;
        }

        /**
         * 获取来源标签
         *
         * @return 来源标签
         */
        public String getLabel() {
            return label;
        }
    }

    /**
     * 单个字体的来源汇总信息
     */
    public static final class FontSourceInfo {
        /**
         * OFD 字体资源 ID（回退路径时为 -1）
         */
        public final long fontResId;
        /**
         * 字体名（去子集前缀的 PostScript 名）
         */
        public final String fontName;
        /**
         * 来源类型
         */
        public final FontSourceType source;
        /**
         * 实际写入 OFD 的字体字节数，单位：字节
         */
        public final long fontBytes;
        /**
         * 是否经过子集化
         */
        public final boolean subset;
        /**
         * 字体程序格式（ttf/otf 等扩展名）
         */
        public final String fileFormat;

        FontSourceInfo(long fontResId, String fontName, FontSourceType source, long fontBytes,
                       boolean subset, String fileFormat) {
            this.fontResId = fontResId;
            this.fontName = fontName;
            this.source = source;
            this.fontBytes = fontBytes;
            this.subset = subset;
            this.fileFormat = fileFormat;
        }

        @Override
        public String toString() {
            return fontName + " [" + source.getLabel() + "] " + fontBytes + "B"
                    + (subset ? " (subset)" : "") + " " + fileFormat;
        }
    }

    /**
     * 获取已登记字体的来源汇总（按登记顺序），供验证脚手架与报告消费
     * <p>
     * 字体字节数在 {@link #flush()} 之后才最终确定；未 flush 时返回原样嵌入的字节数。
     *
     * @return 来源汇总列表（只读快照）
     */
    public synchronized java.util.List<FontSourceInfo> getFontSources() {
        java.util.List<FontSourceInfo> list = new java.util.ArrayList<>();
        for (Registered registered : byKey.values()) {
            long resId = registered.resId == null ? -1 : registered.resId.getId();
            long bytes = registered.writtenBytes > 0 ? registered.writtenBytes : registered.rawData.length;
            list.add(new FontSourceInfo(resId, registered.postScriptName, sourceTypeOf(registered),
                    bytes, registered.subset, registered.subset ? ".ttf" : registered.rawSuffix));
        }
        return list;
    }

    /**
     * 记录一个回退路径绘制的字体
     *
     * @param name PDF 字体名
     */
    private void recordFallback(String name) {
        fallbackFonts.add(stripSubsetPrefix(name));
    }

    /**
     * 获取回退路径绘制的字体名列表
     *
     * @return 只读快照
     */
    public java.util.List<String> getFallbackFonts() {
        return new java.util.ArrayList<>(fallbackFonts);
    }

    /**
     * 把内部来源枚举转换为对外来源类型
     *
     * @param registered 登记项
     * @return 来源类型
     */
    private static FontSourceType sourceTypeOf(Registered registered) {
        switch (registered.source) {
            case TTC_EXTRACTED:
                return FontSourceType.TTC_EXTRACTED;
            case SYSTEM_MATCHED:
                return FontSourceType.SYSTEM_MATCHED;
            case PATH_FALLBACK:
                return FontSourceType.PATH_FALLBACK;
            case EMBEDDED_VERBATIM:
            default:
                return FontSourceType.EMBEDDED_VERBATIM;
        }
    }

    /**
     * 登记 PDF 字体
     * <p>
     * 首次登记时立即读取并缓存字体程序字节（PDF 文档关闭后仍可写出），之后同一字体对象直接命中去重表。
     * 无法嵌入（Type3、无字体程序且系统字体精确匹配失败、字体程序损坏等）时返回 null，调用方应回退路径绘制。
     *
     * @param font PDF 字体
     * @return OFD 字体资源 ID；无法嵌入时返回 null
     */
    /**
     * 按字体名查找已登记的字体资源 ID
     * <p>
     * 供页面之外的资源域（例如平铺底纹单元的文字）复用同一字体；
     * 字体名比较时忽略子集前缀（{@code ABCDEF+}）。
     *
     * @param fontName 字体名（可带子集前缀）
     * @return 字体资源 ID；未登记时返回 null
     */
    /**
     * 登记“平铺底纹/水印单元”里出现的字体
     * <p>
     * 与 {@link #registerFont(PDFont)} 的差别：单元字体常常没有内嵌程序（水印文字随注释走），
     * 因此允许在 {@code fontDir} 已配置时用系统字体匹配替代；正文渲染仍走严格策略，
     * 避免整个文档被替换成完整 CJK 字体导致体积暴涨。
     *
     * @param font PDF 字体
     * @return 字体资源 ID；无法登记时返回 null
     */
    public ST_ID registerAnnotationFont(PDFont font) {
        return registerAnnotationFont(font, null);
    }

    /**
     * 登记注释单元字体，并按用到的 Unicode 反查系统字体 GID 用于子集化
     * <p>
     * 系统匹配得到的完整 CJK 字体（如 18MB 的 SimSun）只有把**实际用到的字形**登记到子集依据里，
     * 才能被裁剪成 KB 级子集。难点在于：PDF 的 GID（CIDToGIDMap 体系）与系统字体的 GID 完全不同，
     * 直接沿用会把错误的字形加入子集。这里按 Unicode 在系统字体的 cmap 中反查真实 GID 后再登记。
     *
     * @param font         PDF 字体
     * @param usedUnicodes 单元内实际用到的 Unicode 码位集合，可为 null
     * @return 字体资源 ID；无法登记时返回 null
     */
    public ST_ID registerAnnotationFont(PDFont font, Set<Integer> usedUnicodes) {
        if (font == null) {
            return null;
        }
        String identity = identityOf(font);
        Registered existing = byFont.get(identity);
        if (existing != null) {
            // 已有登记项也要补登记本次用到的字形，否则系统字体永远凑不齐子集依据
            addSystemMatchedGlyphs(existing, usedUnicodes);
            return existing.resId;
        }
        synchronized (this) {
            existing = byFont.get(identity);
            if (existing != null) {
                addSystemMatchedGlyphs(existing, usedUnicodes);
                return existing.resId;
            }
            Registered registered = resolve(font, true);
            if (registered == null) {
                byFont.put(identity, new Registered(null));
                return null;
            }
            Registered same = byKey.get(registered.dedupeKey);
            if (same != null) {
                // 同一字体程序已经登记过：补充本次用到的字形（系统字体需要按 Unicode 反查 GID）
                addSystemMatchedGlyphs(same, usedUnicodes);
                registered.resId = same.resId;
                registered.ctFont = same.ctFont;
                byFont.put(identity, registered);
                return registered.resId;
            }
            addSystemMatchedGlyphs(registered, usedUnicodes);
            registered.ctFont = buildFontElement(registered);
            registered.resId = doc.resMgr.addRawWithCache(registered.ctFont);
            byKey.put(registered.dedupeKey, registered);
            byFont.put(identity, registered);
            if (registered.resId != null) {
                byName.putIfAbsent(baseFontName(registered.fontName), registered.resId);
            }
            return registered.resId;
        }
    }

    /**
     * 把“实际用到的 Unicode”按系统字体的 cmap 反查成 GID 并登记为子集依据
     * <p>
     * 只对来源为 {@link Source#SYSTEM_MATCHED} 的登记项生效：这类字体的 GID 体系与 PDF 无关，
     * 必须重新反查；PDF 内嵌字体仍沿用 PDF 的原始 GID。
     *
     * @param registered   登记项
     * @param usedUnicodes 用到的 Unicode 码位，可为 null
     */
    private void addSystemMatchedGlyphs(Registered registered, Set<Integer> usedUnicodes) {
        if (registered == null || registered.source != Source.SYSTEM_MATCHED
                || usedUnicodes == null || usedUnicodes.isEmpty() || registered.rawData == null) {
            return;
        }
        try {
            org.apache.fontbox.ttf.TrueTypeFont ttf =
                    new org.apache.fontbox.ttf.TTFParser().parse(
                            new org.apache.pdfbox.io.RandomAccessReadBuffer(registered.rawData));
            try {
                org.apache.fontbox.ttf.CmapLookup cmap = ttf.getUnicodeCmapLookup();
                if (cmap == null) {
                    return;
                }
                for (Integer unicode : usedUnicodes) {
                    if (unicode == null || unicode < 0) {
                        continue;
                    }
                    int gid = cmap.getGlyphId(unicode);
                    if (gid > 0) {
                        registered.usedGids.add(gid);
                        registered.usedUnicodes.add(unicode);
                    }
                }
            } finally {
                ttf.close();
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("系统字体 GID 反查失败：{}", e.toString());
        }
    }

    public ST_ID findRegisteredByName(String fontName) {
        return fontName == null ? null : byName.get(baseFontName(fontName));
    }

    /**
     * 去掉字体名的子集前缀
     *
     * @param fontName 原始字体名
     * @return 去掉 {@code ABCDEF+} 前缀后的名称
     */
    private static String baseFontName(String fontName) {
        if (fontName == null) {
            return "";
        }
        int plus = fontName.indexOf('+');
        return plus == 7 ? fontName.substring(8) : fontName;
    }

    public ST_ID registerFont(PDFont font) {
        if (font == null) {
            return null;
        }
        String identity = identityOf(font);
        Registered existing = byFont.get(identity);
        if (existing != null) {
            return existing.resId;
        }
        synchronized (this) {
            existing = byFont.get(identity);
            if (existing != null) {
                return existing.resId;
            }
            Registered registered = resolve(font);
            if (registered == null) {
                // 失败也记入缓存，避免同一字体反复尝试与反复告警
                byFont.put(identity, new Registered(null));
                return null;
            }
            Registered same = byKey.get(registered.dedupeKey);
            if (same != null) {
                registered.resId = same.resId;
                registered.ctFont = same.ctFont;
                byFont.put(identity, registered);
                return registered.resId;
            }
            registered.ctFont = buildFontElement(registered);
            registered.resId = doc.resMgr.addRawWithCache(registered.ctFont);
            byKey.put(registered.dedupeKey, registered);
            byFont.put(identity, registered);
            if (registered.resId != null) {
                byName.putIfAbsent(baseFontName(registered.fontName), registered.resId);
            }
            return registered.resId;
        }
    }

    /**
     * 取 PDF 字体中 code 对应的原始 GID
     *
     * @param font PDF 字体
     * @param code PDF 字符编码（内容流中的字节值）
     * @return GID（PDF 原始字形索引）；不支持的字体类型或取不到时返回 -1
     */
    public int glyphId(PDFont font, int code) {
        if (font == null) {
            return -1;
        }
        try {
            if (font instanceof PDType0Font) {
                return ((PDType0Font) font).codeToGID(code);
            }
            if (font instanceof PDTrueTypeFont) {
                return ((PDTrueTypeFont) font).codeToGID(code);
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("取字形索引失败：code={}，字体={}", code, safeName(font), e);
        }
        return -1;
    }

    /**
     * 记录该字体实际使用的一个字形及其 Unicode
     * <p>
     * 默认（原样嵌入）只用于统计与告警；开启子集化后作为子集与 cmap 重建的依据。
     *
     * @param font    PDF 字体
     * @param code    PDF 字符编码（内容流中的字节值）
     * @param gid     PDF 原始 GID
     * @param unicode 该字形对应的 Unicode 码位；未知时传负数
     */
    public void addUsedGlyph(PDFont font, int code, int gid, int unicode) {
        if (font == null) {
            return;
        }
        /* 字形使用记录必须落到“字体资源”上：
         * 文字对象化时 PDFBox 的 PageDrawer 每次渲染都会新建 PDFont 对象，同一个字体程序对应多个 PDFont 实例，
         * 按 PDFont 对象身份查找会查不到登记项（子集化时丢失使用字形）。这里按字体程序哈希回查登记项。 */
        Registered registered;
        synchronized (this) {
            registered = byFont.get(identityOf(font));
            if (registered == null) {
                registerFont(font);
                registered = byFont.get(identityOf(font));
            }
        }
        if (registered == null || registered.rawData == null) {
            return;
        }
        registered.usedGids.add(sanitizeGid(gid));
        if (unicode >= 0) {
            registered.usedUnicodes.add(unicode);
        }
    }

    /**
     * 规整 GID：负数按 0（.notdef）处理，超大值忽略
     *
     * @param gid PDF 原始 GID
     * @return 可加入集合的 GID，单位为个
     */
    private static int sanitizeGid(int gid) {
        return gid < 0 ? 0 : gid;
    }

    /**
     * 判断该字体是否会被重新子集化
     *
     * @param font PDF 字体
     * @return true 表示 flush() 后 GID 会重排，CGTransform 必须使用 {@link #subsetGid(PDFont, int)}
     */
    public boolean isSubset(PDFont font) {
        Registered registered = font == null ? null : byFont.get(identityOf(font));
        return registered != null && registered.subset;
    }

    /**
     * 把 PDF 原始 GID 换算为最终写入 OFD 字体文件的 GID（CGTransform 使用）
     * <p>
     * 原样嵌入（默认）时恒等；子集化后为子集字体中的新 GID。须在 {@link #flush()} 之后调用，
     * 此前子集尚未生成，返回的是原始 GID。
     *
     * @param font   PDF 字体
     * @param pdfGid PDF 原始 GID
     * @return 最终 GID；字体未登记或映射缺失时返回 0（.notdef）
     */
    public int subsetGid(PDFont font, int pdfGid) {
        Registered registered = font == null ? null : byFont.get(identityOf(font));
        if (registered == null) {
            return pdfGid;
        }
        if (registered.subsetOldToNew == null) {
            return pdfGid;
        }
        Integer newGid = registered.subsetOldToNew.get(pdfGid);
        return newGid == null ? 0 : newGid;
    }

    /**
     * 写出全部字体文件并回填 {@code CT_Font.FontFile}
     * <p>
     * 幂等，可多次调用；必须在 {@code OFDGraphicsDocument#close()} 之前调用。
     * 单个字体写出失败时回退为整字体嵌入，仍失败则移除 {@code FontFile}（共享同一资源的其它字体不受影响）。
     */
    public synchronized void flush() {
        if (flushed) {
            return;
        }
        flushed = true;
        for (Registered registered : new LinkedHashSet<>(byKey.values())) {
            if (registered.rawData == null || registered.resId == null) {
                continue;
            }
            try {
                writeFontFile(registered);
            } catch (IOException | RuntimeException e) {
                warn("写出字体文件失败|" + registered.fontName, e.getMessage());
                // 回退：清空子集状态，重新以整字体写出
                registered.subset = false;
                registered.subsetOldToNew = null;
                registered.suffix = registered.rawSuffix;
                try {
                    writeFontFile(registered);
                } catch (IOException | RuntimeException again) {
                    logger.debug("整字体嵌入仍然失败：{}", registered.fontName, again);
                    registered.ctFont.removeOFDElemByNames("FontFile");
                }
            }
        }
        flushWarnings();
    }

    /**
     * 关闭注册表：先写出字体文件（等价于 {@link #flush()}）
     * <p>
     * 字体失败一律降级为 warning，本方法不会抛出 IOException；签名保留以支持 try-with-resources。
     */
    @Override
    public void close() {
        flush();
    }

    /**
     * 把单个字体的数据写入资源目录并回填 FontFile
     *
     * @param registered 登记项
     * @throws IOException 字体数据不可用或容器写入失败
     */
    private void writeFontFile(Registered registered) throws IOException {
        byte[] data = decideFontData(registered);
        if (data.length > LARGE_FONT_WARN_BYTES) {
            // 默认（原样嵌入）遇到超大字体只记录聚合告警，不做任何隐式子集化：
            // 是否子集化完全由 setSubsetFont(true) 决定（见 decideFontData）
            warn("字体体积超过提示阈值（默认原样嵌入，未子集化）|" + registered.fontName,
                    data.length + " 字节，超过 " + LARGE_FONT_WARN_BYTES + " 字节");
        }
        ResDir resDir = doc.docDir.obtainRes();
        ST_Loc loc = resDir.getAbsLoc().cat(registered.fontName + registered.suffix);
        resDir.putFile(registered.fontName + registered.suffix, data);
        registered.writtenBytes = data.length;
        registered.ctFont.setFontFile(loc);
        logger.debug("字体 {} 写入 {}，{} 字节{}", registered.fontName, loc, data.length,
                registered.subset ? "（已子集化）" : "（原样嵌入）");
    }

    /**
     * 决定最终写入的字体数据：原样嵌入，或（开启子集化且满足阈值时）子集化
     *
     * @param registered 登记项
     * @return 字体数据，单位：字节
     * @throws IOException 字体数据不可用
     */
    private byte[] decideFontData(Registered registered) throws IOException {
        /*
         * 系统字体匹配得到的字体必然是大体积的完整 CJK 字体，而实际使用的字形往往只有寥寥几个
         * （例如水印单元里的“严禁复制”四个字）。这种情况下必须子集化，否则单个水印就会把
         * 十几 MB 的系统字体整个塞进 OFD（实测 492KB → 10.1MB）。
         * 该行为只针对 system-matched 来源，PDF 内嵌字体仍遵循"默认原样嵌入"的既定策略。
         */
        /*
         * 来自系统字体目录的字体（system-matched，或从其 TTC 中提取的 ttc-extracted）
         * 必然是大体积完整 CJK 字体，实际使用的字形往往只有几个，必须子集化，
         * 否则单个水印就会把十几 MB 塞进 OFD。PDF 内嵌字体仍遵循"默认原样嵌入"策略。
         */
        boolean fromSystemFont = registered.source == Source.SYSTEM_MATCHED
                || registered.source == Source.TTC_EXTRACTED;
        boolean forceSubset = fromSystemFont && isWorthSubsetting(registered);
        if (!forceSubset && (!subsetFont || !isWorthSubsetting(registered))) {
            registered.subset = false;
            registered.subsetOldToNew = null;
            registered.suffix = registered.rawSuffix;
            return registered.rawData;
        }
        byte[] subset = trySubset(registered);
        if (subset != null) {
            return subset;
        }
        registered.subset = false;
        registered.subsetOldToNew = null;
        registered.suffix = registered.rawSuffix;
        return registered.rawData;
    }

    /**
     * 按实际使用字形生成子集字体（失败时记录聚合警告并返回 null）
     *
     * @param registered 登记项
     * @return 子集字体数据；失败返回 null
     */
    private byte[] trySubset(Registered registered) {
        try {
            FontSubsetTool.Result result = FontSubsetTool.subset(
                    registered.rawData, registered.usedGids, registered.usedUnicodes);
            registered.subset = true;
            registered.subsetOldToNew = result.oldToNew;
            registered.suffix = ".ttf";
            return result.data;
        } catch (IOException | RuntimeException e) {
            warn("子集化失败，回退整字体嵌入|" + registered.fontName, e.getMessage());
            return null;
        }
    }

    /**
     * 判断字体是否值得子集化（设计文档 5.6.3）
     * <p>
     * 两个条件分开判断，避免“小字体但字形数很多”的字体被体积阈值一刀切掉：
     * <ol>
     *     <li><b>可子集</b>：TrueType 轮廓（glyf/loca），且已记录实际使用的字形；</li>
     *     <li><b>有收益</b>：字形总数大于使用字形数 × {@link #SUBSET_GLYPH_RATIO}（字体已含大量未使用字形），
     *     或字体体积大于 {@link #SUBSET_MIN_SIZE}（体积本身可观，子集能明显减小输出）。</li>
     * </ol>
     * 典型收益场景：小体积但字形齐全的字体（Signout 的 EvermoreKai 341 KB / 42,204 字形，
     * Signout 只用到 51 个文字对象）—— 体积阈值会误判为“小字体不子集”，因此改为“等比例压缩判断”。
     *
     * @param registered 登记项
     * @return true 表示建议子集化
     */
    private boolean isWorthSubsetting(Registered registered) {
        if (registered.rawData == null || registered.usedGids.isEmpty()) {
            return false;
        }
        // 只有 TrueType 轮廓（glyf/loca）才能用 TTFSubsetter 子集化；CFF/OTF 原样嵌入
        if (!FontFileFormat.hasTable(registered.rawData, 0, "glyf")
                || !FontFileFormat.hasTable(registered.rawData, 0, "loca")) {
            return false;
        }
        boolean bigFont = registered.rawData.length > SUBSET_MIN_SIZE;
        int usedGlyphs = registered.usedGids.size() + 1;
        if (registered.numGlyphs <= 0) {
            // 无法读取字形数：只按体积判断
            return bigFont;
        }
        if (registered.numGlyphs <= usedGlyphs) {
            // 实际使用字形已覆盖全部字形（如整页 CJK 文档用满字体），子集化无收益
            return false;
        }
        boolean manyUnused = registered.numGlyphs > usedGlyphs * SUBSET_GLYPH_RATIO;
        return bigFont || manyUnused;
    }

    /**
     * 解析 PDF 字体，决定字体来源并准备登记项（不写文件）
     *
     * @param font PDF 字体
     * @return 登记项；无法嵌入时返回 null
     */
    private Registered resolve(PDFont font) {
        return resolve(font, false);
    }

    /**
     * 解析并登记 PDF 字体（带“是否允许系统字体兜底”的开关）
     * <p>
     * 系统字体兜底有三道门槛，避免把所有无内嵌字体的文档都替换成完整 CJK 字体（体积会暴涨数十倍）：
     * <ol>
     *     <li>{@code fontDir} 已显式配置（默认关闭，绝不自动扫描系统目录）；</li>
     *     <li>{@code allowSystemFallback} 为真 —— 只有“注释/底纹单元的字体”这类页面外资源域才放行；</li>
     *     <li>PDF 内嵌程序<b>缺失</b>，或<b>存在但不完整</b>（例如缺 cmap 表）。</li>
     * </ol>
     * 正文里“整个文档都没有内嵌字体”的样本（hello 系列等）不在放行范围内，继续走路径回退。
     *
     * @param font                PDF 字体
     * @param allowSystemFallback 是否允许在字体程序不可用时用系统字体替代
     * @return 登记项；无法嵌入时返回 null
     */
    Registered resolve(PDFont font, boolean allowSystemFallback) {
        String name = safeName(font);
        PDFontDescriptor pdDescriptor = font.getFontDescriptor();
        // PDType0Font 的字体程序挂在后代 CIDFont 上
        PDCIDFont fontProgramOwner = font instanceof PDType0Font ? ((PDType0Font) font).getDescendantFont() : null;
        byte[] data = readEmbeddedProgram(font, fontProgramOwner, name);
        Source source = Source.EMBEDDED_VERBATIM;
        boolean fromSystem = false;
        int ttcIndex = 0;
        // 内嵌程序存在但不完整（缺 cmap 等）时同样视为“不可用”
        boolean incomplete = data != null && !isFontComplete(data);
        if (incomplete) {
            warn("内嵌字体缺少必要表结构|" + name, "缺少 cmap 等表，按字体不可用处理");
            data = null;
        }
        if (data == null) {
            boolean permitted = allowSystemFallback && fontDir != null;
            Path systemFont = permitted ? matchSystemFont(font) : null;
            if (systemFont == null) {
                warn("没有可嵌入字体且系统字体未匹配|" + name,
                        Source.PATH_FALLBACK.getLabel() + "：回退路径绘制");
                recordFallback(name);
                return null;
            }
            try {
                data = Files.readAllBytes(systemFont);
            } catch (IOException e) {
                warn("读取系统字体失败|" + name, e.getMessage());
                return null;
            }
            fromSystem = true;
            source = Source.SYSTEM_MATCHED;
            ttcIndex = ttcIndexOf(systemFont.getFileName().toString());
            warn("使用系统字体匹配替代不可用的内嵌字体|" + name, systemFont.toString());
        }

        Registered registered;
        try {
            registered = prepare(name, data, ttcIndex, pdDescriptor, source);
        } catch (IOException | RuntimeException e) {
            warn("字体程序无法解析|" + name, Source.PATH_FALLBACK.getLabel() + "：" + e.getMessage());
            return null;
        }
        if (fromSystem) {
            registered.numGlyphs = Math.max(registered.numGlyphs, 1);
        }
        return registered;
    }

    /**
     * 判断字体程序是否完整可用
     * <p>
     * 只做结构层面的最小校验：TrueType/OpenType 必须具备 {@code glyf}/{@code CFF } 轮廓表，
     * 且必须具备 {@code cmap}（查看器按字符码定位字形依赖它）；缺少 {@code cmap} 的字体
     * 在 OFD 中表现为整段文字不可见（实测 dzbh 的 SimSun 子集即缺 cmap）。
     * 裸 CFF（{@code \x01\x00\x04\x01} 开头）没有 sfnt 表目录，直接视为完整，交由后续流程处理。
     *
     * @param data 字体程序字节
     * @return true - 结构完整；false - 缺少必要表
     */
    static boolean isFontComplete(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }
        // 裸 CFF 字体程序
        if (data.length >= 4 && (data[0] & 0xFF) == 0x01 && (data[1] & 0xFF) == 0x00
                && (data[2] & 0xFF) == 0x04) {
            return true;
        }
        int tag = ((data[0] & 0xFF) << 24) | ((data[1] & 0xFF) << 16) | ((data[2] & 0xFF) << 8) | (data[3] & 0xFF);
        boolean sfnt = tag == 0x00010000 || tag == 0x4F54544F || tag == 0x74727565;
        if (!sfnt) {
            return false;
        }
        int numTables = ((data[4] & 0xFF) << 8) | (data[5] & 0xFF);
        if (numTables <= 0 || 12 + numTables * 16 > data.length) {
            return false;
        }
        boolean hasCmap = false;
        boolean hasOutline = false;
        for (int i = 0; i < numTables; i++) {
            int off = 12 + i * 16;
            int t = ((data[off] & 0xFF) << 24) | ((data[off + 1] & 0xFF) << 16)
                    | ((data[off + 2] & 0xFF) << 8) | (data[off + 3] & 0xFF);
            if (t == 0x636D6170) {
                hasCmap = true;
            } else if (t == 0x676C7966 || t == 0x43464620) {
                hasOutline = true;
            }
        }
        return hasCmap && hasOutline;
    }

    /**
     * 读取 PDF 内嵌字体程序字节
     *
     * @param font            PDF 字体（用于判断类型）
     * @param fontProgramOwner 挂载字体程序的字体对象（Type0 时为后代 CIDFont）
     * @param name            日志用字体名
     * @return 字体程序字节；无内嵌程序（Type3、未内嵌且无系统字体匹配）时返回 null
     * <p>
     * FontFile2 / FontFile3 / FontFile 一律原样返回：TrueType、OpenType（CFF 轮廓）、裸 CFF、
     * 乃至 TTC 都由 {@link #prepare} 统一识别与处理，不再按“后代字体类型”拒绝复合字体。
     */
    private byte[] readEmbeddedProgram(PDFont font, PDCIDFont fontProgramOwner, String name) {
        PDFontDescriptor descriptor = fontProgramOwner != null
                ? fontProgramOwner.getFontDescriptor() : font.getFontDescriptor();
        if (descriptor == null) {
            return null;
        }
        PDStream stream = descriptor.getFontFile2();
        boolean fontFile2 = stream != null;
        if (stream == null) {
            stream = descriptor.getFontFile3();
        }
        if (stream == null) {
            stream = descriptor.getFontFile();
        }
        if (stream == null) {
            return null;
        }
        try {
            byte[] data = readAll(stream);
            if (data.length < 4) {
                warn("字体程序损坏|" + name, "长度 " + data.length + " 字节");
                return null;
            }
            if (fontFile2 && data.length < 12) {
                warn("FontFile2 长度不足|" + name, "长度 " + data.length + " 字节");
                return null;
            }
            return data;
        } catch (IOException | RuntimeException e) {
            warn("读取内嵌字体失败|" + name, e.getMessage());
            return null;
        }
    }

    /**
     * 读取裸 CFF 的字形数量（取 CharStrings INDEX 的元素个数）
     *
     * @param cff 裸 CFF 数据
     * @return 字形数量，单位为个；解析失败返回 1（至少保留 .notdef）
     */
    private static int readBareCffNumGlyphs(byte[] cff) {
        try {
            // 头部：major(1) minor(1) hdrSize(1) offSize(1)，之后依次为 Name INDEX、Top DICT INDEX、String INDEX、Global Subr INDEX
            int hdrSize = cff[2] & 0xFF;
            int pos = hdrSize;
            pos = cffIndexEnd(cff, pos);        // Name INDEX
            int topDictEnd = cffIndexEnd(cff, pos);
            pos = topDictEnd;                   // Top DICT INDEX
            pos = cffIndexEnd(cff, pos);        // String INDEX
            pos = cffIndexEnd(cff, pos);        // Global Subr INDEX
            // CharStrings INDEX 起点的精确位置取决于 Top DICT 的 CS 操作符，这里退化为：
            // 从 Global Subr 之后开始扫描，取第一个“元素数 > 0 且长度合理”的 INDEX（即 CharStrings）
            int count = cffIndexCount(cff, pos);
            if (count > 0) {
                return count;
            }
        } catch (RuntimeException e) {
            logger.debug("读取裸 CFF 字形数失败", e);
        }
        return 1;
    }

    /**
     * 读取裸 CFF 的字形名（来自 CIDFont 的 Encoding/ToUnicode 不可用时返回 null）
     * <p>
     * PDF 的 Type1C 字体程序通常不带字形名（CID-keyed CFF 只有 CID），此时返回 null，
     * 包装出的 OpenType 不带 cmap，查看器必须依赖 CGTransform（文字对象化总会写 CGTransform）。
     *
     * @param cff          裸 CFF 数据
     * @param numGlyphs    字形数量，单位为个
     * @param pdDescriptor PDF 字体描述符（CIDToGIDMap 等情况）
     * @return 每个字形的字形名数组；无可解析字形名时返回 null
     */
    private static String[] readBareCffGlyphNames(byte[] cff, int numGlyphs, PDFontDescriptor pdDescriptor) {
        // 裸 CFF 的 String INDEX 需要解析 Top DICT 才能定位，考虑成本与收益，这里不解析；
        // CID-keyed CFF 本身没有字形名，返回 null 由调用方依赖 CGTransform。
        return null;
    }

    /**
     * 跳过 CFF INDEX 结构，返回下一个结构的起始偏移
     *
     * @param data CFF 数据
     * @param pos  当前偏移，单位为字节
     * @return 下一个结构的起始偏移，单位为字节
     */
    private static int cffIndexEnd(byte[] data, int pos) {
        int count = SfntTable.readU16(data, pos);
        if (count == 0) {
            return pos + 2;
        }
        int offSize = data[pos + 2] & 0xFF;
        int offsetsEnd = pos + 3 + (count + 1) * offSize;
        long dataLen = readCffOffset(data, offsetsEnd - offSize, offSize);
        return (int) (offsetsEnd + dataLen - 1);
    }

    /**
     * 读取 CFF INDEX 的元素个数
     *
     * @param data CFF 数据
     * @param pos  INDEX 起始偏移，单位为字节
     * @return 元素个数
     */
    private static int cffIndexCount(byte[] data, int pos) {
        return SfntTable.readU16(data, pos);
    }

    /**
     * 读取 CFF INDEX 的偏移量字段
     *
     * @param data    数据
     * @param pos     起始位置，单位为字节
     * @param offSize 偏移量字节数，取值 1 ~ 4
     * @return 偏移量
     */
    private static long readCffOffset(byte[] data, int pos, int offSize) {
        long value = 0;
        for (int i = 0; i < offSize; i++) {
            value = (value << 8) | (data[pos + i] & 0xFF);
        }
        return value;
    }

    /**
     * 按字体数据准备登记项：格式识别、TTC 提取、裸 CFF 包装、字体属性提取、去重键计算
     *
     * @param name         日志用字体名（BaseFont，已去子集前缀）
     * @param data         字体程序字节
     * @param ttcIndex     TTC 索引，单位：个
     * @param pdDescriptor PDF 字体描述符，可为 null（系统字体替代时仍取原 PDF 描述符）
     * @param source       字体来源标注（写入日志便于排查）
     * @return 登记项
     * @throws IOException 字体数据无法作为 OFD 字体文件
     */
    private Registered prepare(String name, byte[] data, int ttcIndex, PDFontDescriptor pdDescriptor,
                               Source source) throws IOException {
        FontFileFormat format = FontFileFormat.detect(data);
        if (format == FontFileFormat.Type1) {
            throw new IOException("Type1（PFB）不是 OpenType，不能作为 OFD 字体文件");
        }
        byte[] sfnt = data;
        int index = 0;
        if (format == FontFileFormat.Collection) {
            index = ttcIndex;
            sfnt = FontFileFormat.toSfnt(data, index);
            format = FontFileFormat.detect(sfnt);
            source = Source.TTC_EXTRACTED;
        }
        if (CffFontData.isBareCff(sfnt)) {
            /*
             * 裸 CFF（FontFile3 / Subtype=Type1C）：PDF 里没有 sfnt 表目录，也不带 cmap，
             * 这里补一个最小 OpenType 外壳，并用“字形名 → Unicode”合成 cmap（ADR-211 规则：
             * uniXXXX / uXXXX / 单字符名 / 常见标点名），使查看器无需 CGTransform 也能定位字形。
             */
            int bareGlyphs = readBareCffNumGlyphs(sfnt);
            String[] glyphNames = readBareCffGlyphNames(sfnt, bareGlyphs, pdDescriptor);
            sfnt = CffFontData.wrap(sfnt, Math.max(1, bareGlyphs), new int[Math.max(1, bareGlyphs)], null, name, glyphNames);
            format = FontFileFormat.OpenTypeCff;
            logger.debug("裸 CFF 已包装为 OpenType：{} 个字形、{} 个可合成 cmap 的字形名", bareGlyphs,
                    glyphNames == null ? 0 : glyphNames.length);
        }
        if (!format.isOpenType()) {
            throw new IOException("字体格式 " + format + " 不是 OpenType，不能作为 OFD 字体文件");
        }
        // 字体名取字体程序 name 表的 PostScript 名（去子集前缀），与 OFD FontName 保持一致
        String postScriptName = postScriptNameOf(sfnt, name);
        String hash = sha256(sfnt);
        Registered registered = new Registered(hash + "#" + index + "#" + (subsetFont ? "subset" : "raw"));
        registered.rawData = sfnt;
        registered.rawSuffix = format.getSuffix();
        registered.suffix = registered.rawSuffix;
        // 资源文件名追加字体程序哈希前缀，避免不同字体程序同名覆盖
        registered.fontName = postScriptName + "-" + hash.substring(0, 8);
        registered.postScriptName = postScriptName;
        registered.familyName = fontFamilyOf(pdDescriptor, postScriptName);
        registered.bold = isBold(pdDescriptor, postScriptName);
        registered.italic = isItalic(pdDescriptor, postScriptName);
        registered.serif = isSerif(pdDescriptor);
        registered.fixedWidth = isFixedWidth(pdDescriptor);
        registered.numGlyphs = readNumGlyphs(sfnt);
        registered.source = source;
        logger.debug("字体 {} 来源 {}，{} 字节，{} 个字形", postScriptName, source.getLabel(), sfnt.length, registered.numGlyphs);
        return registered;
    }

    /**
     * 按登记项构造 {@code CT_Font} 元素（FontFile 在 flush() 回填）
     * <p>
     * 字段约定（设计文档 5.6.1）：FontName 去子集前缀；FamilyName 为字体族名（为空时退回 FontName）；
     * Charset=unicode；Bold/Italic/Serif/FixedWidth 来自 PDF 字体描述符。
     *
     * @param registered 登记项
     * @return 字体元素
     */
    private static CT_Font buildFontElement(Registered registered) {
        return new CT_Font()
                .setFontName(registered.postScriptName)
                .setFamilyName(registered.familyName)
                .setCharset(Charset.unicode)
                .setBold(registered.bold)
                .setItalic(registered.italic)
                .setSerif(registered.serif)
                .setFixedWidth(registered.fixedWidth);
    }

    /**
     * 读取字体程序流的全部字节
     *
     * @param stream PDF 字体程序流
     * @return 字节数据
     * @throws IOException 读取失败
     */
    private static byte[] readAll(PDStream stream) throws IOException {
        try (COSInputStream in = stream.createInputStream()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) != -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        }
    }

    /**
     * 在系统/配置字体目录中精确匹配字体（名称 + 字重 + 斜体）
     * <p>
     * 匹配规则（设计文档 5.6.2 第 3 级）：
     * <ol>
     *     <li>名称键：FontName、去子集前缀的 FontName、FontFamily、去子集前缀的 FontFamily，
     *     与字体文件名（去扩展名，忽略 {@code #索引}）、PostScript 名、字族名做 {@link String#equalsIgnoreCase}
     *     比较（忽略空格），不做相似匹配；</li>
     *     <li>同一名称命中多个候选（Regular/Bold/Italic/BoldItalic）时，按“字重一致 + 斜体一致”筛选；</li>
     *     <li>名称完全相同（如 {@code simsunb}、{@code DejaVuSans-Bold}）的候选直接采用，不再按字重/斜体过滤。</li>
     * </ol>
     *
     * @param font PDF 字体
     * @return 字体文件路径；未匹配时返回 null
     */
    private Path matchSystemFont(PDFont font) {
        if (fontDir == null) {
            // 未显式配置字体目录：不查系统字体，交给调用方回退路径绘制
            return null;
        }
        PDFontDescriptor descriptor = font.getFontDescriptor();
        Set<String> keys = new LinkedHashSet<>();
        collectKey(keys, font.getName());
        if (descriptor != null) {
            collectKey(keys, descriptor.getFontName());
            collectKey(keys, descriptor.getFontFamily());
        }
        if (keys.isEmpty()) {
            return null;
        }
        boolean wantBold = isBold(descriptor, font.getName());
        boolean wantItalic = isItalic(descriptor, font.getName());
        Map<String, List<FontEntry>> index = systemFontIndex();
        for (String key : keys) {
            List<FontEntry> candidates = index.get(key);
            if (candidates == null) {
                continue;
            }
            List<FontEntry> matched = filterByStyle(candidates, wantBold, wantItalic);
            if (!matched.isEmpty()) {
                return matched.get(0).path;
            }
        }
        return null;
    }

    /**
     * 按字重与斜体筛选候选字体
     * <p>
     * 名称键命中可能有多种情况：
     * <ul>
     *     <li>候选本身就用完整名称索引（如 {@code simsunb}、{@code DejaVuSans-Bold}）：直接采用；</li>
     *     <li>候选来自字族名（如 {@code DejaVuSans} 同时索引 Regular/Bold/Italic/BoldItalic）：只保留风格一致者，
     *     风格不一致的候选不参与，避免把 Regular 冒充 Bold；</li>
     * </ul>
     *
     * @param candidates 候选字体列表
     * @param wantBold   是否要求粗体
     * @param wantItalic 是否要求斜体
     * @return 风格一致的候选列表（保持扫描顺序），可能为空
     */
    private static List<FontEntry> filterByStyle(List<FontEntry> candidates, boolean wantBold, boolean wantItalic) {
        List<FontEntry> matched = new ArrayList<>();
        for (FontEntry entry : candidates) {
            if (entry.styleMatched) {
                // 以完整名称索引的候选：名称已经表达风格
                return Collections.singletonList(entry);
            }
            if (entry.bold == wantBold && entry.italic == wantItalic) {
                matched.add(entry);
            }
        }
        return matched;
    }

    /**
     * 把字体名规整后加入匹配键集合（去子集前缀、小写、去空格）
     *
     * @param keys 键集合
     * @param name 字体名，可为 null
     */
    private static void collectKey(Set<String> keys, String name) {
        if (name == null || name.trim().isEmpty()) {
            return;
        }
        String normalized = normalizeName(stripSubsetPrefix(name.trim()));
        if (!normalized.isEmpty()) {
            keys.add(normalized);
        }
    }

    /**
     * 规整用于匹配的名称（小写 + 去空格）
     *
     * @param name 名称
     * @return 规整后的名称
     */
    private static String normalizeName(String name) {
        return name.replace(" ", "").toLowerCase();
    }

    /**
     * 构建系统字体索引（小写名称 → 候选字体列表）
     * <p>
     * 索引键：文件名去扩展名（去 {@code #索引}）；可解析字体的 PostScript 名与字族名。
     * 每个键可对应多个文件（字族的 Regular/Bold/Italic/BoldItalic），由 {@link #filterByStyle} 按风格挑选。
     * 解析失败的字体（TTC、裸 CFF、损坏文件）只按文件名建索引，不阻断扫描。
     *
     * @return 只读索引
     */
    private synchronized Map<String, List<FontEntry>> systemFontIndex() {
        if (systemFontIndex != null) {
            return systemFontIndex;
        }
        Map<String, List<FontEntry>> index = new HashMap<>();
        List<Path> fontDirs = new ArrayList<>();
        if (fontDir != null) {
            // 只在显式配置时扫描；不再自动附加 DEFAULT_FONT_DIRS（默认不查系统字体）
            fontDirs.add(fontDir);
        }
        for (Path dir : fontDirs) {
            if (dir == null || !Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> stream = Files.walk(dir, FONT_DIR_SCAN_DEPTH)) {
                List<Path> files = new ArrayList<>();
                stream.filter(Files::isRegularFile).filter(PdfFontRegistry::hasFontSuffix).forEach(files::add);
                files.sort(Comparator.comparing(Path::toString));
                for (Path file : files) {
                    indexFontFile(index, file);
                }
            } catch (IOException | RuntimeException e) {
                logger.debug("扫描字体目录失败：{}", dir, e);
            }
        }
        systemFontIndex = Collections.unmodifiableMap(index);
        logger.debug("系统字体索引共 {} 个名称、目录 {}", index.size(), fontDirs);
        return systemFontIndex;
    }

    /**
     * 把一个字体文件加入索引
     *
     * @param index 索引（名称 → 候选列表）
     * @param path  字体文件路径
     */
    private static void indexFontFile(Map<String, List<FontEntry>> index, Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String bare = dot > 0 ? fileName.substring(0, dot) : fileName;
        int sharp = bare.lastIndexOf('#');
        if (sharp > 0) {
            bare = bare.substring(0, sharp);
        }
        String styleName = fileName;
        String familyName = bare;
        boolean bold = false;
        boolean italic = false;
        try (java.io.InputStream in = Files.newInputStream(path)) {
            TrueTypeFont font = new TTFParser().parseEmbedded(in);
            try {
                if (font.getNaming() != null) {
                    String psName = font.getNaming().getPostScriptName();
                    String family = font.getNaming().getFontFamily();
                    String subFamily = font.getNaming().getFontSubFamily();
                    if (psName != null) {
                        styleName = psName;
                    }
                    if (family != null) {
                        familyName = family;
                    }
                    bold = containsAny(subFamily, "Bold", "Semibold", "Black", "Heavy", "Medium")
                            || containsAny(psName, "Bold", "Black", "Heavy");
                    italic = containsAny(subFamily, "Italic", "Oblique")
                            || containsAny(psName, "Italic", "Oblique");
                }
            } finally {
                font.close();
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("字体解析失败，按文件名推断风格：{}", path, e);
            bold = containsAny(fileName, "Bold", "Black", "Heavy");
            italic = containsAny(fileName, "Italic", "Oblique");
        }
        // 1) 完整名称索引（文件名 / PostScript 名）：名称本身已表达风格，直接采用
        addCandidate(index, normalizeName(bare), new FontEntry(path, bold, italic, styleName, true));
        addCandidate(index, normalizeName(styleName), new FontEntry(path, bold, italic, styleName, true));
        // 2) 字族名索引：同一字族可能有多种风格，由字重/斜体筛选
        addCandidate(index, normalizeName(familyName), new FontEntry(path, bold, italic, styleName, false));
    }

    /**
     * 把候选字体加入索引（同名去重）
     *
     * @param index 索引
     * @param key   规整后的名称键
     * @param entry 候选字体
     */
    private static void addCandidate(Map<String, List<FontEntry>> index, String key, FontEntry entry) {
        if (key == null || key.isEmpty()) {
            return;
        }
        List<FontEntry> list = index.computeIfAbsent(key, k -> new ArrayList<>(2));
        for (FontEntry existing : list) {
            if (existing.path.equals(entry.path) && existing.styleMatched == entry.styleMatched) {
                return;
            }
        }
        list.add(entry);
    }

    /**
     * 判断文本是否包含任一关键字（大小写不敏感）
     *
     * @param text     待检查文本，可为 null
     * @param keywords 关键字
     * @return true 表示包含任一关键字
     */
    private static boolean containsAny(String text, String... keywords) {
        if (text == null) {
            return false;
        }
        String lower = text.toLowerCase();
        for (String keyword : keywords) {
            if (lower.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断文件是否可能为字体文件
     *
     * @param path 文件路径
     * @return true 表示扩展名为 ttf/otf/ttc/otc
     */
    private static boolean hasFontSuffix(Path path) {
        String name = path.getFileName().toString().toLowerCase();
        for (String suffix : FONT_SUFFIXES) {
            if (name.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 解析字体文件名中的 TTC 索引后缀（{@code #1}）
     *
     * @param fileName 文件名
     * @return 索引，单位为个；未指定时为 0
     */
    private static int ttcIndexOf(String fileName) {
        int sharp = fileName.lastIndexOf('#');
        if (sharp < 0 || sharp == fileName.length() - 1) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(fileName.substring(sharp + 1)));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 读取 sfnt 的字形数量（maxp.numGlyphs）
     *
     * @param sfnt sfnt 数据
     * @return 字形数量，单位：个；读取失败返回 0
     */
    private static int readNumGlyphs(byte[] sfnt) {
        try {
            return SfntTable.read(sfnt).getNumGlyphs();
        } catch (IOException e) {
            logger.debug("读取字形数量失败", e);
            return 0;
        }
    }

    /**
     * 字体族名：优先 PDF 描述符的 FontFamily，为空时使用字体名
     *
     * @param descriptor PDF 字体描述符，可为 null
     * @param fontName   去子集前缀的字体名
     * @return 字体族名
     */
    private static String fontFamilyOf(PDFontDescriptor descriptor, String fontName) {
        if (descriptor != null && descriptor.getFontFamily() != null
                && !descriptor.getFontFamily().trim().isEmpty()) {
            return descriptor.getFontFamily().trim();
        }
        return fontName;
    }

    /**
     * 是否粗体：Flags 含 ForceBold，或字体名含 Bold/Black/Heavy
     *
     * @param descriptor PDF 字体描述符，可为 null
     * @param fontName   字体名
     * @return true 表示粗体
     */
    private static boolean isBold(PDFontDescriptor descriptor, String fontName) {
        if (descriptor != null && (descriptor.getFlags() & FontDescriptor.FLAG_FORCE_BOLD) != 0) {
            return true;
        }
        String lower = fontName == null ? "" : fontName.toLowerCase();
        return lower.contains("bold") || lower.contains("black") || lower.contains("heavy");
    }

    /**
     * 是否斜体：Flags 含 Italic，或字体名含 Italic/Oblique
     *
     * @param descriptor PDF 字体描述符，可为 null
     * @param fontName   字体名
     * @return true 表示斜体
     */
    private static boolean isItalic(PDFontDescriptor descriptor, String fontName) {
        if (descriptor != null && (descriptor.getFlags() & FontDescriptor.FLAG_ITALIC) != 0) {
            return true;
        }
        String lower = fontName == null ? "" : fontName.toLowerCase();
        return lower.contains("italic") || lower.contains("oblique");
    }

    /**
     * 是否衬线：Flags 含 Serif
     *
     * @param descriptor PDF 字体描述符，可为 null
     * @return true 表示衬线字体
     */
    private static boolean isSerif(PDFontDescriptor descriptor) {
        return descriptor != null && (descriptor.getFlags() & FontDescriptor.FLAG_SERIF) != 0;
    }

    /**
     * 是否等宽：Flags 含 FixedPitch
     *
     * @param descriptor PDF 字体描述符，可为 null
     * @return true 表示等宽字体
     */
    private static boolean isFixedWidth(PDFontDescriptor descriptor) {
        return descriptor != null && (descriptor.getFlags() & FontDescriptor.FLAG_FIXED_PITCH) != 0;
    }

    /**
     * 去掉字体名的子集前缀（形如 {@code ABCDEF+}：6 个大写字母 + 加号）
     *
     * @param fontName 字体名，可为 null
     * @return 去前缀后的字体名；无前缀时原样返回
     */
    static String stripSubsetPrefix(String fontName) {
        if (fontName != null && fontName.length() > 7 && fontName.charAt(6) == '+') {
            boolean upper = true;
            for (int i = 0; i < 6; i++) {
                char c = fontName.charAt(i);
                if (c < 'A' || c > 'Z') {
                    upper = false;
                    break;
                }
            }
            if (upper) {
                return fontName.substring(7);
            }
        }
        return fontName == null ? "" : fontName;
    }

    /**
     * 把字体名转为安全的文件名（替换文件系统非法字符）
     *
     * @param fontName 字体名
     * @return 安全文件名（不含扩展名），单位：字符
     */
    private static String safeFileName(String fontName) {
        StringBuilder sb = new StringBuilder(fontName.length());
        for (int i = 0; i < fontName.length(); i++) {
            char c = fontName.charAt(i);
            boolean valid = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
            sb.append(valid ? c : '_');
        }
        return sb.length() == 0 ? "OFDFont" : sb.toString();
    }

    /**
     * 计算字节数据的 SHA-256（字体程序去重）
     *
     * @param data 数据
     * @return 64 位十六进制小写字符串
     */
    private static String sha256(byte[] data) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(data);
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("JDK 缺少 SHA-256 实现", e);
        }
    }

    /**
     * PDF 字体对象的稳定标识（同一字体对象在多次渲染中保持一致）
     *
     * @param font PDF 字体
     * @return 标识字符串
     */
    private static String identityOf(PDFont font) {
        return font.getCOSObject().hashCode() + "@" + font.getClass().getName();
    }

    /**
     * 日志用字体名：BaseFont 去子集前缀，取不到时用类型名
     *
     * @param font PDF 字体
     * @return 字体名
     */
    private static String safeName(PDFont font) {
        try {
            String name = font.getName();
            if (name != null && !name.trim().isEmpty()) {
                return stripSubsetPrefix(name.trim());
            }
        } catch (RuntimeException e) {
            logger.debug("读取字体名失败", e);
        }
        return font.getClass().getSimpleName();
    }

    /**
     * 取字体程序的 PostScript 名（字体 name 表），用于 OFD 的 {@code FontName} 与资源文件名
     * <p>
     * 优先使用 fontbox 读取的 name 表 PostScript 名；读不到（例如 TrueType 声明但实际为 CFF 封装、
     * 或字体 name 表损坏）时退回 PDF BaseFont 名（已去子集前缀）。
     *
     * @param sfnt        sfnt 字体数据
     * @param fallbackName 兜底名称（PDF BaseFont，已去子集前缀）
     * @return PostScript 名（去子集前缀），单位：字符
     */
    private static String postScriptNameOf(byte[] sfnt, String fallbackName) {
        try (java.io.InputStream in = new java.io.ByteArrayInputStream(sfnt)) {
            org.apache.fontbox.ttf.TrueTypeFont boxFont = new TTFParser().parseEmbedded(in);
            try {
                String psName = boxFont.getNaming() == null ? null : boxFont.getNaming().getPostScriptName();
                if (psName != null && !psName.trim().isEmpty()) {
                    return sanitizeName(stripSubsetPrefix(psName.trim()));
                }
            } finally {
                boxFont.close();
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("读取字体 PostScript 名失败，使用 BaseFont：{}", fallbackName, e);
        }
        return sanitizeName(stripSubsetPrefix(fallbackName));
    }

    /**
     * 规整字体名：把非 ASCII 字母数字与常见分隔符之外的字符替换为下划线，保证可作为文件名
     *
     * @param name 字体名，单位：字符
     * @return 规整后的名称
     */
    private static String sanitizeName(String name) {
        if (name == null || name.isEmpty()) {
            return "OFDFont";
        }
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            boolean valid = Character.isLetterOrDigit(c) || c == '-' || c == '_' || c == '.';
            sb.append(valid ? c : '_');
        }
        return sb.toString();
    }

    /**
     * 聚合记录一条 warning（同一“原因 + 字体”只保留一条，并累计出现次数）
     *
     * @param key     告警键（原因|字体名）
     * @param message 详情，可为 null
     */
    private synchronized void warn(String key, String message) {
        Integer count = warnings.get(key);
        if (count != null) {
            warnings.put(key, count + 1);
            return;
        }
        if (warnings.size() >= MAX_WARNING_KEYS) {
            return;
        }
        warnings.put(key, 1);
        logger.warn("[字体] {}：{}", key, message == null ? "" : message);
    }

    /**
     * 输出聚合 warning 的汇总（每个告警一行，含出现次数）
     */
    private synchronized void flushWarnings() {
        if (warnings.isEmpty()) {
            return;
        }
        List<String> lines = new ArrayList<>(warnings.size());
        for (Map.Entry<String, Integer> entry : warnings.entrySet()) {
            lines.add(entry.getKey() + "（" + entry.getValue() + " 次）");
        }
        logger.warn("[字体] 转换结束，共 {} 类字体告警：{}", lines.size(), String.join("；", lines));
    }

    /**
     * 字体来源标注（用于日志与排查，不影响输出结构）
     */
    enum Source {

        /**
         * PDF 内嵌字体程序逐字节原样嵌入（含裸 CFF 包装）
         */
        EMBEDDED_VERBATIM("embedded-verbatim"),

        /**
         * 字体集合（TTC）中提取指定字体后嵌入
         */
        TTC_EXTRACTED("ttc-extracted"),

        /**
         * 非内嵌字体按字体名精确匹配到系统/配置字体后嵌入
         */
        SYSTEM_MATCHED("system-matched"),

        /**
         * 无法嵌入，调用方回退路径绘制
         */
        PATH_FALLBACK("path-fallback");

        /**
         * 标注文本（日志使用）
         */
        private final String label;

        Source(String label) {
            this.label = label;
        }

        /**
         * 标注文本
         *
         * @return 例如 {@code embedded-verbatim}
         */
        String getLabel() {
            return label;
        }
    }

    /**
     * 系统/配置字体目录中的候选字体
     */
    private static final class FontEntry {

        /**
         * 字体文件路径
         */
        final Path path;

        /**
         * 是否粗体（来自 name 表子族名或文件名）
         */
        final boolean bold;

        /**
         * 是否斜体（来自 name 表子族名或文件名）
         */
        final boolean italic;

        /**
         * 字体风格名（PostScript 名或文件名）
         */
        final String styleName;

        /**
         * 是否以完整名称建立了索引（名称本身已表达风格）
         */
        final boolean styleMatched;

        FontEntry(Path path, boolean bold, boolean italic, String styleName, boolean styleMatched) {
            this.path = path;
            this.bold = bold;
            this.italic = italic;
            this.styleName = styleName;
            this.styleMatched = styleMatched;
        }
    }

    /**
     * 字体登记项
     */
    private static final class Registered {

        /**
         * 去重键：字体程序 SHA-256 前缀 + TTC 索引 + 是否子集
         */
        final String dedupeKey;

        /**
         * 原始字体数据（sfnt，注册时缓存），单位：字节
         */
        byte[] rawData;

        /**
         * 实际写入 OFD 的字体字节数，单位：字节；0 表示尚未写入
         */
        long writtenBytes;

        /**
         * 原始字体文件扩展名
         */
        String rawSuffix;

        /**
         * 当前字体文件扩展名
         */
        String suffix;

        /**
         * OFD 字体资源 ID
         */
        ST_ID resId;

        /**
         * OFD 字体元素
         */
        CT_Font ctFont;

        /**
         * 写出文件名（不含扩展名，已去子集前缀并附加哈希）
         */
        String fontName;

        /**
         * OFD {@code CT_Font.FontName} 取值：去子集前缀的 PostScript 名
         */
        String postScriptName;

        /**
         * 字体族名
         */
        String familyName;

        /**
         * 是否粗体
         */
        boolean bold;

        /**
         * 是否斜体
         */
        boolean italic;

        /**
         * 是否衬线
         */
        boolean serif;

        /**
         * 是否等宽
         */
        boolean fixedWidth;

        /**
         * 原始字体字形数量，单位：个
         */
        int numGlyphs;

        /**
         * 字体来源标注
         */
        Source source;

        /**
         * 实际使用的 PDF 原始 GID 集合
         */
        final Set<Integer> usedGids = Collections.synchronizedSet(new HashSet<>());

        /**
         * 实际使用的 Unicode 码位集合（重建 cmap 用）
         */
        final Set<Integer> usedUnicodes = Collections.synchronizedSet(new HashSet<>());

        /**
         * 是否已子集化
         */
        boolean subset;

        /**
         * 子集化后的 old GID → new GID 映射；原样嵌入时为 null
         */
        Map<Integer, Integer> subsetOldToNew;

        Registered(String dedupeKey) {
            this.dedupeKey = dedupeKey;
        }
    }
}
