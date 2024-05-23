package org.ofdrw.font;

import java.awt.*;
import java.io.IOException;
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
    /**
     * 缓存的AWT字体
     */
    private java.awt.Font fontObj;
    
    /**
     *  是否可嵌入OFD文件包
     */
    private boolean embeddable = true;

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
        if (fontFile == null || Files.notExists(fontFile)) {
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }
        this.setFontFile(fontFile);
    }

    public Font(String name, String familyName) {
        this.name = name;
        this.familyName = familyName;
    }

    public Font(String name, Path fontFile) {
        this.name = name;
        if (fontFile == null || Files.notExists(fontFile)) {
            throw new IllegalArgumentException("字体文件(fontFile)不存在");
        }
        this.setFontFile(fontFile);

    }

    /**
     * 创建字体并指定 可打印字符宽度缩放倍数
     *
     * @param name                   字形名
     * @param familyName             字族名
     * @param fontFile               字形文件路径
     * @param printableAsciiWidthMap 可打印字符映射表，用于处理字符的宽度
     */
    public Font(String name, String familyName, Path fontFile, double[] printableAsciiWidthMap) {
        this.name = name;
        this.familyName = familyName;
        this.fontFile = fontFile;
        this.printableAsciiWidthMap = printableAsciiWidthMap;
    }

    /**
     * 字体是否存在预设的字符宽度映射表
     *
     * @return true - 存在；false - 不存在
     */
    public boolean hasWidthMath() {
        return this.printableAsciiWidthMap != null && this.printableAsciiWidthMap.length > 0;
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
     * <p>
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

    /**
     * 设置字体文件
     *
     * @param fontFile 字体文件
     * @return this
     */
    public Font setFontFile(Path fontFile) {
        this.fontFile = fontFile;
        try {
            this.fontObj = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, fontFile.toFile());
        } catch (FontFormatException | IOException e) {
            throw new IllegalArgumentException("字体文件(fontFile)格式错误");
        }
        return this;
    }

    /**
     * 获取AWT字体对象，该对象可能为空（当前没有提供字体路径时为空）
     *
     * @return 字体对象 或  null
     */
    public java.awt.Font getFontObj(){
        return this.fontObj;
    }
    
    /**
     * 获取字体嵌入标识
     * @return true-代表字体文件会被嵌入到OFD文件中，false-表示不嵌入
     */
    public boolean isEmbeddable() {
        return embeddable;
    }
    
    /**
     * 是否可嵌入OFD文件包。 如果设置为false,则字体文件仅在生成OFD文档的过程中被引用，不会包包含到OFD文件内。
     * 默认值为true。
     * @param embeddable true-嵌入，false-不嵌入
     */
    public void setEmbeddable(boolean embeddable) {
        this.embeddable = embeddable;
    }
}
