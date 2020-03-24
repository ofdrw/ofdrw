package org.ofdrw.core.text.font;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 字形
 * <p>
 * 11.1 字形 图 58 表 44
 *
 * @author 权观宇
 * @since 2019-10-18 08:26:16
 */
public class CT_Font extends OFDElement {
    public CT_Font(Element proxy) {
        super(proxy);
    }

    public CT_Font() {
        super("Font");
    }

    /**
     * @param fontName 字形名
     */
    public CT_Font(String fontName) {
        this();
        this.setFontName(fontName);
    }

    public ST_ID getID() {
        return this.getObjID();
    }

    public CT_Font setID(ST_ID id) {
        this.setObjID(id);
        return this;
    }

    public CT_Font setID(long id) {
        this.setObjID(new ST_ID(id));
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 字形名
     *
     * @param fontName 字形名
     * @return this
     */
    public CT_Font setFontName(String fontName) {
        if (fontName == null) {
            throw new IllegalArgumentException("字形名（fontName）不能为空");
        }
        this.addAttribute("FontName", fontName.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 字形名
     *
     * @return 字形名
     */
    public String getFontName() {
        String str = this.attributeValue("FontName");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("字形名（fontName）不能为空");
        }
        return str;
    }

    /**
     * 【可选 属性】
     * 设置 字形族名
     * <p>
     * 用于匹配代替字形
     *
     * @param familyName 字形族名
     * @return this
     */
    public CT_Font setFamilyName(String familyName) {
        if (familyName == null || familyName.trim().length() == 0) {
            // 不设置（可选参数）
            return this;
        }
        this.addAttribute("FamilyName", familyName);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 字形族名
     * <p>
     * 用于匹配代替字形
     *
     * @return 字形族名
     */
    public String getFamilyName() {
        return this.attributeValue("FamilyName");
    }

    /**
     * 【可选 属性】
     * 设置 字形适用的字符分类
     * <p>
     * 可选值参考{@link Charset}
     *
     * @param charset 字形适用的字符分类
     * @return this
     */
    public CT_Font setCharset(Charset charset) {
        if (charset == null) {
            charset = Charset.unicode;
        }
        this.addAttribute("Charset", charset.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 字形适用的字符分类
     * 可选值参考{@link Charset}
     *
     * @return 字形适用的字符分类
     */
    public Charset getCharset() {
        return Charset.getInstance(this.attributeValue("Charset"));
    }

    /**
     * 【可选 属性】
     * 设置 是否是斜体
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @param italic true - 斜体； false - 正常
     * @return this
     */
    public CT_Font setItalic(Boolean italic) {
        if (italic == null) {
            italic = false;
        }
        this.addAttribute("Italic", italic.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否是斜体
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @return true - 斜体； false - 正常
     */
    public Boolean getItalic() {
        String str = this.attributeValue("Italic");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 是否是粗字体
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @param bold true - 粗体； false - 正常
     * @return this
     */
    public CT_Font setBold(Boolean bold) {
        if (bold == null) {
            bold = false;
        }
        this.addAttribute("Bold", bold.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否是粗字体
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @return true - 粗体； false - 正常
     */
    public Boolean getBold() {
        String str = this.attributeValue("Bold");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 是否是带衬线字形
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @param serif true - 带衬线；false - 正常
     * @return this
     */
    public CT_Font setSerif(Boolean serif) {
        if (serif == null) {
            serif = false;
        }
        this.addAttribute("Serif", serif.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 是否是带衬线字形
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @return true - 带衬线；false - 正常
     */
    public Boolean getSerif() {
        String str = this.attributeValue("Serif");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选 属性】
     * 设置 是否是等宽字形
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @param fixedWidth true - 等宽字形；false - 正常
     * @return this
     */
    public CT_Font setFixedWidth(Boolean fixedWidth) {
        if (fixedWidth == null) {
            fixedWidth = false;
        }
        this.addAttribute("FixedWidth", fixedWidth.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 是否是等宽字形
     * <p>
     * 用于匹配替代字形
     * <p>
     * 默认值是 false
     *
     * @return true - 等宽字形；false - 正常
     */
    public Boolean getFixedWidth() {
        String str = this.attributeValue("FixedWidth");
        if (str == null || str.trim().length() == 0) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * 【可选】
     * 设置 指向内嵌字形文件
     * <p>
     * 嵌入字形文件应使用 OpenType 格式
     *
     * @param fontFile 指向内嵌字形文件路径
     * @return this
     */
    public CT_Font setFontFile(ST_Loc fontFile) {
        this.setOFDEntity("FontFile", fontFile.toString());
        return this;
    }

    public CT_Font setFontFile(String path) {
        return setFontFile(new ST_Loc(path));
    }

    /**
     * 【可选】
     * 获取 指向内嵌字形文件
     * <p>
     * 嵌入字形文件应使用 OpenType 格式
     *
     * @return 指向内嵌字形文件路径
     */
    public ST_Loc getFontFile() {
        return ST_Loc.getInstance(this.getOFDElementText("FontFile"));
    }
}
