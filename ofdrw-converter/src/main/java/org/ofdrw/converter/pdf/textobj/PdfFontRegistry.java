package org.ofdrw.converter.pdf.textobj;

import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.cos.COSInputStream;
import org.apache.pdfbox.pdmodel.common.PDStream;
import org.apache.pdfbox.pdmodel.font.PDCIDFont;
import org.apache.pdfbox.pdmodel.font.PDCIDFontType2;
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
     * 取 2 而不是 4：CJK 字体即使只用了常用字，也会因字形总数远超使用数而收益明显；
     * 真正的“小字体”由该倍数与 {@link #SUBSET_MIN_SIZE} 联合排除。
     */
    private static final int SUBSET_GLYPH_RATIO = 2;

    /**
     * 子集化判定阈值：字体文件最小体积，单位：字节（512 KB）
     */
    private static final int SUBSET_MIN_SIZE = 512 * 1024;

    /**
     * 单个内嵌字体的体积上限，单位：字节（20 MB）
     * <p>
     * 超过上限的 TrueType 字体（典型为整页 CJK 原样嵌入）在写出前自动子集化，
     * 既保证“原样嵌入优先”的默认语义，又避免单个字体把 OFD 体积拖到几十 MB。
     */
    private static final int FONT_FILE_MAX_BYTES = 20 * 1024 * 1024;

    /**
     * 扫描字体目录的最大深度，单位：层
     */
    private static final int FONT_DIR_SCAN_DEPTH = 4;

    /**
     * 聚合告警的最大条数，单位：条（超出后只计数）
     */
    private static final int MAX_WARNING_KEYS = 64;

    /**
     * 各平台默认字体目录（不存在的目录在扫描时跳过）
     */
    private static final String[] DEFAULT_FONT_DIRS = {
            "/usr/share/fonts",
            "/usr/local/share/fonts",
            System.getProperty("user.home", "") + "/.local/share/fonts",
            "/Library/Fonts",
            "/System/Library/Fonts",
            System.getenv("WINDIR") == null ? "C:/Windows/Fonts" : System.getenv("WINDIR") + "/Fonts"
    };

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
    private Map<String, Path> systemFontIndex;

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
     * @param subsetFont true 开启子集化；false（默认）原样嵌入
     */
    public void setSubsetFont(boolean subsetFont) {
        this.subsetFont = subsetFont;
    }

    /**
     * 设置额外的字体查找目录（用于 PDF 未内嵌字体时的精确匹配）
     * <p>
     * 该目录会排在系统字体目录之前；为 null 时只使用系统字体目录。匹配只做“字体名完全相等”，
     * 不做相似匹配与风格推断（设计文档 5.6.2 第 3 级）。
     *
     * @param fontDir 自定义字体目录；不存在时忽略
     */
    public void setFontDir(Path fontDir) {
        this.fontDir = fontDir;
        this.systemFontIndex = null;
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
        Registered registered = font == null ? null : byFont.get(identityOf(font));
        if (registered == null || registered.rawData == null) {
            return;
        }
        if (gid > 0) {
            registered.usedGids.add(gid);
        }
        if (unicode >= 0) {
            registered.usedUnicodes.add(unicode);
        }
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
        if (data.length > FONT_FILE_MAX_BYTES && FontFileFormat.hasTable(data, 0, "glyf")) {
            // 体积保护：超过上限的字体自动改用子集化（只保留实际使用字形），避免整页文字被一个超大字体拖垮
            byte[] subset = trySubset(registered);
            if (subset != null) {
                data = subset;
                logger.debug("字体 {} 原样嵌入 {} 字节超过上限，已自动子集化为 {} 字节",
                        registered.fontName, registered.rawData.length, data.length);
            }
        }
        ResDir resDir = doc.docDir.obtainRes();
        ST_Loc loc = resDir.getAbsLoc().cat(registered.fontName + registered.suffix);
        resDir.putFile(registered.fontName + registered.suffix, data);
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
        if (!subsetFont || !isWorthSubsetting(registered)) {
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
     * 判断字体是否值得子集化（设计文档 5.6.3：完整 CJK 字体才值得，已是小体积子集则原样嵌入）
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
        /*
         * 体积决策（设计文档 5.6.3，按实际收益判断而不是固定阈值）：
         * PDF 内嵌字体常为完整 CJK 字体（如 SimSun 10MB 内嵌后仅被使用几百个字形），
         * 只有“使用字形数远少于字形总数”的子集才有收益。
         */
        if (registered.numGlyphs > 0) {
            if (registered.numGlyphs <= registered.usedGids.size() + 1) {
                // 实际使用字形已覆盖全部字形（如整页 CJK 文档用满字体），子集化无收益
                return false;
            }
            return registered.numGlyphs > (registered.usedGids.size() + 1) * SUBSET_GLYPH_RATIO;
        }
        // 无法读取字形数时按体积判断
        return registered.rawData.length > SUBSET_MIN_SIZE;
    }

    /**
     * 解析 PDF 字体，决定字体来源并准备登记项（不写文件）
     *
     * @param font PDF 字体
     * @return 登记项；无法嵌入时返回 null
     */
    private Registered resolve(PDFont font) {
        String name = safeName(font);
        PDFontDescriptor pdDescriptor = font.getFontDescriptor();
        // PDType0Font 的字体程序挂在后代 CIDFont 上
        PDCIDFont fontProgramOwner = font instanceof PDType0Font ? ((PDType0Font) font).getDescendantFont() : null;
        byte[] data = readEmbeddedProgram(font, fontProgramOwner, name);
        Source source = Source.EMBEDDED_VERBATIM;
        boolean fromSystem = false;
        int ttcIndex = 0;
        if (data == null) {
            Path systemFont = matchSystemFont(font);
            if (systemFont == null) {
                warn("没有可嵌入字体且系统字体未精确匹配|" + name,
                        Source.PATH_FALLBACK.getLabel() + "：回退路径绘制");
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
            warn("使用系统字体精确匹配替代非内嵌字体|" + name, systemFont.toString());
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
     * 读取 PDF 内嵌字体程序字节
     *
     * @param font            PDF 字体（用于判断类型）
     * @param fontProgramOwner 挂载字体程序的字体对象（Type0 时为后代 CIDFont）
     * @param name            日志用字体名
     * @return 字体程序字节；无内嵌程序或不可用（Type3、非 CIDFontType2 的复合字体）时返回 null
     */
    private byte[] readEmbeddedProgram(PDFont font, PDCIDFont fontProgramOwner, String name) {
        if (font instanceof PDType0Font && !(fontProgramOwner instanceof PDCIDFontType2)) {
            // Type0 + Type1C / Type0 + 其它后代：首版按“内嵌字体原样嵌入”的目标改为系统字体或回退
            warn("复合字体后代类型暂不直接内嵌|" + name,
                    fontProgramOwner == null ? "后代字体为空" : fontProgramOwner.getClass().getSimpleName());
            return null;
        }
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
            // 裸 CFF：补最小 sfnt 外壳后嵌入（无 cmap，依赖 CGTransform）
            sfnt = CffFontData.wrap(sfnt, 1, new int[]{0}, null, name);
            format = FontFileFormat.OpenTypeCff;
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
     * 在系统/配置字体目录中精确匹配字体
     * <p>
     * 匹配键：FontName、去子集前缀的 FontName、FontFamily；与字体文件名（去扩展名）或字体文件内
     * PostScript 名 / 字族名做大小写不敏感的完全相等比较，不做相似匹配与风格推断。
     *
     * @param font PDF 字体
     * @return 字体文件路径；未匹配时返回 null
     */
    private Path matchSystemFont(PDFont font) {
        Set<String> keys = new LinkedHashSet<>();
        collectKey(keys, font.getName());
        if (font.getFontDescriptor() != null) {
            collectKey(keys, font.getFontDescriptor().getFontName());
            collectKey(keys, font.getFontDescriptor().getFontFamily());
        }
        if (keys.isEmpty()) {
            return null;
        }
        Map<String, Path> index = systemFontIndex();
        for (String key : keys) {
            Path path = index.get(key);
            if (path != null) {
                return path;
            }
        }
        return null;
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
        String normalized = stripSubsetPrefix(name.trim()).replace(" ", "").toLowerCase();
        if (!normalized.isEmpty()) {
            keys.add(normalized);
        }
    }

    /**
     * 构建系统字体索引（小写名称 → 字体文件）
     * <p>
     * 索引键：文件名去扩展名；可解析字体的 PostScript 名与字族名（去空格后小写）。
     * 解析失败的字体（TTC、裸 CFF、损坏文件）只按文件名建索引，不阻断扫描。
     *
     * @return 只读索引
     */
    private synchronized Map<String, Path> systemFontIndex() {
        if (systemFontIndex != null) {
            return systemFontIndex;
        }
        Map<String, Path> index = new HashMap<>();
        List<Path> fontDirs = new ArrayList<>();
        if (fontDir != null) {
            fontDirs.add(fontDir);
        }
        fontDirs.addAll(defaultFontDirs());
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
     * 把一个字体文件加入索引（同名键只保留先出现的文件）
     *
     * @param index 索引
     * @param path  字体文件路径
     */
    private static void indexFontFile(Map<String, Path> index, Path path) {
        String fileName = path.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String bare = dot > 0 ? fileName.substring(0, dot) : fileName;
        index.putIfAbsent(bare.replace(" ", "").toLowerCase(), path);
        try (java.io.InputStream in = Files.newInputStream(path)) {
            TrueTypeFont font = new TTFParser().parseEmbedded(in);
            try {
                if (font.getNaming() != null) {
                    String psName = font.getNaming().getPostScriptName();
                    String family = font.getNaming().getFontFamily();
                    if (psName != null) {
                        index.putIfAbsent(psName.replace(" ", "").toLowerCase(), path);
                    }
                    if (family != null) {
                        index.putIfAbsent(family.replace(" ", "").toLowerCase(), path);
                    }
                }
            } finally {
                font.close();
            }
        } catch (IOException | RuntimeException e) {
            logger.debug("字体解析失败，仅按文件名索引：{}", path, e);
        }
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
     * 默认系统字体目录
     *
     * @return 只读目录列表
     */
    private static List<Path> defaultFontDirs() {
        List<Path> dirs = new ArrayList<>();
        for (String dir : DEFAULT_FONT_DIRS) {
            if (dir != null && !dir.isEmpty()) {
                dirs.add(Paths.get(dir));
            }
        }
        return Collections.unmodifiableList(dirs);
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
