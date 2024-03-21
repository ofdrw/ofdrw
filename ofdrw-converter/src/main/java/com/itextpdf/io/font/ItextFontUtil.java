package com.itextpdf.io.font;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Itext字体工具类，支持裁剪字体
 * 
 * @author spenggch
 *
 */
public class ItextFontUtil {

    private ItextFontUtil() {}

    /**
     * 加载字体，加载完成后释放字体文件
     * 
     * @param fontFile 字体文件路径
     * @return 字体信息
     * @throws IOException 解析失败
     */
    public static FontProgram loadFontProgram(String fontFile) throws IOException {
        if (fontFile == null || fontFile.isEmpty()) {
            throw new IllegalArgumentException("字体路径为空");
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
     * @param fontRaw 字体文件内容
     * @return 字体信息
     * @throws IOException 解析失败
     */
    public static TrueTypeFont loadFont(byte[] fontRaw) throws IOException {
        try {
            return new TrueTypeFont(fontRaw);
        } catch (Exception e1) {
            try {
                return new ItextTrueTypeFont(fontRaw);
            } catch (IOException e2) {
                throw e2;
            } catch (Exception e2) {
                throw new IOException(e2);
            }
        }
    }

}
