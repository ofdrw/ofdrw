package org.ofdrw.converter.utils;

import org.apache.fontbox.ttf.*;
import org.ofdrw.converter.FontLoader;

import java.io.File;


/**
 * @deprecated {@link org.ofdrw.converter.FontLoader}
 */
@Deprecated
public class FontUtils {

    public static void init() {
        FontLoader.getInstance();
    }

    public static void addAliasMapping(String familyName, String fontName, String aliasFamilyName, String aliasFontName) {
        FontLoader.getInstance().addAliasMapping(familyName, fontName, aliasFamilyName, aliasFontName);
    }

    public static void addSystemFontMapping(String familyName, String fontName, String fontFilePath) {
        FontLoader.getInstance().addSystemFontMapping(familyName, fontName, fontFilePath);
    }

    public static String getSystemFontPath(String familyName, String fontName) {
        return FontLoader.getInstance().getSystemFontPath(familyName, fontName);
    }

//    public static TrueTypeFont loadSystemFont(String familyName, String fontName) {
//        return FontLoader.getInstance().loadSystemFont(familyName, fontName);
//    }

    public static void scanFontDir(File dir) {
        FontLoader.getInstance().scanFontDir(dir);
    }

    public static void loadFont(File file) {
        FontLoader.getInstance().loadFont(file);
    }

}
