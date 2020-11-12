package org.ofdrw.core.text;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Array;

/**
 * 变换描述
 * <p>
 * 当存在字形变换时，TextCode对象中使用字形变换节点（CGTransform）描述字符编码
 * 和字形索引之间的关系。
 * <p>
 * 11.4.1 变换描述 图 66 表 48
 *
 * @author 权观宇
 * @since 2019-10-19 07:01:53
 */
public class CT_CGTransform extends OFDElement {
    public CT_CGTransform(Element proxy) {
        super(proxy);
    }

    public CT_CGTransform() {
        super("CGTransform");
    }

    /**
     * 【必选 属性】
     * 设置 TextCode 中字符编码的起始位置
     * <p>
     * 从 0 开始
     *
     * @param codePosition TextCode 中字符编码的起始位置
     * @return this
     */
    public CT_CGTransform setCodePosition(Integer codePosition) {
        if (codePosition == null) {
            throw new IllegalArgumentException("字符编码的起始位置不能为空");
        }
        this.addAttribute("CodePosition", codePosition.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 TextCode 中字符编码的起始位置
     * <p>
     * 从 0 开始
     *
     * @return TextCode 中字符编码的起始位置
     */
    public Integer getCodePosition() {
        String str = this.attributeValue("CodePosition");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("字符编码的起始位置不能为空");
        }
        return Integer.parseInt(str);
    }

    /**
     * 【可选 属性】
     * 设置 变换关系中字符的数量
     * <p>
     * 该数值应大于等于 1，否则属于错误描述
     * <p>
     * 默认为 1
     *
     * @param codeCount 变换关系中字符的数量，数值应大于等于 1，否则属于错误描述
     * @return this
     */
    public CT_CGTransform setCodeCount(Integer codeCount) {
        if (codeCount == null) {
            codeCount = 1;
        }
        if (codeCount < 1) {
            throw new NumberFormatException("变换关系中字符的数值应大于等于 1");
        }
        this.addAttribute("CodeCount", codeCount.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 变换关系中字符的数量
     * <p>
     * 该数值应大于等于 1，否则属于错误描述
     * <p>
     * 默认为 1
     *
     * @return 变换关系中字符的数量，数值应大于等于 1，否则属于错误描述
     */
    public Integer getCodeCount() {
        String str = this.attributeValue("CodeCount");
        if (str == null || str.trim().length() == 0) {
            return 1;
        }
        return Integer.parseInt(str);
    }

    /**
     * 【可选 属性】
     * 设置 变换关系中字形索引的个数
     * <p>
     * 该数值应大于等于 1，否则属于错误描述
     * <p>
     * 默认为 1
     *
     * @param glyphCount 变换关系中字形索引的个数
     * @return this
     */
    public CT_CGTransform setGlyphCount(Integer glyphCount) {
        if (glyphCount == null) {
            glyphCount = 1;
        }
        if (glyphCount < 1) {
            throw new NumberFormatException("变换关系中字符的数值应大于等于 1");
        }
        this.addAttribute("GlyphCount", glyphCount.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 设置 变换关系中字形索引的个数
     * <p>
     * 该数值应大于等于 1，否则属于错误描述
     * <p>
     * 默认为 1
     *
     * @return 变换关系中字形索引的个数
     */
    public Integer getGlyphCount() {
        String str = this.attributeValue("GlyphCount");
        if (str == null || str.trim().length() == 0) {
            return 1;
        }
        return Integer.parseInt(str);
    }


    /**
     * 【可选】
     * 设置 变换后的字形索引列表
     *
     * @param glyphs 变换后的字形索引列表
     * @return this
     */
    public CT_CGTransform setGlyphs(ST_Array glyphs) {
        this.setOFDEntity("Glyphs", glyphs.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 变换后的字形索引列表
     *
     * @return 变换后的字形索引列表
     */
    public ST_Array getGlyphs() {
        return ST_Array.getInstance(this.getOFDElementText("Glyphs"));
    }
}
