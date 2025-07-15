package org.ofdrw.layout.edit;

import org.ofdrw.core.basicStructure.pageObj.layer.block.TextObject;
import org.ofdrw.core.text.CT_CGTransform;
import org.ofdrw.core.text.font.CT_Font;
import org.ofdrw.font.Font;

/**
 * 字体变换类型的替换处理器
 *
 * @author 权观宇
 * @since 2025-7-15 10:37:47
 */
public interface ReplaceTextHandler {

    /**
     * 扩展预留，为对应的文字构造CgTransform
     *
     * @param textObject   TextObject对象
     * @param newText      替换后的文字
     * @param beforeCtFont 原文字内容的字体文件
     * @return 替换后的字体形变对象
     */
    public CT_CGTransform handleCgTransform(TextObject textObject, String newText, CT_Font beforeCtFont) ;

    /**
     * 创建新的字体
     *
     * @param textObject   TextObject对象
     * @param newText      替换后的文字
     * @param beforeCtFont 原文字内容的字体文件
     * @return 替换文字使用的字体对象
     */

    public Font handleNewFont(TextObject textObject, String newText, CT_Font beforeCtFont);
}
