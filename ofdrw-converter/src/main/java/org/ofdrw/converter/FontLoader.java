package org.ofdrw.converter;


import com.itextpdf.io.font.FontProgram;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import org.apache.fontbox.ttf.*;
import org.ofdrw.converter.font.FontWrapper;
import org.ofdrw.converter.utils.OSinfo;
import org.ofdrw.converter.utils.StringUtils;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.reader.ResourceLocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

    private final Map<String, String> pathMapping = new ConcurrentHashMap<>();
    private final Map<String, String> nameMapping = new ConcurrentHashMap<>();
    private final Map<String, String> aliasMapping = new ConcurrentHashMap<>();
    /**
     * 使用正则匹配映射到系统或嵌入字体
     */
    private final Map<String, String> similarFontReplaceRegexMapping = new ConcurrentHashMap<>();

    public static final String Separator = "$$$$";
    public static final String SeparatorRegex = "\\$\\$\\$\\$";

    private static final String Empty = "null";
    private static final String DEFAULT_FONT_DIR_MAC = "/System/Library/Fonts";
    private static final String DEFAULT_FONT_DIR_WINDOWS = "C:/Windows/Fonts";
    private static final String DEFAULT_FONT_DIR_LINUX = "/usr/share/fonts";
    private static TrueTypeFont defaultFont;
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

    public static FontLoader getInstance() {
        if (instance == null) {
            syncInit();
        }
        return instance;
    }

    /*
     * 加载默认字体
     * 加载系统字体
     * */
    public void init() {
        try (InputStream in = FontLoader.class.getResourceAsStream(DEFAULT_FONT_RESOURCE_PATH)) {
            defaultFont = new TTFParser(true).parse(in);
        } catch (IOException ignored) {
        }
        if (OSinfo.isWindows()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_WINDOWS));
        } else if (OSinfo.isMacOS()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
        } else if (OSinfo.isMacOSX()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_MAC));
        } else if (OSinfo.isLinux()) {
            scanFontDir(new File(DEFAULT_FONT_DIR_LINUX));
        }
        // 预置一些常用的字体别名
        this.addAliasMapping(null, "小标宋体", "方正小标宋简体", "方正小标宋简体")
                .addAliasMapping(null, "KaiTi_GB2312", "楷体", "楷体")
                .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
                .addSimilarFontReplaceRegexMapping(null, ".*Kai.*", null, "楷体")
                .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun")
                .addSimilarFontReplaceRegexMapping(null, ".*SimSun.*", null, "SimSun")
                .addSimilarFontReplaceRegexMapping(null, ".*Song.*", null, "宋体")
                .addSimilarFontReplaceRegexMapping(null, ".*MinionPro.*", null, "SimSun");
    }

    /**
     * 追加字体映射
     *
     * @param familyName      字族名
     * @param fontName        字体名
     * @param aliasFamilyName 字族别名
     * @param aliasFontName   字体别名
     * @return this
     */
    public FontLoader addAliasMapping(String familyName, String fontName, String aliasFamilyName, String aliasFontName) {
        if (familyName == null) {
            familyName = Empty;
        }
        if (fontName == null) {
            fontName = Empty;
        }
        if (aliasFamilyName == null) {
            aliasFamilyName = Empty;
        }
        if (aliasFontName == null) {
            aliasFontName = Empty;
        }
        String key1 = familyName + Separator + fontName;
        String key2 = aliasFamilyName + Separator + aliasFontName;
        if (nameMapping.get(key2) == null || pathMapping.get(key2) == null) {
            log.info("字体别名 [{} {}] -> [{} {}] 不存在", familyName, fontName, aliasFamilyName, aliasFontName);
            return this;
        }
        aliasMapping.put(key1, key2);
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
     */
    public FontLoader addSimilarFontReplaceRegexMapping(String familyNameRegex, String fontNameRegex, String aliasFamilyName, String aliasFontName) {

        if (familyNameRegex == null) {
            familyNameRegex = Empty;
        }
        if (fontNameRegex == null) {
            fontNameRegex = Empty;
        }
        if (aliasFamilyName == null) {
            aliasFamilyName = Empty;
        }
        if (aliasFontName == null) {
            aliasFontName = Empty;
        }
        String key1 = familyNameRegex + Separator + fontNameRegex;
        String key2 = aliasFamilyName + Separator + aliasFontName;
        if (pathMapping.get(key2) == null) {
            log.info("字体别名 [{} {}] -> [{} {}] 不存在", familyNameRegex, fontNameRegex, aliasFamilyName, aliasFontName);
            return this;
        }
        similarFontReplaceRegexMapping.put(key1, key2);
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
     */
    public void addSystemFontMapping(String familyName, String fontName, String fontFilePath) {
        if (StringUtils.isBlank(familyName) || StringUtils.isBlank(fontName) || StringUtils.isBlank(fontFilePath)) {
            log.info("添加系统字体映射失败，FamilyName: {} FontName: {} 路径：{}", familyName, fontName, fontFilePath);
        }
        File file = new File(fontFilePath);
        if (!file.exists() || file.isDirectory()) {
            log.info("添加系统字体映射失败，字体文件：{} 不存在", fontFilePath);
        }
        if (!file.getName().toLowerCase().endsWith("otf") && !file.getName().toLowerCase().endsWith("ttf") && !file.getName().toLowerCase().endsWith("ttc")) {
            log.info("添加系统字体映射失败，{} 不是一个OpenType字体文件", fontFilePath);
        }
        synchronized (pathMapping) {
            pathMapping.put(familyName + Separator + fontName, fontFilePath);
        }
    }

    /**
     * 从操作系统字体目下获取字体路径
     *
     * @param familyName 字族名
     * @param fontName   字体名
     * @return 字体操作系统内绝对路径，如果不存在返还null
     */
    public String getSystemFontPath(String familyName, String fontName) {
        if (familyName == null) {
            familyName = Empty;
        }
        String key = familyName + Separator + fontName;

        if (pathMapping.get(key) != null) {
            return pathMapping.get(key);
        }

        if (aliasMapping.get(key) != null) {
            // return aliasMapping.get(key);
            String alias = aliasMapping.get(key);
            if (pathMapping.get(alias) != null) {
                String sysPath = pathMapping.get(alias);
                log.info("映射字体 {} {} 为 {} , {}", familyName, fontName, alias.replace(Separator, " "), sysPath);
                return sysPath;
            }
        }

        // 尝试使用单独字体名，不含字族名
        if (!Empty.equals(familyName))
            return getSystemFontPath(Empty, fontName);

        return null;
    }


    /**
     * 获取配置的 相似字体 对应的字体路径
     *
     * @param familyName 字族名
     * @param fontName   字体名
     * @return 字体操作系统内绝对路径，如果不存在返还 null
     */
    public String getReplaceSimilarFontPath(String familyName, String fontName) {

        for (String regexKey : similarFontReplaceRegexMapping.keySet()) {

            String[] regexps = regexKey.split(SeparatorRegex);
            boolean isMatchFamilyName = familyName != null && Pattern.matches(regexps[0], familyName);
            boolean isMatchFontName = fontName != null && Pattern.matches(regexps[1], fontName);
            if (isMatchFamilyName || isMatchFontName) {
                String s = similarFontReplaceRegexMapping.get(regexKey);
                String sysPath = pathMapping.get(s);
//                Pattern.compile(".*MiniPro.*").matcher("MiniPro-sad").matches()
                log.info("替换字体 {} {} 为 {} , {}", familyName, fontName, s.replace(Separator, " "), sysPath);
                return sysPath;
            }
        }

        return null;
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
            //如果直接切换为默认字体，字体映射可能会错误
//            log.info("加载系统字体失败：[{} {}], 切换至默认字体(宋体)", familyName, fontName);
//            return loadDefaultFont();
        }
        // 加载字体
        TrueTypeFont ttf = loadExternalFont(fontFilePath, familyName, fontName);
//        if (ttf == null) {
//            return loadDefaultFont();
//        }
        return ttf;
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
     * @param familyName 字族名
     * @param fontName   字体名
     * @return 字体
     */
    public TrueTypeFont loadExternalFont(String absPath, String familyName, String fontName) {
        try (InputStream in = new FileInputStream(absPath)) {
            if (absPath.toLowerCase().endsWith(".ttf")) {
                return new TTFParser(true).parse(in);
            } else if (absPath.toLowerCase().endsWith(".otf")) {
                return new OTFParser(true).parse(in);
            } else if (absPath.toLowerCase().endsWith(".ttc")) {
                TrueTypeCollection trueTypeCollection = new TrueTypeCollection(in);
                TrueTypeFont res = trueTypeCollection.getFontByName(fontName);
                if (res == null) {
                    boolean[] flag = new boolean[]{false};
                    // 使用第一个出现的字体
                    TrueTypeFont[] ttfHolder = new TrueTypeFont[]{null};
                    trueTypeCollection.processAllFonts((trueTypeFont) -> {
                        if (flag[0]) {
                            return;
                        }
                        flag[0] = true;
                        ttfHolder[0] = trueTypeFont;
                    });
                    res = ttfHolder[0];
                }
                return res;
            }
//            else if (absPath.toUpperCase().endsWith(".CFF")) {
//                CFFParser parser = new CFFParser();
//                return parser.parse(in);
//            }
            else {
                log.info("不支持的字体格式：" + absPath);
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
        try {
            ST_Loc fontFileLoc = ctFont.getFontFile();
            TrueTypeFont trueTypeFont = null;
            if (fontFileLoc != null) {
                // 通过资源加载器获取文件的绝对路径
                String fontAbsPath = rl.getFile(ctFont.getFontFile()).toAbsolutePath().toString();
                trueTypeFont = loadExternalFont(fontAbsPath, ctFont.getFamilyName(), ctFont.getFontName());
            }
            CmapLookup cmapLookup = null;
            try {
                cmapLookup = trueTypeFont.getUnicodeCmapLookup();
            } catch (Exception ignore) {
            }
            boolean hasReplace = false;
            if (trueTypeFont == null || trueTypeFont.getName() == null || cmapLookup == null) {
                trueTypeFont = loadSystemFont(ctFont.getFamilyName(), ctFont.getFontName());

                if (trueTypeFont == null && enableSimilarFontReplace) {
                    String similarFontPath = getReplaceSimilarFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                    if (similarFontPath != null) {
                        trueTypeFont = loadExternalFont(similarFontPath, null, null);
                    }
                    if (trueTypeFont != null) {
                        hasReplace = true;
                    } else {
                        trueTypeFont = defaultFont;
                        hasReplace = true;
                    }
                }
//                else{
//                    trueTypeFont = defaultFont;
//                }

            }
            return new FontWrapper<TrueTypeFont>(trueTypeFont, hasReplace);
        } catch (Exception e) {
            log.info("无法加载字体: {} {} {}" + ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile());
            return null;
        }
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
        try {
            ST_Loc fontFileLoc = ctFont.getFontFile();
            String fontAbsPath = null;
            boolean hasReplace = false;
            if (fontFileLoc != null) {
                // 通过资源加载器获取文件的绝对路径
                fontAbsPath = rl.getFile(fontFileLoc).toAbsolutePath().toString();
            } else {
                fontAbsPath = getSystemFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                if (fontAbsPath == null && enableSimilarFontReplace) {
                    fontAbsPath = getReplaceSimilarFontPath(ctFont.getFamilyName(), ctFont.getFontName());
                    if (fontAbsPath != null) {
                        hasReplace = true;
                    }
                    //可以增加内建字体文件
//                    else if(enableBuildInFont){
//                    }
                }
            }

            boolean embedded = false;
            if (fontAbsPath == null) {
                if (enableSimilarFontReplace) {
                    URL resource = FontLoader.class.getResource(DEFAULT_FONT_RESOURCE_PATH);
                    if (resource != null) {
                        embedded = true;
                        fontAbsPath = new File(resource.getFile()).getPath();
                        log.info("使用默认字体替代 {} ,{}", ctFont.getFontName(), fontAbsPath);
                    }
                } else
                    throw new FileNotFoundException("无法在OFD内找到字体 " + fontAbsPath);
            }
            FontProgram fontProgram = null;
            if (fontAbsPath == null) {
                throw new IllegalArgumentException("无法找到字体路径");
            }
            // 解决TTC无法加载问题
            final String fileName = fontAbsPath.toLowerCase();
            if (fileName.endsWith(".ttc")) {
                fontAbsPath = fontAbsPath + ",0";
                fontProgram = FontProgramFactory.createFont(fontAbsPath, false);
            } else if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                final byte[] fontBin = Files.readAllBytes(Paths.get(fontAbsPath));
                fontProgram = new com.itextpdf.io.font.TrueTypeFont(fontBin);
            } else {
                // 即便设置不缓存，任然会出现文件没有关闭的问题。
                // 因此，读取到内存防止因为 FontProgram 的缓存导致无法删除临时OFD文件的问题。
                final byte[] fontBin = Files.readAllBytes(Paths.get(fontAbsPath));
                fontProgram = FontProgramFactory.createFont(fontBin, false);
            }

            return new FontWrapper(PdfFontFactory.createFont(fontProgram, PdfEncodings.IDENTITY_H, embedded), hasReplace);
        } catch (Exception e) {
//            e.printStackTrace();
            log.info("无法加载字体 {} {} {}，原因:{}",
                    ctFont.getFamilyName(), ctFont.getFontName(), ctFont.getFontFile(),
                    e.getMessage());
            //如果是文件中的字体资源加载失败，使用系统中的字体: Type of font is not recognized.
            if (ctFont.getFontFile() != null) {
                ctFont.setFontFile("");
                return loadPDFFontSimilar(rl, ctFont);
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
     *
     * @param file 字体文件
     */
    public void loadFont(File file) {
        String fileName = file.getName().toLowerCase();
        if (fileName.endsWith("ttc")) {
            try {
                TrueTypeCollection trueTypeCollection = new TrueTypeCollection(file);
                trueTypeCollection.processAllFonts(trueTypeFont -> {
                    NamingTable namingTable = trueTypeFont.getNaming();
                    addSystemFontMapping(namingTable, file.getPath());
                });
            } catch (IOException e) {
                log.info("无法加载字体：" + file.getAbsolutePath());
            }
            return;
        }
        if (!fileName.endsWith("otf") && !fileName.endsWith("ttf")) {
            return;
        }
        try {
            OTFParser parser = new OTFParser(true);
            OpenTypeFont openTypeFont = parser.parse(file);
            NamingTable namingTable = openTypeFont.getNaming();
            addSystemFontMapping(namingTable, file.getPath());
        } catch (Exception e) {
            log.info("无法加载字体：" + file.getAbsolutePath());
        }
    }

    /**
     * 根据OpenType 中的name表加载字体映射
     *
     * @param namingTable name表
     * @param path        字体绝对路径
     */
    private void addSystemFontMapping(NamingTable namingTable, String path) {
        String name = null;
        Set<String> familyNames = new HashSet<>();
        Set<String> fontNames = new HashSet<>();
        familyNames.add(namingTable.getFontFamily());
        fontNames.add(namingTable.getPostScriptName());
        name = namingTable.getPostScriptName();
        for (NameRecord record : namingTable.getNameRecords()) {
            if (record.getNameId() == 1) {
                familyNames.add(record.getString());
            } else if (record.getNameId() == 4) {
                fontNames.add(record.getString());
            }
            if (record.getLanguageId() == 0) {
//                if (record.getNameId() == 1) {
//                    family = record.getString();
//                } else
                if (record.getNameId() == 4) {
                    name = record.getString();
                }
            }
//            if (record.getLanguageId() == 2052) {
//                if (record.getNameId() == 1) {
//                    cnFamily = record.getString();
//                } else if (record.getNameId() == 4) {
//                    cnName = record.getString();
//                }
//            }

        }
        String finalName = name;
        for (String familyName : familyNames) {
            for (String fontName : fontNames) {
                nameMapping.put(familyName + Separator + fontName, finalName);
                nameMapping.put(Empty + Separator + fontName, finalName);
//                log.debug(String.format("注册字体 %s,%s,%s", familyName, fontName, path));
                addSystemFontMapping(familyName, fontName, path);
                addSystemFontMapping(Empty, fontName, path);
            }
        }
//        System.out.println(String.format("%s %s %s %s", family, name, cnFamily, cnName));
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
