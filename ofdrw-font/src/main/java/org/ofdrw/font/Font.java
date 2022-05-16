package org.ofdrw.font;

import java.nio.file.Files;
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
     * 字体文件路径
     */
    private Path fontFile;

    /**
     * 可打印字符宽度映射
     */
    private double[] printableAsciiWidthMap = null;

    private Font() {
    }

    /**
     * @return 默认字体（Noto思源宋体）
     */
    public static Font getDefault() {
        return FontName.SimSun.font();
    }

    public Font(String name, String familyName, Path fontFile) {
        this.name = name;
        this.familyName = familyName;
        if (fontFile == null || Files.notExists(fontFile)){
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }

        this.fontFile = fontFile;
    }

    public Font(String name, String familyName) {
        this.name = name;
        this.familyName = familyName;
    }

    public Font(String name, Path fontFile) {
        this.name = name;
        if (fontFile == null || Files.notExists(fontFile)){
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }
        this.fontFile = fontFile;

    }

    public Font(String name, String familyName, Path fontFile, double[] printableAsciiWidthMap) {
        this.name = name;
        this.familyName = familyName;
        this.fontFile = fontFile;
        this.printableAsciiWidthMap = printableAsciiWidthMap;
    }

    /**
     * 获取字符占比
     *
     * @param txt 字符
     * @return 0~1 占比
     */
    public double getCharWidthScale(char txt) {
        // 如果存在字符映射那么从字符映射中获取宽度占比
        if (printableAsciiWidthMap != null) {
            // 所有 ASCII码均采用半角
            if (txt >= 32 && txt <= 126) {
                // 根据可打印宽度比例映射表打印
                return printableAsciiWidthMap[txt - 32];
            } else {
                // 非英文字符
                return 1;
            }
        } else {
            // 不存在字符映射，那么认为是等宽度比例 ASCII 为 0.5 其他为 1
            return (txt >= 32 && txt <= 126) ? 0.5 : 1;
        }
    }

    /**
     * 设置可打印字符宽度映射表
     *
     * 在使用操作系统字体时，默认采用ACSII 0.5 其余1的比例计算宽度，因此可能需要手动设置宽度比例才可以达到相应的效果
     *
     * @param map 映射比例表
     * @return this
     */
    public Font setPrintableAsciiWidthMap(double[] map) {
        this.printableAsciiWidthMap = map;
        return this;
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
