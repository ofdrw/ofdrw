package org.ofdrw.converter;


import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.converter.font.*;
import org.ofdrw.converter.utils.OSinfo;
import org.ofdrw.core.Holder;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * 字体加载器
 *
 * @author qaqtutu
 * @since 2021-04-13 19:00:39
 */
public final class FontLoader {
    private static final Logger log = LoggerFactory.getLogger(FontLoader.class);

    private final Map<String, String> fontNamePathMapping = new ConcurrentHashMap<>();
    private final Map<String, String> fontNameAliasMapping = new ConcurrentHashMap<>();

    /**
     * 使用正则匹配映射到系统或嵌入字体
     */
    private final Map<Pattern, String> similarFontReplaceRegexMapping = new ConcurrentHashMap<>();

    private static final String DEFAULT_FONT_DIR_MAC = "/System/Library/Fonts";
    private static final String DEFAULT_FONT_DIR_WINDOWS = "C:/Windows/Fonts";
    private static final String DEFAULT_FONT_DIR_LINUX = "/usr/share/fonts";
    private static TrueTypeFont defaultFont;
    private static com.itextpdf.io.font.TrueTypeFont iTextDefaultFont;

    /**
     * 默认字体资源文件路径
     */
    private static final String DEFAULT_FONT_RESOURCE_PATH = "/fonts/simsun.ttf";

    private static final String BUILD_IN_FONT_DIR = "/fonts";

