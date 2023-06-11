package org.ofdrw.layout.engine;

import org.ofdrw.core.text.font.CT_Font;

import java.nio.file.Path;

/**
 * 已经存在的字体对象
 *
 * @author 权观宇
 * @since 2020-05-10 18:27:27
 */
public class ExistCTFont {

    /**
     * 在文档中的字体对象
     */
    public CT_Font font;
    /**
     * 字体文件绝对路径，若为操作系统字体可能为空
     */
    public Path absPath;

    public ExistCTFont(CT_Font font, Path absPath) {
        this.font = font;
        this.absPath = absPath;
    }
}
