package org.ofdrw.core.pageDescription.color.colorSpace;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 颜色空间
 * <p>
 * 本标准支持 GRAY、RGB、CMYK 颜色空间。除通过
 * 设置各通道使用颜色空间内的任意颜色之外，还可
 * 在颜色空间内定义调色板或指定相应颜色配置文件，
 * 通过设置索引值进行引用。
 * <p>
 * 8.3 颜色 图 24
 *
 * @author 权观宇
 * @since 2019-10-11 08:05:25
 */
public class CT_ColorSpace extends OFDElement {
    public CT_ColorSpace(Element proxy) {
        super(proxy);
    }

    public CT_ColorSpace() {
        super("ColorSpace");
    }

    /**
     * @param type 颜色类型
     */
    public CT_ColorSpace(OFDColorSpaceType type) {
        this();
        this.setType(type);
    }

    /**
     * @param type 颜色类型
     * @param id   对象ID
     */
    public CT_ColorSpace(OFDColorSpaceType type, long id) {
        this();
        this.setType(type)
                .setID(new ST_ID(id));
    }

    public ST_ID getID() {
        return this.getObjID();
    }

    public CT_ColorSpace setID(ST_ID id) {
        this.setObjID(id);
        return this;
    }

    /**
     * 【必选 属性】
     * 设置 颜色空间的类型
     * <p>
     * 可选类型{@link OFDColorSpaceType}
     *
     * @param type 颜色空间的类型
     * @return this
     */
    public CT_ColorSpace setType(OFDColorSpaceType type) {
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 颜色空间的类型
     * <p>
     * 可选类型{@link OFDColorSpaceType}
     *
     * @return 颜色空间的类型
     */
    public OFDColorSpaceType getType() {
        return OFDColorSpaceType.getInstance(this.attributeValue("Type"));
    }


    /**
     * 【可选 属性】
     * 设置 每个颜色通道使用的位数
     * <p>
     * 有效取值为：1，2，4，8，16 参考{@link BitsPerComponent}
     *
     * @param bitsPerComponent 每个颜色通道使用的位数
     * @return this
     */
    public CT_ColorSpace setBitsPerComponent(BitsPerComponent bitsPerComponent) {
        if (bitsPerComponent == null) {
            this.removeAttr("BitsPerComponent");
            return this;
        }
        this.addAttribute("BitsPerComponent", bitsPerComponent.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 每个颜色通道使用的位数
     * <p>
     * 有效取值为：1，2，4，8，16 参考{@link BitsPerComponent}
     *
     * @return 每个颜色通道使用的位数
     */
    public BitsPerComponent getBitsPerComponent() {
        return BitsPerComponent.getInstance(this.attributeValue("BitsPerComponent"));
    }

    /**
     * 【可选 属性】
     * 设置 指向包内颜色配置文件
     *
     * @param profile 指向包内颜色配置文件路径
     * @return this
     */
    public CT_ColorSpace setProfile(ST_Loc profile) {
        if (profile == null) {
            this.removeAttr("Profile");
            return this;
        }
        this.addAttribute("Profile", profile.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 指向包内颜色配置文件
     *
     * @return 指向包内颜色配置文件路径
     */
    public ST_Loc getProfile() {
        return ST_Loc.getInstance(this.attributeValue("Profile"));
    }


    /**
     * 【可选】
     * 设置 调色板
     * <p>
     * 调色板中颜色的索引编号从 0 开始
     *
     * @param palette 调色板
     * @return this
     */
    public CT_ColorSpace setPalette(Palette palette) {
        this.set(palette);
        return this;
    }

    /**
     * 【可选】
     * 获取 调色板
     * <p>
     * 调色板中颜色的索引编号从 0 开始
     *
     * @return 调色板
     */
    public Palette getPalette() {
        Element e = this.getOFDElement("Palette");
        return e == null ? null : new Palette(e);
    }
}
