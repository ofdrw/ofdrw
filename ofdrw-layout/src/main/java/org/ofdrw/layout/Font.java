package org.ofdrw.layout;

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
     * @return 默认字体（宋体）
     */
    public static Font getDefault() {
        return new Font()
                // 默认为宋体
                .setName("SimSun");
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

    public Font setFontFile(Path fontFile) {
        this.fontFile = fontFile;
        return this;
    }
}
