package org.ofdrw.converter;


import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.ItextFontUtil;
import com.itextpdf.io.font.ItextTrueTypeFont;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.apache.commons.io.IOUtils;
import org.ofdrw.converter.font.*;
import org.ofdrw.converter.utils.OSinfo;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * 字体加载器
 *
 * @author qaqtutu
 * @since 2021-04-13 19:00:39
 */
public final class FontLoader {
    private static final Logger log = LoggerFactory.getLogger(FontLoader.class);

    public static boolean DEBUG = false;

    /**
     * KEY: 字族名、字体名、PSName，如 SimSun、SitkaBanner-Bold
     * VALUE: 字体所在绝对路径
     */
    private final Map<String, String> fontNamePathMapping = new ConcurrentHashMap<>();
    /**
     * KEY: 字体可替换名称（支持正则）
     * VALUE: 字体所在绝对路径
     */
    private final Map<String, String> fontNameAliasMapping = new ConcurrentHashMap<>();

    /**
     * 使用正则匹配映射到系统或嵌入字体
     */
    private final Map<Pattern, String> similarFontReplaceRegexMapping = new ConcurrentHashMap<>();

    private static final String DEFAULT_FONT_DIR_MAC = "/System/Library/Fonts";
    private static final String DEFAULT_FONT_DIR_WINDOWS = "C:/Windows/Fonts";
    private static final String DEFAULT_FONT_DIR_LINUX = "/usr/share/fonts";

    /**
     * Linux 下除默认目录外还会尝试扫描的常见中文字体目录
     * <p>
     * 目录不存在时自动跳过，不影响既有环境；如需扩展可自行调用
     * {@link #scanFontDir(File)} 追加字体目录后再调用 {@link #init()} 之外的字体加载接口。
     */
    private static final String TTC_INDEX_SEPARATOR = "#";

    private static final String[] LINUX_EXTRA_FONT_DIRS = {
            "/usr/share/fonts/opentype/noto",
            "/usr/local/share/fonts",
            "/usr/share/wine/fonts",
            "/opt/share/fonts"
    };

    /**
     * 用于判断字体能否覆盖中文的常用汉字码位，单位：Unicode 码位
     * <p>
     * 同时覆盖简体与繁体常用字，避免把只含部分汉字的字体误判为可用的中文字体。
     */
    private static final char[] CJK_PROBE_CHARS = {'中', '国', '文', '字', '体', '測', '試'};

    /**
     * 默认字体选择的优先族名顺序
     * <p>
     * 依次按这些族名（含别名表与相似字体规则）查找系统字体，取第一个能覆盖汉字的字体作为默认字体。
     */
    private static final String[] CJK_DEFAULT_FAMILY_ORDER = {
            "宋体", "SimSun", "楷体", "KaiTi", "仿宋", "FangSong", "黑体", "SimHei",
            "微软雅黑", "Microsoft YaHei", "STHeiti-Light", "Noto Sans CJK SC", "Noto Serif CJK SC",
            "Source Han Sans SC", "Source Han Serif SC", "WenQuanYi Zen Hei", "WenQuanYi Micro Hei"
    };

    /**
     * 中文字族的别名映射：KEY 为常见中文族名或英文名（小写），VALUE 为可能的等价字体名
     * <p>
     * 用于在系统字体表中查找中文族名对应的字体（如“宋体” ↔ SimSun）。
     */
    private static final Map<String, String[]> CJK_FAMILY_ALIASES = new LinkedHashMap<>();

    static {
        CJK_FAMILY_ALIASES.put("宋体", new String[]{"simsun", "宋体", "songti", "stsong", "notoserifcjksc", "notosanscjksc"});
        CJK_FAMILY_ALIASES.put("simsun", new String[]{"宋体", "songti"});
        CJK_FAMILY_ALIASES.put("nsimsun", new String[]{"新宋体"});
        CJK_FAMILY_ALIASES.put("黑体", new String[]{"simhei", "黑体", "heiti", "stheiti", "notosanscjksc"});
        CJK_FAMILY_ALIASES.put("simhei", new String[]{"黑体", "heiti"});
        CJK_FAMILY_ALIASES.put("楷体", new String[]{"kaiti", "楷体", "simkai", "stkaiti", "kaiti_gb2312", "notoserifcjksc", "notosanscjksc"});
        CJK_FAMILY_ALIASES.put("kaiti", new String[]{"楷体"});
        CJK_FAMILY_ALIASES.put("仿宋", new String[]{"fangsong", "仿宋", "simfang", "stfangsong", "notoserifcjksc", "notosanscjksc"});
        CJK_FAMILY_ALIASES.put("fangsong", new String[]{"仿宋"});
        CJK_FAMILY_ALIASES.put("微软雅黑", new String[]{"microsoft yahei", "microsoftyahei", "微软雅黑", "msyh", "notosanscjksc"});
        CJK_FAMILY_ALIASES.put("microsoft yahei", new String[]{"微软雅黑", "msyh"});
        CJK_FAMILY_ALIASES.put("等线", new String[]{"dengxian", "等线"});
        CJK_FAMILY_ALIASES.put("方正小标宋简体", new String[]{"fzxbsjw", "小标宋体", "方正小标宋"});
        CJK_FAMILY_ALIASES.put("华文宋体", new String[]{"stsong", "华文宋体"});
        CJK_FAMILY_ALIASES.put("华文楷体", new String[]{"stkaiti", "华文楷体"});
        CJK_FAMILY_ALIASES.put("华文黑体", new String[]{"stheiti", "华文黑体"});
        CJK_FAMILY_ALIASES.put("noto sans cjk sc", new String[]{"notosanscjksc", "思源黑体"});
        CJK_FAMILY_ALIASES.put("noto serif cjk sc", new String[]{"notoserifcjksc", "思源宋体"});
    }

    /**
     * 默认字体
     */
    private static TrueTypeFont defaultFont;
    private static com.itextpdf.io.font.TrueTypeFont iTextDefaultFont;

    /**
     * 默认字体路径
     */
    private static Path DefaultFontPath;

    /**
     * 设置为volatile，防止后续 syncInit() 方法中 instance = singleInstance 语句的指令重排序
     */
    private static volatile FontLoader instance = null;

    private boolean enableSimilarFontReplace = true;

    private FontLoader() {
    }

    /**
     * 防止因为并发造成的异常
     * <p>
     * 初始化字体加载器 单例
     */
    private static synchronized void syncInit() {
        if (instance == null) {
            FontLoader singleInstance = new FontLoader();
            // 初始化加载字体信息
            singleInstance.init();
            instance = singleInstance;
        }
    }

