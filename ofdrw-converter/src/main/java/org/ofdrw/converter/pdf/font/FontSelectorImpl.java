package org.ofdrw.converter.pdf.font;

import org.apache.commons.io.IOUtils;
import org.ofdrw.converter.font.TrueTypeFont;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 字体选择与整字体嵌入（决策 15、16）
 * <p>
 * 策略“内嵌优先、替换兜底”：
 * <ol>
 *     <li>OFD 字体资源带 {@code FontFile} 时，读取字体文件并整字体嵌入（不做子集裁剪，决策 16）；
 *     字体集合（TTC）按 {@code #索引} 后缀或按名称匹配选择集合内的字体；</li>
 *     <li>内嵌缺失、无法解析或无法嵌入（如裸 CFF）时，按 FamilyName/Serif/Bold/Italic 匹配系统字体作为替代，
 *     并输出 <b>warning</b> 日志（决策 7/8：降级不阻断）；</li>
 *     <li>替代字体也不可用时，生成只含字形 0 的兜底引用，保证文字仍被写出且文本可提取（CMap 为 Identity）；</li>
 *     <li>同一字体对象在页面内复用同一资源名与同一 PDF 字体对象，避免重复嵌入。</li>
 * </ol>
 * 字体格式识别、度量、嵌入数据与字形映射均由 {@code ofdrw-font} 提供，本类只负责“选哪种字体、如何映射到 PDF 对象”。
 *
 * @author 权观宇
 * @since 2.5.1
 */
public final class FontSelectorImpl implements FontSelector {

    private static final Logger logger = LoggerFactory.getLogger(FontSelectorImpl.class);

    /**
     * 字体集合路径中的索引分隔符
     */
    private static final String TTC_INDEX_SEPARATOR = "#";

    /**
     * OFD 资源定位器，用于读取 OFD 包内字体文件，可为 null
     */
    private final ResourceLocator locator;

    /**
     * 页面字体资源登记表
     */
    private final TrueTypePdfFont.Registry registry;

    /**
     * 字体资源解析器：资源引用（ID）→ {@code CT_Font}
     */
    private java.util.function.Function<String, CT_Font> fontResolver;

    /**
     * 构造字体选择器
     *
     * @param locator  OFD 资源定位器，可为 null（此时无法读取包内字体，只能替换）
     * @param registry 字体资源登记表，可为 null（自动创建）
     */
    public FontSelectorImpl(ResourceLocator locator, TrueTypePdfFont.Registry registry) {
        this.locator = locator;
        this.registry = registry == null ? new TrueTypePdfFont.Registry() : registry;
    }

    /**
     * 构造字体选择器（使用默认资源登记表）
     *
     * @param locator OFD 资源定位器，可为 null
     */
    public FontSelectorImpl(ResourceLocator locator) {
        this(locator, new TrueTypePdfFont.Registry());
    }

    /**
     * 字体资源登记表
     *
     * @return 登记表
     */
    public TrueTypePdfFont.Registry getRegistry() {
        return registry;
    }

    /**
     * 解析 OFD 字体资源的引用（资源 ID 或包内路径）
     * <p>
     * 由上层在创建选择器时注入解析器，渲染器只传引用字符串，不直接依赖资源管理器。
     *
     * @param ref 资源引用
     * @return 字体资源对象（{@code CT_Font}）；无法解析时返回 null
     */
    public Object resolveFont(String ref) {
        return fontResolver == null || ref == null ? null : fontResolver.apply(ref);
    }

    /**
     * 设置字体资源解析器
     *
     * @param resolver 由资源引用到 {@code CT_Font} 的解析函数
     * @return this
     */
    public FontSelectorImpl fontResolver(java.util.function.Function<String, CT_Font> resolver) {
        this.fontResolver = resolver;
        return this;
    }

    @Override
    public PdfFontRef select(List<?> fontResources, org.ofdrw.converter.pdf.render.RenderContext context) {
        if (fontResources == null || fontResources.isEmpty()) {
            logger.warn("文字图元没有引用字体资源，无法写出文字");
            return null;
        }
        Object first = fontResources.get(0);
        if (!(first instanceof CT_Font)) {
            logger.warn("文字图元的字体资源类型不支持：{}", first == null ? "null" : first.getClass().getName());
            return null;
        }
        CT_Font ctFont = (CT_Font) first;
        String key = cacheKey(ctFont);
        // 同一页面内同一字体只写入一次
        TrueTypePdfFont cached = registry.lookup(key);
        if (cached != null) {
            return registry.register(cached);
        }
        TrueTypePdfFont selected = doSelect(ctFont);
        registry.resolved(key, selected);
        return registry.register(selected);
    }

    /**
     * 选择字体：内嵌优先，替换兜底
     *
     * @param ctFont OFD 字体资源
     * @return 字体引用，不会返回 null
     */
    private TrueTypePdfFont doSelect(CT_Font ctFont) {
        String fontName = safeName(ctFont);
        byte[] embeddedData = readEmbeddedFont(ctFont);
        int ttcIndex = ttcIndexOf(ctFont);
        if (embeddedData != null) {
            try {
                TrueTypePdfFont font = TrueTypePdfFont.create(registry.allocateResourceName(), embeddedData, ttcIndex, false);
                logger.debug("嵌入 OFD 内嵌字体：{} → {}", fontName, font);
                return font;
            } catch (IOException | RuntimeException e) {
                logger.warn("OFD 内嵌字体 {} 无法嵌入（{}），改用替代字体", fontName, e.getMessage());
            }
        } else {
            logger.warn("字形资源 {} 没有可用的内嵌字体文件，改用替代字体", fontName);
        }
        return substitute(ctFont);
    }

    /**
     * 匹配并加载替代字体
     *
     * @param ctFont OFD 字体资源
     * @return 替代字体引用；系统字体也不可用时为兜底引用
     */
    private TrueTypePdfFont substitute(CT_Font ctFont) {
        String fontName = safeName(ctFont);
        Path substitute = matchSystemFont(ctFont);
        if (substitute == null) {
            logger.warn("字体 {} 没有找到可用的替代字体，文字将按缺省字形写出", fontName);
            return TrueTypePdfFont.withoutFontFile(registry.allocateResourceName(), fontName, true);
        }
        try {
            byte[] data = Files.readAllBytes(substitute);
            TrueTypePdfFont font = TrueTypePdfFont.create(registry.allocateResourceName(), data, 0, true);
            logger.warn("字体 {} 使用替代字体 {}（{}）", fontName, font.getFamilyName(), substitute.getFileName());
            return font;
        } catch (IOException | RuntimeException e) {
            logger.warn("替代字体 {} 加载失败（{}），文字将按缺省字形写出", substitute, e.getMessage());
            return TrueTypePdfFont.withoutFontFile(registry.allocateResourceName(), fontName, true);
        }
    }

    /**
     * 按 FamilyName/Serif/Bold/Italic 匹配系统字体
     *
     * @param ctFont OFD 字体资源
     * @return 系统字体文件路径；未找到时返回 null
     */
    private static Path matchSystemFont(CT_Font ctFont) {
        String family = firstNonEmpty(ctFont.getFamilyName(), safeName(ctFont));
        String fontName = safeName(ctFont);
        boolean bold = Boolean.TRUE.equals(ctFont.getBold());
        boolean italic = Boolean.TRUE.equals(ctFont.getItalic());
        boolean serif = Boolean.TRUE.equals(ctFont.getSerif());

        // 1) 字族名/字体名就是系统字体文件路径（部分生成器直接把路径写进 FamilyName）
        for (String name : new String[]{family, fontName}) {
            Path direct = existingFontFile(name);
            if (direct != null) {
                return direct;
            }
        }
        // 2) 常见中文字族名 → 字体文件名（与 fontconfig 的别名等价，属于通用字体替换规则）
        for (String name : new String[]{family, fontName}) {
            Path found = matchAlias(name, bold, italic);
            if (found != null) {
                return found;
            }
        }
        // 3) 字族名与字体名直接匹配文件名
        for (String name : new String[]{family, fontName}) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            Path found = matchByName(name, bold, italic);
            if (found != null) {
                return found;
            }
        }
        // 2) 按衬线/非衬线、粗体、斜体的方位回退
        List<String> candidates = new ArrayList<>();
        if (serif) {
            candidates.add("serif/SimSun.ttf");
            candidates.add("serif/NotoSerifCJK-Regular.ttc");
        } else {
            candidates.add("truetype/wqy/wqy-microhei.ttc");
            candidates.add("truetype/arphic/uming.ttc");
            candidates.add("truetype/dejavu/DejaVuSans.ttf");
        }
        for (String candidate : candidates) {
            Path path = Paths.get("/usr/share/fonts").resolve(candidate);
            if (Files.isRegularFile(path)) {
                return path;
            }
        }
        return null;
    }

    /**
     * 系统字体目录（含 WSL 挂载的 Windows 字体目录）
     */
    private static final String[] SYSTEM_FONT_ROOTS = {
            "/usr/share/fonts", "/usr/local/share/fonts", "/mnt/c/Windows/Fonts",
            "/Library/Fonts", "C:/Windows/Fonts"};

    /**
     * 常见中文字族名到字体文件名的别名表
     * <p>
     * OFD 字体资源通常只声明 {@code FontName}/{@code FamilyName}（如“宋体”“黑体”），
     * 系统字体目录里的文件名则是 {@code simsun.ttc}/{@code simhei.ttf}，两者需要别名才能对应。
     * 别名按通用字体族给出，不针对具体文档。
     */
    private static final String[][] CJK_FONT_ALIASES = {
            {"宋体", "simsun"}, {"新宋体", "simsun"}, {"simsun", "simsun"},
            {"黑体", "simhei"}, {"simhei", "simhei"},
            {"楷体", "simkai"}, {"楷体_gb2312", "simkai"}, {"kaiti", "simkai"}, {"simkai", "simkai"},
            {"仿宋", "simfang"}, {"仿宋_gb2312", "simfang"}, {"simfang", "simfang"},
            {"微软雅黑", "msyh"}, {"microsoftyahei", "msyh"},
            {"等线", "deng"}, {"dengxian", "deng"},
            {"隶书", "simli"}, {"幼圆", "simyou"}, {"华文行楷", "stxingka"},
            {"华文楷体", "stkaiti"}, {"华文宋体", "stsong"}, {"华文黑体", "stheiti"},
            // 常见西文字族：系统中通常以 Liberation/DejaVu 字体的同度量版本存在
            {"couriernew", "liberationmono"}, {"courier", "liberationmono"},
            {"arial", "liberationsans"}, {"helvetica", "liberationsans"},
            {"timesnewroman", "liberationserif"}, {"times", "liberationserif"},
    };

    /**
     * 判断名称是否为可用的字体文件路径
     *
     * @param name 字族名或字体名
     * @return 字体文件路径；不是路径、文件不存在或格式不支持时返回 null
     */
    private static Path existingFontFile(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String candidate = name.trim();
        boolean looksLikePath = candidate.indexOf('/') >= 0 || candidate.indexOf('\\') >= 0
                || candidate.matches("^[A-Za-z]:.*");
        if (!looksLikePath) {
            return null;
        }
        Path path = Paths.get(candidate);
        if (Files.isRegularFile(path)) {
            return path;
        }
        /*
         * 路径可能来自其它平台（如 /usr/share/fonts/fonts/simhei.ttf）：
         * 用文件名在系统字体目录中再找一次，仍找不到则放弃。
         */
        Path fileName = path.getFileName();
        return fileName == null ? null : matchByName(fileName.toString(), false, false);
    }

    /**
     * 按中文字族名别名匹配字体文件
     *
     * @param name   字族名或字体名
     * @param bold   是否需要粗体
     * @param italic 是否需要斜体
     * @return 字体文件路径；没有别名或文件不存在时返回 null
     */
    private static Path matchAlias(String name, boolean bold, boolean italic) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        String lower = name.trim().toLowerCase().replaceAll("\\s+", "");
        for (String[] alias : CJK_FONT_ALIASES) {
            if (!lower.equals(alias[0])) {
                continue;
            }
            String stem = alias[1];
            String boldStem = bold ? stem + "bd" : stem;
            Path found = matchByName(boldStem, bold, italic);
            if (found == null && bold) {
                found = matchByName(stem, false, italic);
            }
            return found;
        }
        return null;
    }

    /**
     * 按字族名匹配系统字体文件
     *
     * @param name   字族名或字体名
     * @param bold   是否需要粗体
     * @param italic 是否需要斜体
     * @return 字体文件路径；未找到时返回 null
     */
    private static Path matchByName(String name, boolean bold, boolean italic) {
        String lower = name.toLowerCase();
        String[] roots = SYSTEM_FONT_ROOTS;
        for (String root : roots) {
            Path dir = Paths.get(root);
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (java.util.stream.Stream<Path> stream = Files.walk(dir, 4)) {
                Path found = stream
                        .filter(Files::isRegularFile)
                        .filter(p -> {
                            String fileName = p.getFileName().toString().toLowerCase();
                            if (!fileName.endsWith(".ttf") && !fileName.endsWith(".ttc") && !fileName.endsWith(".otf")) {
                                return false;
                            }
                            boolean boldMatch = !bold || fileName.contains("bold");
                            boolean italicMatch = !italic || fileName.contains("italic") || fileName.contains("oblique");
                            return boldMatch && italicMatch && fileName.contains(lower.replaceAll("\\s+", ""));
                        })
                        .findFirst()
                        .orElse(null);
                if (found != null) {
                    return found;
                }
            } catch (IOException | RuntimeException e) {
                logger.debug("扫描系统字体目录失败：{}", root, e);
            }
        }
        return null;
    }

    /**
     * 读取 OFD 内嵌字体文件
     *
     * @param ctFont OFD 字体资源
     * @return 字体文件字节；没有字体文件或读取失败时返回 null
     */
    private byte[] readEmbeddedFont(CT_Font ctFont) {
        if (ctFont.getFontFile() == null || ctFont.getFontFile().getLoc() == null) {
            return null;
        }
        String loc = splitTtc(ctFont.getFontFile().getLoc())[0];
        // 1) OFD 包内资源（资源管理器把 FontFile 转换为包内绝对路径）
        if (locator != null) {
            try (InputStream in = locator.getFile(loc).toFile().exists()
                    ? Files.newInputStream(locator.getFile(loc)) : null) {
                if (in != null) {
                    byte[] data = IOUtils.toByteArray(in);
                    if (data.length > 0) {
                        return data;
                    }
                }
            } catch (IOException | RuntimeException e) {
                logger.debug("从 OFD 包内读取字体失败：{}", loc, e);
            }
        }
        // 2) 系统文件路径
        Path path = Paths.get(loc);
        if (Files.isRegularFile(path)) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                logger.warn("读取字体文件失败：{}（{}）", loc, e.getMessage());
            }
        }
        return null;
    }

    /**
     * 字体集合内索引
     *
     * @param ctFont OFD 字体资源
     * @return 索引，单位：个；非字体集合时返回 0
     */
    private static int ttcIndexOf(CT_Font ctFont) {
        if (ctFont.getFontFile() == null || ctFont.getFontFile().getLoc() == null) {
            return 0;
        }
        try {
            return Integer.parseInt(splitTtc(ctFont.getFontFile().getLoc())[1]);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 拆分字体集合后缀 {@code 路径#索引}
     *
     * @param loc 字体文件路径
     * @return 下标 0 为路径，下标 1 为索引字符串
     */
    private static String[] splitTtc(String loc) {
        int index = loc.lastIndexOf(TTC_INDEX_SEPARATOR);
        if (index <= 0 || index == loc.length() - 1) {
            return new String[]{loc, "0"};
        }
        String suffix = loc.substring(index + 1);
        if (!suffix.chars().allMatch(Character::isDigit)) {
            return new String[]{loc, "0"};
        }
        return new String[]{loc.substring(0, index), suffix};
    }

    /**
     * 字体对象缓存键
     *
     * @param ctFont OFD 字体资源
     * @return 缓存键
     */
    private static String cacheKey(CT_Font ctFont) {
        String loc = ctFont.getFontFile() == null ? "" : String.valueOf(ctFont.getFontFile().getLoc());
        return safeName(ctFont) + "|" + loc + "|" + Boolean.TRUE.equals(ctFont.getBold())
                + "|" + Boolean.TRUE.equals(ctFont.getItalic());
    }

    /**
     * 字体名称
     *
     * @param ctFont OFD 字体资源
     * @return FontName；缺失时回退 FamilyName；都为 null 时返回空串
     */
    private static String safeName(CT_Font ctFont) {
        try {
            String name = ctFont.getFontName();
            if (name != null && !name.isEmpty()) {
                return name;
            }
        } catch (RuntimeException e) {
            logger.debug("读取 FontName 失败", e);
        }
        try {
            String family = ctFont.getFamilyName();
            return family == null ? "" : family;
        } catch (RuntimeException e) {
            return "";
        }
    }

    /**
     * 取第一个非空字符串
     *
     * @param values 候选值
     * @return 第一个非空值；全部为空时返回 null
     */
    private static String firstNonEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty()) {
                return value;
            }
        }
        return null;
    }

    /**
     * 由字体数据快速解析字族名（供日志与诊断）
     *
     * @param fontData 字体文件字节
     * @return 字族名；解析失败时返回 null
     */
    public static String probeFamilyName(byte[] fontData) {
        try {
            TrueTypeFont font = new TrueTypeFont().parse(fontData);
            return font.fontFamily;
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
