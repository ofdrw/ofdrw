package org.ofdrw.font;

import java.nio.file.Path;

/**
 * 字体
 *
 * @author 权观宇
 * @since 2020-02-03 02:04:29
 */
public class Font {
    /**
     * 字体名称
     */
    private String name;

    /**
     * 字体族名称
     */
    private String familyName;

    /**
     * 字体文件目录
     */
    private Path fontFile;

    private Font() {
    }

    /**
     * @return 默认字体（Noto思源宋体）
     */
    public static Font getDefault() {
        return FontSet.get(FontName.NotoSerif);
    }

    public Font(String name, String familyName, Path fontFile) {
        this.name = name;
        this.familyName = familyName;
        this.fontFile = fontFile;
    }

    public Font(String name, Path fontFile) {
        this.name = name;
        this.fontFile = fontFile;
    }

    /**
     * 获取字体全名
     *
     * @return 字体全名
     */
    public String getCompleteFontName() {
        if (familyName == null) {
            return name;
        } else {
            return name + "-" + familyName;
        }
    }

    public String getName() {
        return name;
    }

    public Font setName(String name) {
        this.name = name;
        return this;
    }

    public String getFamilyName() {
        return familyName;
    }

    public Font setFamilyName(String familyName) {
        this.familyName = familyName;
        return this;
    }

    public Path getFontFile() {
        return fontFile;
    }

    /**
     * 获取字体文件名称
     *
     * @return 字体文件名称
     */
    public String getFontFileName() {
        return fontFile.getFileName().toString();
    }

    public Font setFontFile(Path fontFile) {
        this.fontFile = fontFile;
        return this;
    }
}