    /**
     * 判断字体是否为中文变体
     */
    private static boolean isChineseVariant(String psName, String fontFamily, String[] chineseIndicators) {
        if (psName == null && fontFamily == null) {
            return false;
        }
        String text = (psName != null ? psName : "") + " " + (fontFamily != null ? fontFamily : "");
        text = text.toLowerCase();
        for (String indicator : chineseIndicators) {
            if (text.contains(indicator.toLowerCase())) {
                return true;
            }
        }
        return false;
    }


    /**
     * 是否开启 字体替换，替换规则 similarFontReplaceRegexMapping
     *
     * @return 实例
     * @deprecated {@link #setSimilarFontReplace}
     */
    @Deprecated
    public static FontLoader enableSimilarFontReplace() {
        FontLoader instance = getInstance();
        instance.enableSimilarFontReplace = true;
        return instance;
    }

    /**
     * 设置是否开启相近字体替换
     * <p>
     * 该方法用于在字体无法识别的情况下采用默认的字体进行替换
     * 防止渲染时字体内容缺失。
     *
     * @param enable true - 开启(默认); false - 关闭
     * @return 实例
     */
    public static FontLoader setSimilarFontReplace(boolean enable) {
        FontLoader instance = getInstance();
        instance.enableSimilarFontReplace = enable;
        return instance;
    }


    /**
     * 获取字体加载器实例并加载程序
     *
     * @return 字体加载器
     */
    public static FontLoader getInstance() {
        if (instance == null) {
            syncInit();
        }
        return instance;
    }

    /**
     * 预加载字体
     * <p>
     * 扫描操作系统内字体,功能与{@link #getInstance()} 一致
     *
     * @return 字体加载器
     */
    public static FontLoader Preload() {
        return getInstance();
    }

