package org.ofdrw.font;

import java.awt.FontFormatException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 自定义字体类，解决文件句柄占用无法删除的问题
 */
public class FontExt extends Font {

    // 1. 在子类中“隐藏”这两个无法直接操作的父类私有变量
    private Path customFontFile;
    private java.awt.Font customFontObj;

    // 2. 补齐必要的构造方法
    public FontExt(String name, String familyName, Path fontFile) {
        // super(...) 内部会触发被重写的 setFontFile()
        super(name, familyName, fontFile);
    }

    public FontExt(String name, Path fontFile) {
        super(name, fontFile);
    }

    public FontExt(String name, String familyName, Path fontFile, double[] printableAsciiWidthMap) {
        super(name, familyName, fontFile, printableAsciiWidthMap);
    }

    /**
     * 3. 重写设置文件的方法，使用流加载以释放句柄
     */
    @Override
    public Font setFontFile(Path fontFile) {
        // 注意：千万不要调用 super.setFontFile(fontFile); 否则又会占用文件句柄
        this.customFontFile = fontFile;
        try (InputStream is = Files.newInputStream(fontFile)) {
            this.customFontObj = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, is);
        } catch (FontFormatException | IOException e) {
            throw new IllegalArgumentException("字体文件(fontFile)格式错误", e);
        }
        return this;
    }

    /**
     * 4. 重写对应的 Getter，返回子类持有的变量
     */
    @Override
    public Path getFontFile() {
        return this.customFontFile;
    }

    @Override
    public java.awt.Font getFontObj() {
        return this.customFontObj;
    }

    @Override
    public String getFontFileName() {
        if (this.customFontFile != null && this.customFontFile.getFileName() != null) {
            return this.customFontFile.getFileName().toString();
        }
        return null;
    }
}