package org.ofdrw.converter.utils;


import org.apache.fontbox.ttf.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


public class FontUtils {
    private static final Logger log = LoggerFactory.getLogger(FontUtils.class);

    private static Map<String, String> pathMapping = new HashMap<>();
    private static Map<String, String> nameMapping = new HashMap<>();
    private static Map<String, String> aliasMapping = new HashMap<>();
    public static final String Separator = "$$$$";


    private static final String DEFAULT_FONT_DIR_MAC = "/System/Library/Fonts";
    private static final String DEFAULT_FONT_DIR_WINDOWS = "C:/Windows/Fonts";
    private static final String DEFAULT_FONT_DIR_LINUX = "/usr/share/fonts";
    private static TrueTypeFont defaultFont;

    /*
     * 加载默认字体
     * 加载系统字体
     * */
    public static void init() {
        try (InputStream in = FontUtils.class.getResourceAsStream("/fonts/simhei.ttf")) {
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

    public static void addAliasMapping(String familyName, String fontName, String aliasFamilyName, String aliasFontName) {
        String key1 = familyName + Separator + fontName;
        String key2 = aliasFamilyName + Separator + aliasFontName;
        if (nameMapping.get(key2) == null || pathMapping.get(key2) == null) {
            log.error("要设置别名的字体不存在");
            return;
        }
        aliasMapping.put(key1, key2);
    }

    public static void addSystemFontMapping(String familyName, String fontName, String fontFilePath) {
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

    public static String getSystemFontPath(String familyName, String fontName) {
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

    public static TrueTypeFont loadSystemFont(String familyName, String fontName) {
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
            e.printStackTrace();
        }
        return loadSystemFont(null, fontName);
    }


    private static TrueTypeFont loadDefaultFont() {
        return defaultFont;
    }

    public static void scanFontDir(File dir) {
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

    public static void loadFont(File file) {
        if (file.getName().toLowerCase().endsWith("ttc")) {
            try {
                TrueTypeCollection trueTypeCollection = new TrueTypeCollection(file);
                trueTypeCollection.processAllFonts(new TrueTypeCollection.TrueTypeFontProcessor() {
                    @Override
                    public void process(TrueTypeFont trueTypeFont) throws IOException {
                        NamingTable namingTable = trueTypeFont.getNaming();
                        addSystemFontMapping(namingTable, file.getPath());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
                log.warn("加载字体失败：" + file.getAbsolutePath());
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
            log.warn("加载字体失败：" + file.getAbsolutePath());
        }
    }

    private static void addSystemFontMapping(NamingTable namingTable, String path) {
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
