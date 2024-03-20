package com.itextpdf.io.font;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Itext字体工具类，支持裁剪字体
 * 
 * @author spenggch
 *
 */
public class ItextFontUtil {

    private static Constructor<TrueTypeFont> TrueTypeFontConstructor;
    static {
        TrueTypeFontConstructor = getConstructor();
    }

    private ItextFontUtil() {}

    /**
     * 加载字体，加载完成后释放字体文件
     * 
     * @param fontFile 字体文件路径
     * @return
     * @throws IOException
     */
    public static FontProgram loadFontProgram(String fontFile) throws IOException {
        if (fontFile == null || fontFile.length() == 0) {
            throw new RuntimeException("fontFile is empty");
        }
        FontProgram fontProgram = null;
        final String fileName = fontFile.toLowerCase();
        try {
            byte[] fontRaw = Files.readAllBytes(Paths.get(fontFile));
            if (fileName.endsWith(".ttc")) {
                fontProgram = FontProgramFactory.createFont(fontRaw, 0, false);
            } else if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                fontProgram = loadFont(fontRaw);
            } else {
                fontProgram = FontProgramFactory.createFont(fontRaw);
            }
            return fontProgram;
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * 加载字体
     * 
     * @param fontName 字体文件名称或全路径
     * @param fontRaw 字体文件内容
     * @return
     * @throws IOException
     */
    public static FontProgram loadFontProgram(String fontName, byte[] fontRaw) throws IOException {
        if (fontRaw != null && fontRaw.length > 0) {
            FontProgram fontProgram = null;
            final String fileName = fontName.toLowerCase();
            try {
                if (fileName.endsWith(".ttc")) {
                    fontProgram = FontProgramFactory.createFont(fontRaw, 0, false);
                } else if (fileName.endsWith(".ttf") || fileName.endsWith(".otf")) {
                    fontProgram = loadFont(fontRaw);
                } else {
                    fontProgram = FontProgramFactory.createFont(fontRaw);
                }
                return fontProgram;
            } catch (IOException e) {
                throw e;
            } catch (Exception e) {
                throw new IOException(e);
            }
        } else {
            return loadFontProgram(fontName);
        }
    }

    /**
     * 加载字体
     * 
     * @param fontRaw 字体文件内容
     * @return
     * @throws IOException
     */
    public static TrueTypeFont loadFont(byte[] fontRaw) throws Exception {
        try {
            return new TrueTypeFont(fontRaw);
        } catch (Exception e1) {
            try {
                return loadFont(new ItextOpenTypeParser(fontRaw));
            } catch (Exception e2) {
                throw e2;
            }
        }
    }

    private static TrueTypeFont loadFont(OpenTypeParser fontParser) throws Exception {
        return TrueTypeFontConstructor.newInstance(fontParser);
    }

    @SuppressWarnings("unchecked")
    private static Constructor<TrueTypeFont> getConstructor() {
        Constructor<?>[] constructors = TrueTypeFont.class.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() != 1) {
                continue;
            }
            if (constructor.getParameterTypes()[0].isAssignableFrom(OpenTypeParser.class)) {
                try {
                    if (!constructor.isAccessible()) {
                        constructor.setAccessible(true);
                    }
                    return (Constructor<TrueTypeFont>)constructor;
                } catch (Exception e) {
                }
            }
        }
        return null;
    }

}
