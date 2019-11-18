package org.ofdrw.core.basicStructure.res.resources;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.res.OFDResource;
import org.ofdrw.core.text.font.CT_Font;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 包含了文档所有字形的描述
 * <p>
 * 7.9 图 20 表 18
 *
 * @author 权观宇
 * @since 2019-11-13 19:41:17
 */
public class Fonts extends OFDElement implements OFDResource {
    public Fonts(Element proxy) {
        super(proxy);
    }

    public Fonts() {
        super("Fonts");
    }


    /**
     * 【必选】
     * 添加 字形描述
     * <p>
     * 必须要有ID属性
     *
     * @param font 字形描述
     * @return this
     */
    public Fonts addFont(CT_Font font) {
        if (font == null) {
            return this;
        }
        if (font.getID() == null) {
            throw new IllegalArgumentException("字形描述ID不能为空");
        }
        this.add(font);
        return this;
    }

    /**
     * 【必选】
     * 获取 字形描述序列
     * <p>
     * 必须要有ID属性
     *
     * @return 字形描述
     */
    public List<CT_Font> getFonts() {
        return this.getOFDElements("Font", CT_Font::new);
    }
}
