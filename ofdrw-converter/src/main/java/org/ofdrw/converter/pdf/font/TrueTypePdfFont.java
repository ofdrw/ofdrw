package org.ofdrw.converter.pdf.font;

import org.ofdrw.converter.font.EmbeddedFontData;
import org.ofdrw.converter.font.FontDescriptor;
import org.ofdrw.converter.font.FontMetrics;
import org.ofdrw.converter.font.GlyphIndexProvider;
import org.ofdrw.converter.font.ToUnicodeCMap;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.converter.pdf.CoordSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * PDF TrueType 字体引用：整字体嵌入 + 字形优先 + ToUnicode（决策 3、决策 16）
 * <p>
 * 本类只做“字体选择结果的承载与导出”，字体格式识别、度量、嵌入数据与 ToUnicode 全部来自 {@code ofdrw-font}
 * 的 {@link FontDescriptor}、{@link FontMetrics}、{@link EmbeddedFontData}、{@link ToUnicodeCMap}、
 * {@link GlyphIndexProvider}，<b>converter 不自行解析字体结构</b>（决策 15）。
 * <p>
 * 字形编号约定（见 {@link #encodeGlyph(int)}）：字形总数不超过 255 时按单字节 CID 写入，否则按双字节 CID 写入，
 * 与 {@link ToUnicodeCMap} 自动选择的码位字节数保持一致，因此 ToUnicode 可直接覆盖全部写入的字形。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class TrueTypePdfFont implements PdfFontRef {

    private static final Logger logger = LoggerFactory.getLogger(TrueTypePdfFont.class);

    /**
     * 缺失字形（.notdef）的索引
     */
    public static final int NOTDEF_GLYPH = 0;

    /**
     * 页面内唯一的资源名，例如 {@code F1}
     */
    private final String resourceName;

    /**
     * 是否使用了替代字体
     */
    private final boolean substituted;

    /**
     * 字体族名（日志与 PDF FontFamily 使用）
     */
    private final String familyName;

    /**
     * 嵌入元信息，替代字体时可能为 null
     */
    private final FontDescriptor descriptor;

    /**
     * 字体度量
     */
    private final FontMetrics metrics;

    /**
     * 可嵌入的整字体数据
     */
    private final EmbeddedFontData embedded;

    /**
     * 字形索引提供器（字符 → GID）
     */
    private final GlyphIndexProvider glyphIndexProvider;

    /**
     * 已登记的字形到 Unicode 映射
     */
    private final ToUnicodeCMap toUnicode;

    /**
     * 码位字节数：默认按字形总数（&gt; 255 时为 2，否则为 1），生成 ToUnicode 后以 CMap 的码位宽度为准
     */
    private int codeByteLength;

    /**
     * 资源是否已写入页面资源字典
     */
    private boolean registered;

    /**
     * 构造字体引用
     *
     * @param resourceName      资源名，例如 {@code F1}
     * @param substituted       是否替代字体
     * @param familyName        字体族名
     * @param descriptor        嵌入元信息，替代字体时可为 null
     * @param metrics           字体度量，可为 null
     * @param embedded          可嵌入字体数据，替代字体时可为 null
     * @param glyphIndexProvider 字形索引提供器，可为 null
     * @param numGlyphs         字形总数，无法确定时传 -1
     */
    public TrueTypePdfFont(String resourceName, boolean substituted, String familyName,
                           FontDescriptor descriptor, FontMetrics metrics, EmbeddedFontData embedded,
                           GlyphIndexProvider glyphIndexProvider, int numGlyphs) {
        if (resourceName == null || resourceName.isEmpty()) {
            throw new IllegalArgumentException("资源名不能为空");
        }
        this.resourceName = resourceName;
        this.substituted = substituted;
        this.familyName = familyName == null ? "UnknownFont" : familyName;
        this.descriptor = descriptor;
        this.metrics = metrics;
        this.embedded = embedded;
        this.glyphIndexProvider = glyphIndexProvider;
        // Identity-H 编码下文本字符串恒为 2 字节 CID，与字形总数无关
        this.codeByteLength = 2;
        String psName = descriptor == null ? this.familyName : descriptor.getRenamedPostScriptName();
        this.toUnicode = new ToUnicodeCMap(psName, metrics);
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public boolean isSubstituted() {
        return substituted;
    }

    @Override
    public String getFamilyName() {
        return familyName;
    }

    /**
     * 嵌入元信息
     *
     * @return 嵌入元信息；替代字体且无可用字体文件时为 null
     */
    public FontDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * 字体度量
     *
     * @return 字体度量，可能为 null
     */
    public FontMetrics getMetrics() {
        return metrics;
    }

    /**
     * 可嵌入的整字体数据
     *
     * @return 字体数据；替代字体且无可用字体文件时为 null
     */
    public EmbeddedFontData getEmbedded() {
        return embedded;
    }

    /**
     * 字形索引提供器
     *
     * @return 提供器，可能为 null
     */
    public GlyphIndexProvider getGlyphIndexProvider() {
        return glyphIndexProvider;
    }

    /**
     * 写入 PDF 时使用的 BaseFont 名称
     *
     * @return 带 {@code ABCDEF+} 前缀（如需要）的 PostScript 名
     */
    public String getBaseFontName() {
        return descriptor == null ? familyName : descriptor.getRenamedPostScriptName();
    }

    /**
     * 每个字形按字号换算的标称步进
     *
     * @param fontSizePt 字号，单位 pt
     * @return 标称步进，单位 pt；字体没有字宽表时按 0.5 em 估算
     */
    public double nominalAdvancePt(double fontSizePt) {
        double width1000;
        if (metrics != null && metrics.hasAdvanceWidth()) {
            width1000 = metrics.getAdvanceWidth(this.sampleGlyph());
        } else {
            width1000 = 500;
        }
        if (width1000 <= 0) {
            width1000 = 500;
        }
        return width1000 / 1000d * fontSizePt;
    }

    /**
     * 用于估算标称步进的字形（取空格与 'M' 之外的首个有宽度字形，退化为 1 号字形）
     *
     * @return 字形索引
     */
    private int sampleGlyph() {
        if (metrics == null) {
            return 1;
        }
        for (int gid = 3; gid < 100; gid++) {
            if (metrics.getAdvanceWidth(gid) > 0) {
                return gid;
            }
        }
        return 1;
    }

    /**
     * 码位字节数（跟随最近一次 CMap 生成结果）
     *
     * @return 1 或 2
     */
    public int getCodeByteLength() {
        return codeByteLength;
    }

    /**
     * 依据当前已登记的字形映射生成 ToUnicode CMap，并锁定 CID 的码位宽度
     * <p>
     * 字形编号是否超过 255 决定了 ToUnicode 的码位宽度（{@link org.ofdrw.converter.font.ToUnicodeCMap#requiresTwoBytes()}）；
     * 为保证内容流中的 CID 与 CMap 的码位长度一致，这里以 CMap 的实际结果为准确认编码宽度，
     * 之后的 {@link #encodeGlyph(int)} 与 {@link #getCodeByteLength()} 均以此为准。
     *
     * @return CMap 文本；没有登记任何映射时返回 null（此时沿用按字体字形总数确定的宽度）
     */
    public String prepareToUnicode() {
        if (toUnicode.size() == 0) {
            return null;
        }
        // Identity-H：码位字节数恒为 2，同时让 ToUnicode 声明双字节码空间
        toUnicode.setCodeBytes(2);
        String text = toUnicode.toCMapText();
        this.codeByteLength = 2;
        return text;
    }

    /**
     * 解析字形索引：显式 GID 优先，其次 cmap 查表，最后回退字形 0
     * <p>
     * 字体没有可用 cmap（OFD 内嵌字体常见：整字体被裁剪到只保留 glyph 轮廓，cmap 被移除）时，
     * 无法把字符映射为字形，返回 {@link #NOTDEF_GLYPH}；调用方据此报错并提示“需要参数化字体”的降级原因，
     * 而不是静默画出 .notdef（表现为圆点/乱码）。
     *
     * @param explicitGid CGTransform 给出的字形索引，负值表示未给出
     * @param codePoints  Unicode 码位序列，可为空
     * @param warnings    降级事件收集器
     * @return 字形索引
     */
    public int resolveGlyphId(int explicitGid, int[] codePoints, java.util.List<String> warnings) {
        if (glyphIndexProvider == null) {
            return NOTDEF_GLYPH;
        }
        int codePoint = codePoints == null || codePoints.length == 0 ? 0 : codePoints[0];
        GlyphIndexProvider.GlyphSelection selection = glyphIndexProvider.resolve(explicitGid, codePoint);
        if (selection.isSubstituted() && warnings != null) {
            warnings.add(describeSubstitution(codePoint, selection));
        }
        return selection.getGlyphId(0);
    }

    /**
     * 是否含可用的 Unicode cmap 表
     * <p>
     * 无 cmap 的字体只能按显式 GID（CGTransform）取字形。
     *
     * @return true 表示可以按字符查表
     */
    public boolean hasUnicodeCmap() {
        return glyphIndexProvider != null && glyphIndexProvider.hasUnicodeCmap();
    }

    /**
     * 生成字形替换的说明文本（区分“字体无 cmap”与“该字符不在字体中”）
     *
     * @param codePoint 无法映射的码位
     * @param selection 字形选择结果
     * @return 说明文本
     */
    private String describeSubstitution(int codePoint, GlyphIndexProvider.GlyphSelection selection) {
        String charText = codePoint > 0 && codePoint <= 0x10FFFF
                ? new String(Character.toChars(codePoint)) : "?";
        if (!hasUnicodeCmap()) {
            return "字体 " + familyName + " 没有 cmap 表（字形不可按字符映射），字符 [" + charText
                    + "] U+" + String.format("%04X", codePoint) + " 回退字形 " + selection.getGlyphId(0)
                    + "；该 OFD 内嵌字体需按 CGTransform 的字形索引绘制，或改用参数化字体";
        }
        return "字符 [" + charText + "] U+" + String.format("%04X", codePoint) + " 不在字体 " + familyName
                + " 中，回退字形 " + selection.getGlyphId(0);
    }

    /**
     * 布局步进换算为 pt
     *
     * @param advanceMm  布局给出的步进，单位 mm
     * @param fontSizePt 字号，单位 pt
     * @return 步进，单位 pt
     */
    public double advancePt(double advanceMm, double fontSizePt) {
        return advanceMm * CoordSystem.MM_TO_PT;
    }

    /**
     * 把字形索引编码为 PDF 字符串中的码位值
     * <p>
     * 单字节模式下仅支持 0~255 的字形；超出范围时回退字形 0 并记录 warning。
     *
     * @param gid 字形索引
     * @return 码位值
     */
    public int encodeGlyph(int gid) {
        if (codeByteLength == 1) {
            if (gid > 0xFF) {
                logger.warn("字体 {} 为单字节编码，字形 {} 超出范围，回退字形 0", familyName, gid);
                return 0;
            }
            return gid;
        }
        return gid;
    }

    /**
     * 登记“字形 → Unicode”映射（供 ToUnicode CMap 输出）
     *
     * @param gid       字形索引
     * @param codePoints Unicode 码位序列
     * @return this
     */
    /**
     * 登记文档声明的排版步进
     * <p>
     * 同一字形出现多次时保留首次登记的步进（同一字体同一字符的步进通常一致）。
     *
     * @param gid          字形索引
     * @param advance1000  文档步进，单位 1/1000 em；非正值将被忽略
     * @return this
     */
    public TrueTypePdfFont registerDocumentAdvance(int gid, float advance1000) {
        if (gid >= 0 && advance1000 > 0f) {
            documentAdvances.putIfAbsent(gid, advance1000);
        }
        return this;
    }

    /**
     * 已登记的文档步进表
     *
     * @return 字形索引 → 步进（1/1000 em）的只读视图
     */
    public java.util.Map<Integer, Float> documentAdvances() {
        return java.util.Collections.unmodifiableMap(documentAdvances);
    }

    /**
     * 取已登记的文档步进
     *
     * @param gid 字形索引
     * @return 文档步进，单位 1/1000 em；未登记返回 {@code null}
     */
    public Float getDocumentAdvance(int gid) {
        return documentAdvances.get(gid);
    }

    public TrueTypePdfFont registerUnicode(int gid, int[] codePoints) {
        if (codePoints == null || codePoints.length == 0) {
            return this;
        }
        toUnicode.add(gid, codePoints);
        return this;
    }

    /**
     * 文档声明的排版步进（1/1000 em），按字形索引登记
     * <p>
     * 用于 {@code /W} 字宽：OFD 用 {@code TextCode} 的 {@code DeltaX/DeltaY} 精确定位每个字形，
     * 若 {@code /W} 取替代字体自带的字宽（与文档步进不一致），阅读器会认为“字宽 < 字距”而插入词间空格，
     * 导致复制出的文本多出空格、甚至错行。这里登记文档步进，写入侧优先采用它。
     */
    private final java.util.Map<Integer, Float> documentAdvances =
            new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 生成 ToUnicode CMap 文本
     *
     * @return CMap 文本；没有登记任何映射时返回 null（此时 PDF 中省略 /ToUnicode）
     */
    public String toUnicodeCMap() {
        if (toUnicode.size() == 0) {
            return null;
        }
        return toUnicode.toCMapText();
    }

    /**
     * ToUnicode 映射生成器
     *
     * @return 映射生成器
     */
    public ToUnicodeCMap getToUnicode() {
        return toUnicode;
    }

    /**
     * 资源是否已写入页面资源字典
     *
     * @return true 表示已写入
     */
    public boolean isRegistered() {
        return registered;
    }

    /**
     * 标记资源已写入页面资源字典
     */
    public void markRegistered() {
        this.registered = true;
    }

    /**
     * 由字体数据创建字体引用
     * <p>
     * 字体格式识别、度量、嵌入数据与字形映射全部委托 {@code ofdrw-font}，本方法只做装配。
     *
     * @param resourceName 资源名
     * @param fontData     字体文件的全部字节
     * @param ttcIndex     字体集合内索引，单位：个；非字体集合时忽略
     * @param substituted  是否替代字体
     * @return 字体引用
     * @throws IOException 字体数据无法解析或无法嵌入
     */
    public static TrueTypePdfFont create(String resourceName, byte[] fontData, int ttcIndex, boolean substituted)
            throws IOException {
        EmbeddedFontData embedded = EmbeddedFontData.of(fontData, ttcIndex);
        TrueTypeFont font = new TrueTypeFont().parse(embedded.getData());
        FontMetrics metrics = FontMetrics.of(font);
        FontDescriptor descriptor = FontDescriptor.of(font, embedded);
        GlyphIndexProvider provider = new GlyphIndexProvider(font);
        return new TrueTypePdfFont(resourceName, substituted, descriptor.getFamilyName(),
                descriptor, metrics, embedded, provider, font.getNumGlyphs());
    }

    /**
     * 无字体文件时的兜底引用：只用于继续写出文字（字形 0），并保持 ToUnicode 可用
     *
     * @param resourceName 资源名
     * @param familyName   字体族名
     * @param substituted  是否替代字体
     * @return 字体引用
     */
    public static TrueTypePdfFont withoutFontFile(String resourceName, String familyName, boolean substituted) {
        return new TrueTypePdfFont(resourceName, substituted, familyName, null, null, null, null, -1);
    }

    @Override
    public String toString() {
        return "TrueTypePdfFont{" + resourceName + ", " + getBaseFontName()
                + (substituted ? ", substituted" : "") + ", cidBytes=" + codeByteLength + "}";
    }

    /**
     * 页面字体资源表的默认实现：按资源名登记字体引用
     * <p>
     * 写入器通过 {@link #getFonts()} 取得当前页面需要写入资源字典的全部字体，
     * 并可用 {@link #register(String, byte[], int, boolean)} 为替换字体分配页面内唯一资源名。
     *
     * @author 权观宇
     * @since 2.5.1
     */
    public static final class Registry implements FontSelector {

        /**
         * 已登记的字体引用，KEY 为资源名
         */
        private final Map<String, TrueTypePdfFont> fonts = new LinkedHashMap<>();

        /**
         * 已写入 PDF 的字体，KEY 为字体对象标识（同一字体文件只嵌入一次）
         */
        private final Map<Object, TrueTypePdfFont> resolved = new HashMap<>();

        /**
         * 资源名序号
         */
        private int nextIndex = 1;

        /**
         * 全文档出现过的字体（跨页累积，不随 {@link #newPage()} 清空）
         * <p>
         * 同一字体在多页出现时是同一个 {@link TrueTypePdfFont} 实例，其“字形 → Unicode”映射会随页面推进不断追加，
         * 因此保存前需要据此统一刷新 ToUnicode。
         */
        private final java.util.Set<TrueTypePdfFont> allFonts = new java.util.LinkedHashSet<>();

        @Override
        public PdfFontRef select(List<?> fontResources, org.ofdrw.converter.pdf.render.RenderContext context) {
            if (fontResources != null) {
                for (Object resource : fontResources) {
                    if (resource instanceof TrueTypePdfFont) {
                        return (TrueTypePdfFont) resource;
                    }
                }
            }
            return null;
        }

        /**
         * 资源名序号
         *
         * @return 下一个可用的资源名序号
         */
        public int nextResourceIndex() {
            return nextIndex;
        }

        /**
         * 登记字体引用
         *
         * @param font 字体引用，资源名为空时自动分配
         * @return 登记后的字体引用
         */
        public TrueTypePdfFont register(TrueTypePdfFont font) {
            if (font == null) {
                throw new IllegalArgumentException("字体引用不能为空");
            }
            if (fonts.containsKey(font.getResourceName())) {
                return fonts.get(font.getResourceName());
            }
            fonts.put(font.getResourceName(), font);
            allFonts.add(font);
            return font;
        }

        /**
         * 全文档出现过的字体
         *
         * @return 只读集合，按首次出现顺序
         */
        public java.util.Collection<TrueTypePdfFont> getAllFonts() {
            return java.util.Collections.unmodifiableSet(allFonts);
        }

        /**
         * 登记已写入 PDF 的字体，便于同一字体对象在后续文字中复用
         *
         * @param key  字体对象标识（字体文件路径等）
         * @param font 字体引用
         */
        public void resolved(Object key, TrueTypePdfFont font) {
            if (key != null) {
                resolved.put(key, font);
            }
        }

        /**
         * 查找已写入 PDF 的字体
         *
         * @param key 字体对象标识
         * @return 字体引用；未写入时返回 null
         */
        public TrueTypePdfFont lookup(Object key) {
            return key == null ? null : resolved.get(key);
        }

        /**
         * 按资源名查找当前页面已登记的字体
         * <p>
         * 写入侧凭文字指令携带的资源名（例如 {@code F1}）取回字体引用，
         * 避免调用方先取 {@link #getFonts()} 再逐个比对 {@code getResourceName()} 造成错配。
         *
         * @param resourceName 资源名，例如 {@code F1}，可为空
         * @return 字体引用；资源名为空或未登记时返回 null
         */
        public TrueTypePdfFont lookupByResourceName(String resourceName) {
            return resourceName == null ? null : fonts.get(resourceName);
        }

        /**
         * 当前页面的全部字体
         *
         * @return 只读列表
         */
        public List<TrueTypePdfFont> getFonts() {
            return Collections.unmodifiableList(new ArrayList<>(fonts.values()));
        }

        /**
         * 开始新的页面：清空页面资源表
         */
        public void newPage() {
            fonts.clear();
            nextIndex = 1;
        }

        /**
         * 分配新的资源名
         *
         * @return 资源名，例如 {@code F1}
         */
        public String allocateResourceName() {
            return "F" + (nextIndex++);
        }
    }
}