    /*
     * 加载默认字体
     * 加载系统字体
     * */
    public void init() {
        String username = System.getProperties().getProperty("user.name");
        if (OSinfo.isWindows()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_WINDOWS));
            // 扫描用户字体目录
            scanFontDir(new File(String.format("C:\\Users\\%s\\AppData\\Local\\Microsoft\\Windows\\Fonts", username)));
        } else if (OSinfo.isMacOS()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
        } else if (OSinfo.isMacOSX()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
            scanFontDir(new File(String.format("/Users/%s/Library/Fonts/", username)));
        } else if (OSinfo.isLinux()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_LINUX));
            // 常见的中文字体安装位置（仅在目录存在时扫描，不存在则跳过，不影响既有环境）
            for (String dir : LINUX_EXTRA_FONT_DIRS) {
                scanFontDir(new File(dir));
            }
            scanFontDir(new File(String.format("/home/%s/.fonts/", username)));
            scanFontDir(new File(String.format("/home/%s/.local/share/fonts/", username)));
        }

        addAliasMapping("小标宋体", "方正小标宋简体");
        addAliasMapping("KaiTi_GB2312", "楷体");
        addAliasMapping("楷体", "KaiTi");
        addAliasMapping("宋体", "SimSun");

        addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体");
        addSimilarFontReplaceRegexMapping(".*Kai.*", "楷体");
        addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun");
        addSimilarFontReplaceRegexMapping(".*SimSun.*", "SimSun");
        addSimilarFontReplaceRegexMapping(".*Song.*", "宋体");
        addSimilarFontReplaceRegexMapping(".*MinionPro.*", "SimSun");

        selectDefaultFont();
    }

    /**
     * 选择默认字体
     * <p>
     * 优先选择能覆盖中文字形的字体，避免无内嵌字体的中文文档在渲染时退化为方块：
     * <ol>
     *     <li>按中文字体族名（宋体 → 楷体 → 仿宋 → 黑体 → 微软雅黑 → 常见 CJK 字体）查找并加载；</li>
     *     <li>在已扫描的字体表中查找能覆盖常用汉字的字体；</li>
     *     <li>以上都没有时退回原有行为：取第一个可成功加载的字体。</li>
     * </ol>
     * 初始化阶段不会抛出异常，系统无任何可用字体时默认字体为 null，由调用方按既有逻辑处理。
     */
    private void selectDefaultFont() {
        Set<String> candidates = new LinkedHashSet<>();
        for (String family : CJK_DEFAULT_FAMILY_ORDER) {
            String path = getReplaceSimilarFontPath(family, "");
            if (path != null) {
                candidates.add(path);
            }
        }
        // 已扫描字体表中能覆盖汉字的字体
        synchronized (fontNamePathMapping) {
            candidates.addAll(fontNamePathMapping.values());
        }
        // 优先选能覆盖汉字的字体；没有任何中文字体时退回“第一个可加载的字体”，保持既有行为
        String firstLoadable = null;
        for (String path : candidates) {
            if (firstLoadable == null && loadAsDefaultFont(path)) {
                if (supportsChinese(defaultFont)) {
                    if (DEBUG) {
                        log.info("已选择默认字体（含中文）: {}", path);
                    }
                    return;
                }
                firstLoadable = path;
                if (DEBUG) {
                    log.info("候选字体不含中文字形: {}，继续查找", path);
                }
            }
        }
        if (firstLoadable != null) {
            loadAsDefaultFont(firstLoadable);
            if (DEBUG) {
                log.info("未找到含中文字形的字体，默认字体退回: {}", firstLoadable);
            }
            return;
        }
        if (DEBUG) {
            log.warn("未找到可用字体，默认字体为空");
        }
    }

    /**
     * 解析字体数据中指定索引的字体
     * <p>
     * 字体集合（TTC）取指定索引的字体（越界时取最后一个）；单个字体文件直接解析。
     * iText 默认字体仍按整份数据加载，以兼容 iText 对字体集合的处理。
     *
     * @param buf      字体文件数据
     * @param ttcIndex 字体集合内索引，单位：个，非字体集合时忽略
     * @return 解析出的字体
     * @throws IOException 字体数据无法解析
     */
    private static TrueTypeFont parseFontAt(byte[] buf, int ttcIndex) throws IOException {
        if (buf != null && buf.length >= 4 && buf[0] == 't' && buf[1] == 't' && buf[2] == 'c' && buf[3] == 'f') {
            TrueTypeCollection ttc = new TrueTypeCollection().parse(new MemoryTTFDataStream(buf));
            int fonts = ttc.getNumFonts();
            if (fonts > 0) {
                return ttc.getFontAtIndex(Math.max(0, Math.min(ttcIndex, fonts - 1)));
            }
        }
        return new TrueTypeFont().parse(buf);
    }

    /**
     * 在字体集合中挑选与指定名称匹配的字体索引
     * <p>
     * 依次按 PostScript 名、字族名精确匹配，再按包含匹配；都不匹配时取索引 0。
     *
     * @param ttc        字体集合
     * @param familyName 字族名，可为 null
     * @param fontName   字体名，可为 null
     * @return 字体索引，范围 [0, numFonts)
     * @throws IOException 字体集合解析异常
     */
    private static int findTtcFontIndex(TrueTypeCollection ttc, String familyName, String fontName) throws IOException {
        String[] probes = {fontName, familyName};
        for (String probe : probes) {
            if (probe == null || probe.isEmpty()) {
                continue;
            }
            for (int i = 0; i < ttc.getNumFonts(); i++) {
                TrueTypeFont font = ttc.getFontAtIndex(i);
                if (probe.equalsIgnoreCase(font.psName) || probe.equalsIgnoreCase(font.fontFamily)) {
                    return i;
                }
            }
        }
        for (String probe : probes) {
            if (probe == null || probe.isEmpty()) {
                continue;
            }
            for (int i = 0; i < ttc.getNumFonts(); i++) {
                TrueTypeFont font = ttc.getFontAtIndex(i);
                if ((font.psName != null && font.psName.contains(probe))
                        || (font.fontFamily != null && font.fontFamily.contains(probe))) {
                    return i;
                }
            }
        }
        return 0;
    }

    /**
     * 从字体文件路径读取字体集合中与名称匹配的字体名称，用于给字体路径附加索引标记
     *
     * @param path       字体文件路径
     * @param familyName 字族名，可为 null
     * @param fontName   字体名，可为 null
     * @return 字体集合内的索引；非字体集合或无法解析时返回 0
     */
    private static int ttcIndexAt(String path, String familyName, String fontName) {
        try {
            byte[] buf = Files.readAllBytes(Paths.get(path));
            if (buf.length < 4 || buf[0] != 't' || buf[1] != 't' || buf[2] != 'c' || buf[3] != 'f') {
                return 0;
            }
            TrueTypeCollection ttc = new TrueTypeCollection().parse(new MemoryTTFDataStream(buf));
            if (ttc.getNumFonts() <= 1) {
                return 0;
            }
            return findTtcFontIndex(ttc, familyName, fontName);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 给字体集合路径附加索引标记
     * <p>
     * 字体集合（TTC）内含多个字体，路径本身无法表达“用哪一个”，
     * 这里以 {@code 路径#索引} 的形式标记，加载时由 {@link #splitTtcIndex(String)} 解析。
     *
     * @param path       字体文件路径
     * @param familyName 字族名，可为 null
     * @param fontName   字体名，可为 null
     * @return 字体路径，字体集合时附带索引标记
     */
    private static String withTtcIndex(String path, String familyName, String fontName) {
        int index = ttcIndexAt(path, familyName, fontName);
        return index > 0 ? path + TTC_INDEX_SEPARATOR + index : path;
    }

    /**
     * 拆分字体集合索引标记
     *
     * @param path 可能带 {@code #索引} 标记的字体路径
     * @return 长度为 2 的数组：[0] 实际路径，[1] 字体集合内索引
     */
    private static String[] splitTtcIndex(String path) {
        if (path == null) {
            return new String[]{null, "0"};
        }
        int pos = path.lastIndexOf(TTC_INDEX_SEPARATOR);
        if (pos > 0 && path.indexOf('.', pos) == -1) {
            String suffix = path.substring(pos + 1);
            if (!suffix.isEmpty() && suffix.chars().allMatch(Character::isDigit)) {
                return new String[]{path.substring(0, pos), suffix};
            }
        }
        return new String[]{path, "0"};
    }

    /**
     * 判断字符串中是否包含汉字
     *
     * @param text 待判断文本
     * @return true - 含汉字；false - 不含汉字或文本为空
     */
    private static boolean containsChinese(String text) {
        if (text == null) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) >= 0x4E00 && text.charAt(i) <= 0x9FFF) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断字体能否覆盖常用汉字
     * <p>
     * 优先按 cmap 判断（需覆盖全部探测汉字）；字体没有 cmap 或 cmap 无法判断时，
     * 退回按字体名/字族名是否包含 CJK、Song、Hei、Kai、Ming 等标识判断。
     *
     * @param font 字体
     * @return true - 字体具备常用汉字字形；false - 纯拉丁字体或字体为空
     */
    public static boolean supportsChinese(TrueTypeFont font) {
        if (font == null) {
            return false;
        }
        try {
            boolean hasCmap = true;
            for (char c : CJK_PROBE_CHARS) {
                if (font.getUnicodeCmapLookup().getGlyphId(c) == 0) {
                    hasCmap = false;
                    break;
                }
            }
            if (hasCmap) {
                return true;
            }
        } catch (Exception e) {
            // 字体没有 cmap，改用字体名判断
        }
        String names = (font.psName == null ? "" : font.psName) + " " + (font.fontFamily == null ? "" : font.fontFamily);
        String lower = names.toLowerCase(Locale.ROOT);
        for (String indicator : new String[]{"cjk", "song", "hei", "kai", "ming", "fangsong", "yahei", "宋", "黑", "楷", "仿", "明", "雅黑"}) {
            if (lower.contains(indicator)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按中文字族名查找系统字体路径
     * <p>
     * 依次尝试：原始名称直接匹配、中文族别名表（如“宋体” ↔ SimSun）、
     * 以及在已扫描字体表中做不区分大小写的包含匹配。
     *
     * @param familyName 字族名，可为 null
     * @param fontName   字体名，可为 null
     * @return 字体绝对路径，找不到时返回 null
     */
    private String findChineseFamilyFont(String familyName, String fontName) {
        String direct = getSystemFontPath(familyName, fontName);
        if (direct != null) {
            return direct;
        }
        return findChineseFamilyFontByAlias(familyName, fontName);
    }

    /**
     * 按中文族别名表与已扫描字体表查找字体路径
     *
     * @param familyName 字族名，可为 null
     * @param fontName   字体名，可为 null
     * @return 字体绝对路径（字体集合附带 #{索引} 标记），找不到时返回 null
     */
    private String findChineseFamilyFontByAlias(String familyName, String fontName) {
        for (String name : new String[]{fontName, familyName}) {
            if (name == null || name.isEmpty()) {
                continue;
            }
            String key = name.trim().toLowerCase(Locale.ROOT);
            String[] aliases = CJK_FAMILY_ALIASES.get(key);
            if (aliases == null) {
                continue;
            }
            for (String alias : aliases) {
                String path = getSystemFontPath(alias, alias);
                if (path != null) {
                    return withTtcIndex(path, name, alias);
                }
            }
        }
        // 兜底：在已扫描字体表中做不区分大小写的包含匹配
        String probe = fontName != null && !fontName.isEmpty() ? fontName : familyName;
        if (probe != null && !probe.isEmpty()) {
            String lower = probe.trim().toLowerCase(Locale.ROOT);
            boolean probeIsChinese = containsChinese(lower);
            synchronized (fontNamePathMapping) {
                for (Map.Entry<String, String> entry : fontNamePathMapping.entrySet()) {
                    String name = entry.getKey().toLowerCase(Locale.ROOT);
                    if (name.equals(lower)) {
                        return withTtcIndex(entry.getValue(), fontName, familyName);
                    }
                    /*
                     * 仅当待查名称与字体名都含汉字时才做包含匹配，
                     * 避免 fangsong（仿宋）因包含 song 而误命中 Song 类字体。
                     */
                    if (probeIsChinese && containsChinese(name) && name.contains(lower)) {
                        return withTtcIndex(entry.getValue(), fontName, familyName);
                    }
                }
            }
        }
        return null;
    }

    /**
     * 尝试加载为默认字体
     *
     * @param path 字体路径
     * @return true - 加载成功；false - 加载失败
     */
    public static boolean loadAsDefaultFont(String path) {
        if (DEBUG) {
            log.info("尝试加载为默认字体：{}", path);
        }
        String[] split = splitTtcIndex(path);
        path = split[0];
        int ttcIndex = Integer.parseInt(split[1]);
        InputStream in = null;
        try {
            Path loc = Paths.get(path);
            in = Files.newInputStream(loc);
            byte[] buf = IOUtils.toByteArray(in);
            DefaultFontPath = loc;
            defaultFont = parseFontAt(buf, ttcIndex);
            // 默认文本字体已可用；iText 字体加载失败（如字体集合）时退化为按路径加载，不影响默认字体选择
            try {
                // 使用统一的工具类加载iText字体，对裁剪字体进行兼容
                iTextDefaultFont = ItextFontUtil.loadFont(buf);
            } catch (Exception e) {
                // 字体集合等无法直接解析的数据，按路径加载取集合内首个字体
                com.itextpdf.io.font.FontProgram program = ItextFontUtil.loadFontProgram(path);
                iTextDefaultFont = program instanceof com.itextpdf.io.font.TrueTypeFont
                        ? (com.itextpdf.io.font.TrueTypeFont) program : null;
            }
        } catch (Exception ignored) {
            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }
        return true;
    }

    /**
     * 追加字体映射
     *
     * @param familyName      字族名
     * @param fontName        字体名
     * @param aliasFamilyName 字族别名
     * @param aliasFontName   字体别名
     * @return this
     * @deprecated {@link #addAliasMapping(String, String)}
     */
    @Deprecated
    public FontLoader addAliasMapping(String familyName, String fontName, String aliasFamilyName, String aliasFontName) {
        addAliasMapping(familyName, aliasFamilyName);
        addAliasMapping(fontName, aliasFontName);
        return this;
    }

    /**
     * 添加字体映射
     *
     * @param fontName 字体名称
     * @param alias    字体别名
     * @return this
     */
    public FontLoader addAliasMapping(String fontName, String alias) {
        if (fontName == null || fontName.length() == 0) {
            return this;
        }
        if (alias == null || alias.length() == 0) {
            return this;
        }
        fontNameAliasMapping.put(fontName, alias);
        return this;
    }

    /**
     * 追加相似字体正则匹配规则
     *
     * @param familyNameRegex 字族名匹配规则
     * @param fontNameRegex   字体名匹配规则
     * @param aliasFamilyName 字族别名
     * @param aliasFontName   字体别名
     * @return this
     * @deprecated {@link #addSimilarFontReplaceRegexMapping(String, String)}
     */
    @Deprecated
    public FontLoader addSimilarFontReplaceRegexMapping(String familyNameRegex, String fontNameRegex, String aliasFamilyName, String aliasFontName) {
        addSimilarFontReplaceRegexMapping(familyNameRegex, aliasFamilyName);
        addSimilarFontReplaceRegexMapping(fontNameRegex, aliasFontName);
        return this;
    }

    /**
     * 追加相似字体正则匹配规则
     *
     * @param fontNameRegex 字体名匹配规则
     * @param fontName      相近替换字体名
     * @return this
     */
    public FontLoader addSimilarFontReplaceRegexMapping(String fontNameRegex, String fontName) {
        if (fontNameRegex == null || fontNameRegex.length() == 0) {
            return this;
        }
        if (fontName == null || fontName.length() == 0) {
            return this;
        }
        try {
            // 编译正则表达式，加速查询
            final Pattern pattern = Pattern.compile(fontNameRegex);
            similarFontReplaceRegexMapping.put(pattern, fontName);
        } catch (Exception ignored) {
        }
        return this;
    }

    /**
     * 增加字体映射
     * <p>
     * 用于解决部分字体不存在时的替代
     *
     * @param familyName   字族名
     * @param fontName     字体名
     * @param fontFilePath 字体位置
     * @return this
     * @deprecated {@link #addSystemFontMapping(String, String)}
     */
    @Deprecated
    public FontLoader addSystemFontMapping(String familyName, String fontName, String fontFilePath) {
        addSystemFontMapping(familyName, fontFilePath);
        addSystemFontMapping(fontName, fontFilePath);
        return this;
    }

    /**
     * 增加字体映射
     * <p>
     * 用于解决部分字体不存在时的替代
     *
     * @param fontName     字体名
     * @param fontFilePath 字体位置
     * @return this
     */
    public FontLoader addSystemFontMapping(String fontName, String fontFilePath) {
        if (fontName == null || fontName.length() == 0) {
            return this;
        }
        File file = new File(fontFilePath);
        if (!file.exists() || file.isDirectory()) {
            log.info("字体映射添加失败，字体文件 {} 不存在", fontFilePath);
            return this;
        }

        final String name = file.getName().toLowerCase();
        if (!name.endsWith("otf") && !name.endsWith("ttf") && !name.endsWith("ttc")) {
            log.info("字体映射添加失败 {} 不是一个OpenType字体", fontFilePath);
            return this;
        }
        synchronized (fontNamePathMapping) {
            String p = fontNamePathMapping.get(fontName);
            if (p != null && p.equals(fontFilePath)) {
                return this;
            }

            fontNamePathMapping.put(fontName, fontFilePath);
            if (DEBUG) {
                log.info("建立字体映射 {} --> {}", fontName, fontFilePath);
            }

        }
        return this;
    }

    /**
     * 从操作系统字体目下获取字体路径
     *
     * @param familyName 字族名（用于在字体不存在是替代字体，可为空）
     * @param fontName   字体名
     * @return 字体操作系统内绝对路径，如果不存在返还null
     */
    public String getSystemFontPath(String familyName, String fontName) {
        if (fontName == null && familyName == null) {
            return null;
        }

        String fontPath = null;
        if (fontName != null) {
            fontPath = fontNamePathMapping.get(fontName);
        }
        if (fontPath == null && familyName != null) {
            // 通过字体名称找不到字体时，使用字族名替换字形名称查找
            fontPath = fontNamePathMapping.get(familyName);
        }
        // 如果字体路径找到，那么返回
        if (fontPath != null) {
            return fontPath;
        }

        // 没有找到式，检查别名中是否存在
        String name = fontNameAliasMapping.get(fontName);
        if (name != null && fontNamePathMapping.containsKey(name)) {
            return fontNamePathMapping.get(name);
        }

        // 如果 familyName 不为 null，尝试通过 familyName 别名查找
        if (familyName != null) {
            name = fontNameAliasMapping.get(familyName);
            if (name != null && fontNamePathMapping.containsKey(name)) {
                return fontNamePathMapping.get(name);
            }
        }

        return null;
    }


    /**
     * 获取配置的 相似字体 对应的字体路径
     *
     * @param familyName 字族名（用于在字体不存在是替代字体，可为空）
     * @param fontName   字体名
     * @return 字体操作系统内绝对路径，如果不存在返还 null
     */
    public String getReplaceSimilarFontPath(String familyName, String fontName) {
        if (fontName == null && familyName == null) {
            return null;
        }
        // 首先尝试使用名称直接查找
        String fontPath = getSystemFontPath(familyName, fontName);
        if (fontPath != null) {
            return fontPath;
        }

        String name = null;
        for (Map.Entry<Pattern, String> entry : similarFontReplaceRegexMapping.entrySet()) {
            Pattern pattern = entry.getKey();
            if (fontName != null && pattern.matcher(fontName).matches()) {
                name = entry.getValue();
                break;
            }
            if (familyName != null && pattern.matcher(familyName).matches()) {
                // 通过字体名称无法找到时，使用字族名替换
                name = entry.getValue();
                break;
            }
        }
        if (name != null) {
            fontPath = getSystemFontPath(null, name);
            if (fontPath != null) {
                // 用规则命中的字体名再选一次集合内字体，避免取到集合中同名的其他字重
                return withTtcIndex(fontPath, fontName, name);
            }
        }

        // 中文族名（宋体/黑体/楷体/仿宋/微软雅黑等）按别名表与已扫描字体表匹配
        fontPath = findChineseFamilyFont(familyName, fontName);
        if (fontPath != null) {
            return fontPath;
        }
        // 规则命中的字体名再次走别名表（例如“仿宋”命中“宋体”规则时回落到宋体）
        return name == null ? null : findChineseFamilyFont(name, name);
    }


    /**
     * 尝试从系统字体目录中加载字体
     *
     * @param familyName 字族名
     * @param fontName   字体名
     * @return 字体或null
     */
    public TrueTypeFont loadSystemFont(String familyName, String fontName) {
        // 尝试获取字体路径
        String fontFilePath = getSystemFontPath(familyName, fontName);
        if (fontFilePath == null) {
            return null;
        }
        return loadExternalFont(fontFilePath, familyName, fontName);
    }

    /**
     * 加载外部字体
     *
     * @param absPath 绝对路径
     * @return null或外部字体
     * @deprecated {@link #loadExternalFont(String, String, String)}}
     */
    @Deprecated
    public TrueTypeFont loadExternalFont(String absPath) {
        return loadExternalFont(absPath, null, null);
    }

    /**
     * 加载外部字体
     *
     * @param absPath    字体操作系统绝对路径
     * @param familyName 字族名，因为是可选参数忽略
     * @param fontName   字体名
     * @return 字体
     */
    public TrueTypeFont loadExternalFont(String absPath, String familyName, String fontName) {
        // 字体集合路径可能带有“#索引”标记，加载真实文件路径并记下集合内索引
        String[] split = splitTtcIndex(absPath);
        absPath = split[0];
        int ttcIndex = Integer.parseInt(split[1]);
        try {
            // 内存中不用主动关闭：MemoryTTFDataStream(InputStream) 会读取全部字节并关闭底层文件流
            TTFDataStream raf = new MemoryTTFDataStream(new FileInputStream(absPath));
            int offset = absPath.toLowerCase().lastIndexOf('.');
            String suffix = offset == -1 ? "" : absPath.toLowerCase().substring(offset);
            switch (suffix) {
                case ".ttf":
                case ".otf":
                case ".cff":
                case ".otc":
                    // 扩展名仅作提示，字体格式以文件内容（魔法值）为准：
                    // TrueTypeFont.parse 内部能够识别 sfnt（0x00010000 / 'true' / 'OTTO'）、裸 CFF（0x01000402 / 0x01000403）与 Type1
                    return new TrueTypeFont().parse(raf);
                case ".ttc":
                    TrueTypeCollection ttc = new TrueTypeCollection().parse(raf);
                    if (ttcIndex > 0 && ttcIndex < ttc.getNumFonts()) {
                        // 路径中已指定字体集合内的索引，直接取该字体
                        return ttc.getFontAtIndex(ttcIndex);
                    }
                    TrueTypeFont selectedFont = null;

                    // 中文字体标识，用于优先选择中文变体
                    String[] chineseIndicators = {"SC","TC","HK","TW","JP","KR","CJK"};

                    boolean hasExactMatch = false;

                    // 单次遍历完成所有匹配逻辑
                    for (int i = 0; i < ttc.getNumFonts(); i++) {
                        TrueTypeFont font = ttc.getFontAtIndex(i);
                        String psName = font.psName;

                        // 1. 精确匹配 PS Name（最高优先级）
                        if (fontName != null && psName != null && psName.equals(fontName)) {
                            selectedFont = font;
                            hasExactMatch = true;
                            break;
                        }

                        // 2. 如果还没有选中，评估字体优先级
                        if (selectedFont == null) {
                            // 第一次遍历，先选择第一个字体作为后备
                            selectedFont = font;
                        } else {
                            // 比较优先级：检查是否为中文变体
                            boolean currentIsChinese = isChineseVariant(selectedFont.psName, selectedFont.fontFamily, chineseIndicators);
                            boolean candidateIsChinese = isChineseVariant(psName, font.fontFamily, chineseIndicators);

                            // 优先选择中文变体
                            if (!currentIsChinese && candidateIsChinese) {
                                selectedFont = font;
                            }
                            // 如果两者都是或都不是中文变体，保持原选择（即第一个遇到的）
                        }
                    }

                    // 只有在没有精确匹配时，才尝试部分匹配
                    if (!hasExactMatch && fontName != null && selectedFont != null) {
                        for (int i = 0; i < ttc.getNumFonts(); i++) {
                            TrueTypeFont font = ttc.getFontAtIndex(i);
                            String psName = font.psName;
                            if (psName != null && psName.contains(fontName)) {
                                selectedFont = font;
                                break;
                            }
                        }
                    }

                    if (DEBUG) {
                        log.debug("TTC字体选择结果: 文件={}, 字体名={}, 字族名={}, 选中字体PSName={}, 字体族={}",
                                absPath,
                                fontName != null ? fontName : "null",
                                familyName != null ? familyName : "null",
                                selectedFont != null ? selectedFont.psName : "null",
                                selectedFont != null ? selectedFont.fontFamily : "null");
                    }
                    return selectedFont;
                default:
                    // OFD 内嵌字体文件的扩展名并不总是可靠，未知扩展名时按字体内容（魔法值）尝试解析
                    return new TrueTypeFont().parse(raf);
            }
        } catch (Exception e) {
            // 字体缺少必需表等 RuntimeException 同样视为加载失败，交由上层使用相似字体替换
            log.warn("字体 " + absPath + " 加载失败，将使用相似字体替换，原因: " + e.getMessage(), e);
        }
        return null;
    }


    /**
     * 加载字体
     *
     * <p>
     * 兼容性保留
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体或null
     */
    public TrueTypeFont loadFont(ResourceLocator rl, CT_Font ctFont) {
        return loadFontSimilar(rl, ctFont).getFont();
    }

    /**
     * 加载字体
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体或null
     */
    public FontWrapper<TrueTypeFont> loadFontSimilar(ResourceLocator rl, CT_Font ctFont) {
        if (ctFont == null) {
            return null;
        }
        // 是否使用了非内嵌字体（内嵌字体缺失、加载失败或被迫换字体）
        boolean hasReplace = false;
        // 字形索引是否已失去原字体的含义（此时按字形索引取轮廓会输出错乱字形）
        boolean useSimulatedGlyphIndex = false;
        // 文档是否内嵌了该字体
        boolean hasEmbeddedFont = ctFont.getFontFile() != null;

        TrueTypeFont trueTypeFont = null;
        // 当前加载到的替代字体（相似字体 / 默认字体），用于校验它能否覆盖文档文字
        TrueTypeFont probeFont = null;
        // 是否需要校验替代字体能否覆盖文档文字（内嵌字体加载失败 / 系统找不到 / 回落到默认字体）
        boolean checkTextCoverage = false;
        try {
            if (hasEmbeddedFont) {
                // 内嵌字体：通过资源加载器获取文件的绝对路径
                String fontAbsPath = rl.getFile(ctFont.getFontFile()).toAbsolutePath().toString();
                trueTypeFont = loadExternalFont(fontAbsPath, ctFont.getFamilyName(), ctFont.getFontName());
                checkTextCoverage = trueTypeFont == null;
            }
            if (trueTypeFont == null) {
                // 无法从内部加载时，通过相似字体查找
                String similarFontPath = getReplaceSimilarFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                boolean systemFontMatched = false;
                if (similarFontPath != null) {
                    // 传递原始字体名称，帮助 TTC 选择正确的字体索引
                    trueTypeFont = loadExternalFont(similarFontPath, ctFont.getFamilyName(), ctFont.getFontName());
                    systemFontMatched = true;
                    if (DEBUG && trueTypeFont != null) {
                        log.debug("相似字体替换成功: 原始字体={}/{}, 系统字体路径={}, 加载字体PSName={}",
                                ctFont.getFamilyName(), ctFont.getFontName(), similarFontPath, trueTypeFont.psName);
                    }
                }
                hasReplace = true;
                /*
                 * 文档未内嵌字体时，字形索引本就由文档作者按宿主系统的同名字体给出，
                 * 系统字体匹配成功属于正常加载，字形索引依然有效；
                 * 只有内嵌字体存在却加载失败，或系统也没有该字体、回落到默认字体时，
                 * 字形索引才失去原字体的含义。
                 */
                useSimulatedGlyphIndex = hasEmbeddedFont || !systemFontMatched;
                checkTextCoverage = !systemFontMatched;
                probeFont = trueTypeFont;
            }
        } catch (Exception e) {
            if (DEBUG) {
                log.warn("无法加载字体: " + ctFont.getFamilyName() + " " + ctFont.getFontName() + " " + ctFont.getFontFile(), e);
            }
            // 加载过程异常时按失败处理，字形索引视为已失去原字体含义
            useSimulatedGlyphIndex = true;
            checkTextCoverage = true;
        }
        if (trueTypeFont == null) {
            trueTypeFont = defaultFont;
            probeFont = defaultFont;
            hasReplace = true;
            useSimulatedGlyphIndex = true;
        }
        // 替代字体能否覆盖文档文字：不能覆盖时，跳过字形与绘制 .notdef 同样无法还原文字，渲染侧据此保留既有行为
        boolean replacementSupportsText = !checkTextCoverage || supportsTextGlyph(probeFont, ctFont);
        FontWrapper<TrueTypeFont> wrapper = new FontWrapper<>(trueTypeFont, hasReplace);
        // 记录字体来源信息，供字形索引有效性判断使用
        wrapper.setUseSimulatedGlyphIndex(useSimulatedGlyphIndex);
        wrapper.setEmbedded(hasEmbeddedFont);
        wrapper.setReplacementSupportsText(replacementSupportsText);
        return wrapper;
    }


    /**
     * 加载相近字体流
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体流
     */
    public InputStream loadFontSimilarStream(ResourceLocator rl, CT_Font ctFont) {
        byte[] buf = null;
        try {
            if (ctFont != null) {
                // 内嵌字体绝对路径
                ST_Loc fontFileLoc = ctFont.getFontFile();
                if (fontFileLoc != null) {
                    String fontAbsPath = rl.getFile(ctFont.getFontFile()).toAbsolutePath().toString();
                    buf = Files.readAllBytes(Paths.get(fontAbsPath));
                } else {

                    // 无法从内部加载时，通过相似字体查找
                    String similarFontPath = getReplaceSimilarFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                    if (similarFontPath != null) {
                        buf = Files.readAllBytes(Paths.get(similarFontPath));
                    }
                }
            }
        } catch (Exception e) {
            if (DEBUG) {
                log.warn("无法加载字体: " + ctFont.getFamilyName() + " " + ctFont.getFontName() + " " + ctFont.getFontFile(), e);
            }
        }

        if (buf == null || buf.length == 0) {
            try {
                buf = Files.readAllBytes(DefaultFontPath);
            } catch (IOException e) {
                throw new RuntimeException("默认字体文件读取异常", e);
            }
        }
        return new ByteArrayInputStream(buf);
    }

    /**
     * 获取字体别名
     *
     * @param ctFont 字体对象
     * @return
     */
    public String getFontAlias(CT_Font ctFont) {
        return fontNameAliasMapping.get(ctFont.getFontName());
    }

    /**
     * 尽可能的加载字体
     * <p>
     * 如果字体无法加载时使用相近字体替换
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @param isNoGlyphs 是否不存在字符索引
     * @return 字体 或 null
     */
    public FontWrapper<PdfFont> loadPDFFontSimilar(ResourceLocator rl, CT_Font ctFont, boolean isNoGlyphs) {
        if (ctFont == null) {
            return null;
        }

        /**
         * - 包含路径，尝试从OFD中加载字体
         *      - 失败了 继续流程
         *      - 成功就返还
         * - 不含路径，尝试从操作系统中加载字体
         */
        final String fontName = ctFont.attributeValue("FontName");
        final String familyName = ctFont.getFamilyName();
        try {
            ST_Loc fontFileLoc = ctFont.getFontFile();
            FontProgram fontProgram = null;

            // 1、尝试加载内嵌字体
            if (fontFileLoc != null) {
                String fontAbsPath = rl.getFile(fontFileLoc).toAbsolutePath().toString();
                fontProgram = getFontProgram(fontAbsPath);
            }

            // 是否要加载系统字体
            boolean isLoadSystemFont = false;
            // 源文件不存在索引标签且加载的映射表为空，也需要选择加载操作系统字体
            if (isNoGlyphs && fontProgram instanceof ItextTrueTypeFont) {
                isLoadSystemFont = ((ItextTrueTypeFont) fontProgram).isEmptyCmap;
            }
            if (DEBUG) {
                log.info("是否加载系统字体 {} {} {}", isLoadSystemFont, isNoGlyphs, fontProgram == null);
            }

            // 2、尝试根据名字从操作系统加载字体，只要替换了内嵌字体，就需要设置为需要替换
            boolean hasReplace = isLoadSystemFont || fontProgram == null;
            if (hasReplace) {
                // 首先尝试从操作系统
                String fontAbsPath = getSystemFontPath(familyName, fontName);
                if (fontAbsPath == null && enableSimilarFontReplace) {
                    // 操作系统中不存在，那么尝试使用近似的字体替换
                    fontAbsPath = getReplaceSimilarFontPath(familyName, fontName);
                }
                fontProgram = getFontProgram(fontAbsPath);
            }
            if (DEBUG) {
                log.info("加载PDF中的字体 status=加载{}, {}, {}, {}", fontProgram == null ? "失败" : "成功", familyName, fontName, ctFont.getFontFile());
            }

            // 3、前面两种加载机制都失效时，使用默认字体
            if (fontProgram == null) {
                fontProgram = iTextDefaultFont;
            }

            return new FontWrapper<>(PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED), hasReplace);
        } catch (Exception e) {
            if (DEBUG) {
                log.warn("加载字体异常 " + familyName + " " + fontName + " " + ctFont.getFontFile(), e);
            }
            return new FontWrapper<>(PdfFontFactory.createFont(iTextDefaultFont, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.PREFER_NOT_EMBEDDED), true);
        }

    }

    /**
     * 加载字体
     * <p>
     * 如果无法加载则返回null
     *
     * @param fontAbsPath 字体路径
     * @return 字体对象
     */
    private FontProgram getFontProgram(String fontAbsPath) {
        if (fontAbsPath == null) {
            return null;
        }
        FontProgram fontProgram = null;
		try {
		 // 使用统一的工具类加载iText字体，对裁剪字体进行兼容
			fontProgram = ItextFontUtil.loadFontProgram(fontAbsPath);
			return fontProgram;
		} catch (Exception e) {
			if (DEBUG) {
				log.info("已跳过 {} 字体文件解析，原因 {}" ,fontAbsPath, e.getMessage());
			}
			return null;
		}
    }


    /**
     * 加载默认字体
     *
     * @return 默认字体宋体
     */
    public TrueTypeFont loadDefaultFont() {
        return defaultFont;
    }


    /**
     * 扫描目录下所有字体并加兹安
     *
     * @param dir 目录
     */
    public void scanFontDir(File dir) {
        scanFontDir(dir.toPath());
    }

    /**
     * 扫描目录下所有字体并加兹安
     *
     * @param dirPath 目录路径
     */
    public void scanFontDir(Path dirPath) {
        if (dirPath == null || Files.notExists(dirPath) || Files.isRegularFile(dirPath)) {
            return;
        }
        dirPath = dirPath.toAbsolutePath();
        try (Stream<Path> walk = Files.walk(dirPath)) {
            walk.filter(Files::isRegularFile).forEach(p -> loadFont(p.toFile()));
        } catch (IOException e) {
            if (DEBUG) {
                log.warn("字体加载异常", e);
            }
        }
    }

    /**
     * 加载字体到映射中
     *
     * @param file 字体文件
     */
    public void loadFont(Path file) {
        loadFont(file.toFile());
    }

    /**
     * 加载字体到映射中
     * <p>
     * 支持：otf、ttf、ttc格式
     *
     * @param file 字体文件路径
     */
    public void loadFont(File file) {
        try {
            Font awtFont = Font.createFont(Font.TRUETYPE_FONT, file);
            // 根据地区加载字体名称映射，解决中英文字体名称不一致的问题
            Locale locale = Locale.getDefault();
            String family = awtFont.getFamily();
            if (null != family) {
                String fontName = awtFont.getFontName(locale);
                addSystemFontMapping(fontName, file.getAbsolutePath());
                if (!Locale.CHINA.equals(locale)) {
                    fontName = awtFont.getFontName(Locale.CHINA);
                    addSystemFontMapping(fontName, file.getAbsolutePath());
                }
                if (!Locale.CHINESE.equals(locale)) {
                    fontName = awtFont.getFontName(Locale.CHINESE);
                    addSystemFontMapping(fontName, file.getAbsolutePath());
                }
                if (!Locale.ENGLISH.equals(locale)) {
                    fontName = awtFont.getFontName(Locale.ENGLISH);
                    addSystemFontMapping(fontName, file.getAbsolutePath());
                }
            }
            return;
        } catch (Exception e) {
            if (DEBUG) {
                log.debug("AWT 无法解析字体 {}：{}，尝试按字体文件解析", file.getAbsolutePath(), e.getMessage());
            }
        }
        // AWT 解析失败（常见于 TTC 集合）时，按字体文件逐个登记字体名称
        loadFontByFile(file);
    }

    /**
     * 直接解析字体文件并登记其中每个字体的名称
     * <p>
     * 字体集合（TTC）按索引逐个登记，字体内部的名称（name 表）通常包含中文族名，
     * 可弥补 AWT 只暴露集合中第一个字体（且可能只有英文名）的不足。
     *
     * @param file 字体文件，支持 ttf、otf、ttc
     */
    private void loadFontByFile(File file) {
        String absPath = file.getAbsolutePath();
        try {
            byte[] data = Files.readAllBytes(file.toPath());
            List<TrueTypeFont> fonts = new ArrayList<>();
            if (data.length >= 4 && data[0] == 't' && data[1] == 't' && data[2] == 'c' && data[3] == 'f') {
                // 字体集合：逐个索引解析
                TrueTypeCollection ttc = new TrueTypeCollection().parse(new MemoryTTFDataStream(data));
                for (int i = 0; i < ttc.getNumFonts(); i++) {
                    try {
                        fonts.add(ttc.getFontAtIndex(i));
                    } catch (Exception e) {
                        if (DEBUG) {
                            log.debug("跳过字体集合 {} 的第 {} 个字体：{}", absPath, i, e.getMessage());
                        }
                    }
                }
            } else {
                fonts.add(new TrueTypeFont().parse(data));
            }
            for (TrueTypeFont font : fonts) {
                addSystemFontMapping(font.psName, absPath);
                addSystemFontMapping(font.fontFamily, absPath);
                addSystemFontMapping(font.fontSubFamily, absPath);
            }
        } catch (Exception e) {
            if (DEBUG) {
                log.info("已跳过 {} 字体文件解析，原因 {} ", absPath, e.getMessage());
            }
        }
    }

    /**
     * 修复了字体
     * <p>
     * 小写os/2导致无法读取的问题
     *
     * @param src 待修复字体文件路径
     * @throws IOException 文件读写IO异常
     */
    public static void FixOS2(String src) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(new File(src), "rws")) {
            // Version: 4 byte
            int v1 = raf.readUnsignedShort();
            int v2 = raf.readUnsignedShort();
            // Number of Tables: 2 byte
            int numberOfTables = raf.readUnsignedShort();
            // Search Range: 2 byte
            int searchRange = raf.readUnsignedShort();
            // Entry Selector: 2 byte
            int entrySelector = raf.readUnsignedShort();
            // Range Shift: 2 byte
            int rangeShift = raf.readUnsignedShort();
            for (int i = 0; i < numberOfTables; i++) {
                byte[] buff = new byte[4];
                raf.read(buff);                 // 4byte
                String tag = new String(buff, StandardCharsets.ISO_8859_1);
                int checkSum = raf.readInt();   // 4 byte
                int offset = raf.readInt();     // 4 byte
                int length = raf.readInt();     // 4 byte
                if (tag.equals("os/2")) {
                    long before = raf.getFilePointer(); // 游标
                    long p = before - 4 * 4; // 移动游标到TAG之前
                    raf.seek(p);
                    raf.write(new byte[]{'O', 'S', '/', '2'});
                    raf.seek(before);
                    break;
                }
            }
        }
    }

    /**
     * 默认字体解析模式
     *
     * @return 字体
     */
    public TrueTypeFont getDefaultFont() {
        return defaultFont;
    }

    /**
     * 默认字体iText格式
     *
     * @return 字体
     */
    public com.itextpdf.io.font.TrueTypeFont getITextDefaultFont() {
        return iTextDefaultFont;
    }

    /**
     * 获取默认字体文件路径
     *
     * @return 文件路径
     */
    public Path getDefaultFontPath() {
        return DefaultFontPath;
    }

    /**
     * 判断替代字体能否覆盖文档中该字体所需的字符
     * <p>
     * 用于识别“字体已替换、且替代字体根本不具备文档字符”的情形：此时无论按字形索引还是
     * 按 Unicode 取轮廓，都只能得到 .notdef 方块。判断方式是看替代字体的 cmap 能否把
     * 文档字体名/字族名中的非 ASCII 字符（如汉字）映射到非 0 的字形索引。
     *
     * @param font   当前加载到的替代字体，可为 null
     * @param ctFont 文档中的字体对象，其字体名与字族名用作探测字符
     * @return true - 替代字体具备文档所需的字形（或无法判断）；false - 缺少这些字形
     */
    private static boolean supportsTextGlyph(TrueTypeFont font, CT_Font ctFont) {
        if (font == null) {
            return false;
        }
        String fontName = ctFont.getFontName();
        String familyName = ctFont.getFamilyName();
        StringBuilder probe = new StringBuilder();
        if (fontName != null) {
            probe.append(fontName);
        }
        if (familyName != null) {
            probe.append(familyName);
        }
        int missing = 0;
        int total = 0;
        for (int i = 0; i < probe.length(); i++) {
            char c = probe.charAt(i);
            if (c < 0x80) {
                // 只用非 ASCII 字符（汉字等）判断字体是否具备文档所需的字形
                continue;
            }
            total++;
            int glyphId;
            try {
                glyphId = font.getUnicodeCmapLookup().getGlyphId(c);
            } catch (Exception e) {
                // 字体没有 cmap 时无法判断，按“可以覆盖”处理，保持既有行为
                return true;
            }
            if (glyphId == 0) {
                missing++;
            }
        }
        return total == 0 || missing < total;
    }
}
