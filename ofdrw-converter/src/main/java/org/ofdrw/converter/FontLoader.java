package org.ofdrw.converter;


import org.apache.fontbox.ttf.*;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.ofdrw.converter.utils.OSinfo;
import org.ofdrw.converter.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字体加载器
 *
 * @author qaqtutu
 * @since 2021-04-13 19:00:39
 */
public class FontLoader {
    private static final Logger log = LoggerFactory.getLogger(FontLoader.class);

    private final Map<String, String> pathMapping = new ConcurrentHashMap<>();
    private final Map<String, String> nameMapping = new ConcurrentHashMap<>();
    private final Map<String, String> aliasMapping = new ConcurrentHashMap<>();

    public static final String Separator = "$$$$";
    private static final String DEFAULT_FONT_DIR_MAC = "/System/Library/Fonts";
    private static final String DEFAULT_FONT_DIR_WINDOWS = "C:/Windows/Fonts";
    private static final String DEFAULT_FONT_DIR_LINUX = "/usr/share/fonts";
    private static TrueTypeFont defaultFont;

    private static FontLoader instance = null;

    private FontLoader() {
    }

    private static synchronized void syncInit() {
        if (instance == null) {
            instance = new FontLoader();
            // 初始化加载字体信息
            instance.init();
        }
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
        try (InputStream in = FontLoader.class.getResourceAsStream("/fonts/simhei.ttf")) {
            OTFParser parser = new OTFParser(false);
            defaultFont = parser.parse(in);
        } catch (IOException e) {
            e.printStackTrace();
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
    }

    /**
     * 追加字体映射
     * @param familyName 字族名
     * @param fontName 字体名
     * @param aliasFamilyName 字族别名
     * @param aliasFontName 字体别名
     * @return this
     */
    public FontLoader addAliasMapping(String familyName, String fontName, String aliasFamilyName, String aliasFontName) {
        String key1 = familyName + Separator + fontName;
        String key2 = aliasFamilyName + Separator + aliasFontName;
        if (nameMapping.get(key2) == null || pathMapping.get(key2) == null) {
            log.error("要设置别名的字体不存在");
            return this;
        }
        aliasMapping.put(key1, key2);
        return this;
    }

    public void addSystemFontMapping(String familyName, String fontName, String fontFilePath) {
        if (StringUtils.isBlank(familyName) || StringUtils.isBlank(fontName) || StringUtils.isBlank(fontFilePath)) {
            log.error(String.format("添加系统字体映射失败，FamilyName:%s FontName:%s 路径：%s", familyName, fontName, fontFilePath));
        }
        File file = new File(fontFilePath);
        if (!file.exists()) {
            log.error(String.format("添加系统字体映射失败，字体文件：%s 不存在", fontFilePath));
        }
        if (file.isDirectory()) {
            log.error(String.format("添加系统字体映射失败，%s 不是一个文件", fontFilePath));
        }
        if (!file.getName().toLowerCase().endsWith("otf") && !file.getName().toLowerCase().endsWith("ttf") && !file.getName().toLowerCase().endsWith("ttc")) {
            log.error(String.format("添加系统字体映射失败，%s 不是一个OpenType字体文件", fontFilePath));
        }
        synchronized (pathMapping) {
            pathMapping.put(familyName + Separator + fontName, fontFilePath);
        }
    }

    public String getSystemFontPath(String familyName, String fontName) {
        if (familyName == null) {
            familyName = "null";
        }
        String key = familyName + Separator + fontName;

        if (aliasMapping.get(key) != null) {
            key = aliasMapping.get(key);
        }

        if (nameMapping.get(key) != null) {
            fontName = nameMapping.get(key);
        }
        String fontFilePath = pathMapping.get(key);
        return fontFilePath;
    }

    /**
     * 尝试从系统字体目录中加载字体
     *
     * @param familyName 字族名
     * @param fontName   字体名
     * @return TTF或null
     */
    public TrueTypeFont loadSystemFont(String familyName, String fontName) {
        if (familyName == null) {
            familyName = "null";
        }
        String key = familyName + Separator + fontName;

        if (aliasMapping.get(key) != null) {
            key = aliasMapping.get(key);
        }

        if (nameMapping.get(key) != null) {
            fontName = nameMapping.get(key);
        }
        String fontFilePath = pathMapping.get(key);
        if (fontFilePath == null) {
            log.error("加载系统字体失败：" + familyName + "," + fontName);
            return loadDefaultFont();
        }
        File file = new File(fontFilePath);
        try {
            if (fontFilePath.endsWith("ttc")) {
                TrueTypeCollection trueTypeCollection = new TrueTypeCollection(file);
                TrueTypeFont trueTypeFont = trueTypeCollection.getFontByName(fontName);
                return trueTypeFont;
            } else {
                OTFParser parser = new OTFParser(false);
                OpenTypeFont openTypeFont = parser.parse(file);
                return openTypeFont;
            }
        } catch (IOException e) {
            log.warn("字体全名加载异常，尝试仅记载字体名" + e);
            return loadSystemFont(null, fontName);
        }

    }

    /**
     * 尝试从操作系统中读取PDF文字
     *
     * @param document   PDF文档对象
     * @param familyName 字体族名
     * @param fontName   字体名称
     * @return PDF文字
     * @throws IOException 字体文件读写异常
     */
    public PDFont loadSysPDFont(PDDocument document, String familyName, String fontName) throws IOException {
        final TrueTypeFont typeFont = loadSystemFont(familyName, fontName);
        return PDType0Font.load(document, typeFont, true);
    }

    /**
     * 加载外部字体
     *
     * @param absPath 绝对路径
     * @return null或外部字体
     */
    public TrueTypeFont loadExternalFont(String absPath) {
        log.debug("加载内嵌字体：" + absPath);
        if (absPath.toUpperCase().endsWith(".TTF")) {
            try (InputStream in = new FileInputStream(absPath)) {
                return new TTFParser(true).parse(in);
            } catch (IOException e) {
                log.error("加载TTF字体出错：" + e.getMessage(),e);
            }
        } else if (absPath.toUpperCase().endsWith(".OTF")) {
            try (InputStream in = new FileInputStream(absPath)) {
                OTFParser parser = new OTFParser(true);
                return parser.parse(in);
            } catch (IOException e) {
                log.error("加载OTF字体出错：" + e.getMessage(),e);
            }
        } else {
            log.warn("不支持的字体格式：" + absPath);
        }
        return null;
    }


    public TrueTypeFont loadDefaultFont() {
        return defaultFont;
    }

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

    public void loadFont(File file) {
        if (file.getName().toLowerCase().endsWith("ttc")) {
            try {
                TrueTypeCollection trueTypeCollection = new TrueTypeCollection(file);
                trueTypeCollection.processAllFonts(trueTypeFont -> {
                    NamingTable namingTable = trueTypeFont.getNaming();
                    addSystemFontMapping(namingTable, file.getPath());
                });
            } catch (IOException e) {
                log.warn("加载字体失败：" + file.getAbsolutePath(), e);
            }
        }
        if (!file.getName().toLowerCase().endsWith("otf") && !file.getName().toLowerCase().endsWith("ttf")) {
            return;
        }
        try {
            OTFParser parser = new OTFParser(true);
            OpenTypeFont openTypeFont = parser.parse(file);
            NamingTable namingTable = openTypeFont.getNaming();
            addSystemFontMapping(namingTable, file.getPath());
        } catch (Exception e) {
            e.printStackTrace();
            log.warn("加载字体失败：" + file.getAbsolutePath(), e);
        }
    }

    private void addSystemFontMapping(NamingTable namingTable, String path) {
//        String family = null;
        String name = null;
//        String cnFamily = null;
//        String cnName = null;
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
        familyNames.forEach(familyName -> {
            fontNames.forEach(fontName -> {
                nameMapping.put(familyName + Separator + fontName, finalName);
                nameMapping.put("null$$$$" + fontName, finalName);
//                log.debug(String.format("注册字体 %s,%s,%s", familyName, fontName, path));
                addSystemFontMapping(familyName, fontName, path);
                addSystemFontMapping("null", fontName, path);
            });
        });
//        System.out.println(String.format("%s %s %s %s", family, name, cnFamily, cnName));
    }


}