    private static FontLoader instance = null;

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
            instance = new FontLoader();
            // 初始化加载字体信息
            instance.init();
        }
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
        try (InputStream in = getClass().getResourceAsStream(DEFAULT_FONT_RESOURCE_PATH)) {
            byte[] buf = IOUtils.toByteArray(in);
            defaultFont = new TrueTypeFont().parse(buf);
            iTextDefaultFont = new com.itextpdf.io.font.TrueTypeFont(buf);
        } catch (IOException ignored) {
        }
        if (OSinfo.isWindows()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_WINDOWS));
            // 扫描用户字体目录
            String username = System.getProperties().getProperty("user.name");
            scanFontDir(new File(String.format("C:\\Users\\%s\\AppData\\Local\\Microsoft\\Windows\\Fonts", username)));
        } else if (OSinfo.isMacOS()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
        } else if (OSinfo.isMacOSX()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
        } else if (OSinfo.isLinux()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_LINUX));
        }

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
    public FontLoader addAliasMapping(@Nullable String familyName,
                                      String fontName,
                                      @Nullable String aliasFamilyName,
                                      String aliasFontName) {
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
    public FontLoader addSimilarFontReplaceRegexMapping(@Nullable String familyNameRegex,
                                                        String fontNameRegex,
                                                        @Nullable String aliasFamilyName,
                                                        String aliasFontName) {
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
    public FontLoader addSystemFontMapping(@Nullable String familyName, String fontName, String fontFilePath) {
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
            log.debug("字体映射添加失败，字体文件 {} 不存在", fontFilePath);
            return this;
        }

        final String name = file.getName().toLowerCase();
        if (!name.endsWith("otf") && !name.endsWith("ttf") && !name.endsWith("ttc")) {
            log.debug("字体映射添加失败 {} 不是一个OpenType字体", fontFilePath);
            return this;
        }
        synchronized (fontNamePathMapping) {
            fontNamePathMapping.put(fontName, fontFilePath);
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
    public String getSystemFontPath(@Nullable String familyName, String fontName) {
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
        name = fontNameAliasMapping.get(familyName);
        if (name != null && fontNamePathMapping.containsKey(name)) {
            return fontNamePathMapping.get(name);
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
    public String getReplaceSimilarFontPath(@Nullable String familyName, String fontName) {
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
        }

        return fontPath;
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
    public TrueTypeFont loadExternalFont(@NotNull String absPath, @Nullable String familyName, @Nullable String fontName) {
        try {
            // 内存中不用主动关闭
            TTFDataStream raf = new MemoryTTFDataStream(new FileInputStream(absPath));
            int offset = absPath.toLowerCase().lastIndexOf('.');
            String suffix = offset == -1 ? ".ttf" : absPath.toLowerCase().substring(offset);
            switch (suffix) {
                case ".ttf":
                case ".otf":
                    return new TrueTypeFont().parse(raf);
                case ".ttc":
                    Holder<TrueTypeFont> holder = new Holder<>();
                    TrueTypeCollection ttc = new TrueTypeCollection().parse(raf);

                    ttc.foreach(font -> {
                        if (font.psName.equals(fontName)) {
                            holder.value = font;
                        }
                    });
                    if (holder.value == null) {
                        holder.value = ttc.getFontAtIndex(0);
                    }
                    return holder.value;
            }
        } catch (IOException e) {
            log.info("字体 {},加载失败,原因:{}", absPath, e.getMessage());
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
        boolean hasReplace = false;

        TrueTypeFont trueTypeFont = null;
        try {
            // 内嵌字体绝对路径
            ST_Loc fontFileLoc = ctFont.getFontFile();

            if (fontFileLoc != null) {
                // 通过资源加载器获取文件的绝对路径
                String fontAbsPath = rl.getFile(ctFont.getFontFile()).toAbsolutePath().toString();
                trueTypeFont = loadExternalFont(fontAbsPath, ctFont.getFamilyName(), ctFont.getFontName());
            }
            if (trueTypeFont == null) {
                // 无法从内部加载时，通过相似字体查找
                String similarFontPath = getReplaceSimilarFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                if (similarFontPath != null) {
                    trueTypeFont = loadExternalFont(similarFontPath, null, null);
                }
                hasReplace = true;
            }
        } catch (Exception e) {
            log.info("无法加载字体: {} {} {}, {}" + ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile(), e);
        }
        if (trueTypeFont == null) {
            trueTypeFont = defaultFont;
            hasReplace = true;
        }
        return new FontWrapper<>(trueTypeFont, hasReplace);
    }


    /**
     * 加载相近字体流
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体流
     */
    public InputStream loadFontSimilarStream(ResourceLocator rl, CT_Font ctFont) {
        if (ctFont == null) {
            return getClass().getResourceAsStream(DEFAULT_FONT_RESOURCE_PATH);
        }
        byte[] buf = null;
        try {
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

        } catch (Exception e) {
            log.info("无法加载字体: {} {} {}" + ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
        }
        if (buf == null || buf.length == 0) {
            return getClass().getResourceAsStream(DEFAULT_FONT_RESOURCE_PATH);
        }
        return new ByteArrayInputStream(buf);
    }


    /**
     * 加载字体
     * <p>
     * 兼容性保留
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体 或 null
     */
    public PdfFont loadPDFFont(ResourceLocator rl, CT_Font ctFont) {
        return this.loadPDFFontSimilar(rl, ctFont).getFont();
    }

    /**
     * 尽可能的加载字体
     * <p>
     * 如果字体无法加载时使用相近字体替换
     *
     * @param rl     资源加载器，用于从虚拟容器中取出文件
     * @param ctFont 字体对象
     * @return 字体 或 null
     */
    public FontWrapper<PdfFont> loadPDFFontSimilar(ResourceLocator rl, CT_Font ctFont) {
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
            boolean hasReplace = false;
            // 尝试加载内嵌字体
            if (fontFileLoc != null) {
                String fontAbsPath = rl.getFile(fontFileLoc).toAbsolutePath().toString();
                fontProgram = getFontProgram(fontAbsPath);
            }
            // 尝试根据名字从操作系统加载字体
            if (fontProgram == null) {
                // 首先尝试从操作系统加兹安
                String fontAbsPath = getSystemFontPath(familyName, fontName);
                if (fontAbsPath == null && enableSimilarFontReplace) {
                    // 操作系统中不存在，那么尝试使用近似的字体替换
                    hasReplace = true;
                    fontAbsPath = getReplaceSimilarFontPath(familyName, fontName);
                }
                fontProgram = getFontProgram(fontAbsPath);
            }
            // 前面两种加载机制都失效时，使用默认字体
            if (fontProgram == null) {
                log.info("无法内嵌加载字体 {} {} {}", familyName, fontName, ctFont.getFontFile());
                fontProgram = iTextDefaultFont;
                hasReplace = true;
            }
            return new FontWrapper<>(PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H, false), hasReplace);
        } catch (Exception e) {
            log.info("加载字体异常 {} {} {}，原因:{}", familyName, fontName, ctFont.getFontFile(), e.getMessage());
            return new FontWrapper<>(PdfFontFactory.createFont(iTextDefaultFont, PdfEncodings.IDENTITY_H, false), true);
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
        final String fileName = fontAbsPath.toLowerCase();
        // 统一读取到内存防止因为 FontProgram 解析异常关闭导致无法删除临时OFD文件的问题。
        byte[] fontRaw = new byte[0];
        try {
            fontRaw = Files.readAllBytes(Paths.get(fontAbsPath));

            if (fileName.endsWith(".ttc")) {
                fontProgram = FontProgramFactory.createFont(fontRaw, 0, false);
            } else if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                fontProgram = new com.itextpdf.io.font.TrueTypeFont(fontRaw);
            } else {
                fontProgram = FontProgramFactory.createFont(fontRaw);
            }
            return fontProgram;
        } catch (Exception e) {
            log.info("字体加载失败 {} , {}", fontAbsPath, e.getMessage());
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
     * 扫描目录下所有字体并加载
     *
     * @param dir 目录
     */
    public void scanFontDir(Path dir) {
        scanFontDir(dir.toFile());
    }

    /**
     * 扫描目录下所有字体并加兹安
     *
     * @param dir 目录
     */
    public void scanFontDir(File dir) {
        if (dir == null || !dir.exists() || !dir.isDirectory())
            return;
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scanFontDir(file);
            } else {
                loadFont(file);
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
        String fileName = file.getName();
        int offset = fileName.lastIndexOf('.');
        String suffix = offset == -1 ? ".ttf" : fileName.substring(offset).toLowerCase();
        try (TTFDataStream raf = new MemoryTTFDataStream(new FileInputStream(file))) {
            switch (suffix) {
                case ".otf":
                case ".ttf": {
                    TrueTypeFont font = new TrueTypeFont().parse(raf);
                    addNormalFont(font, file.getPath());

                    font = null;
                    break;
                }
                case ".ttc": {
                    TrueTypeCollection trueTypeCollection = new TrueTypeCollection().parse(raf);
                    trueTypeCollection.foreach(font -> {
                        addNormalFont(font, file.getPath());
                        font = null;
                    });
                    break;
                }
                default:
                    break;
            }
        } catch (Exception e) {
            log.debug("{} 无法解析，忽略错误 {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    private void addNormalFont(TrueTypeFont font, String path) {


        if (!fontNamePathMapping.containsKey(font.fontFamily)) {
            addSystemFontMapping(font.fontFamily, path);
        } else {
            // 只想映射中加入常规字体，特殊变形字体忽略如加粗、斜体等
            if (font.fontSubFamily == null
                    || font.fontSubFamily.length() == 0
                    || font.fontSubFamily.equalsIgnoreCase("Regular")) {
                addSystemFontMapping(font.fontFamily, path);
            }
        }
        addSystemFontMapping(font.psName, path);
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
}
