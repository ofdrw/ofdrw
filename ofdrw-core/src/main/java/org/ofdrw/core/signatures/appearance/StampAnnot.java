package org.ofdrw.core.signatures.appearance;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * 签名的外观
 * <p>
 * 一个数字签名可以跟一个或多个外观描述关联，也可以不关联任何外观，
 * 其关联方式如图 88所示。
 * <p>
 * 18.2.3 图 88 表 69
 *
 * @author 权观宇
 * @since 2019-11-21 18:39:47
 */
public class StampAnnot extends OFDElement {
    public StampAnnot(Element proxy) {
        super(proxy);
    }

    public StampAnnot() {
        super("StampAnnot");
    }

    /**
     * 【必选 属性】
     * 设置 签章注释的标识
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @param id 签章注释的标识
     * @return this
     */
    public StampAnnot setID(String id) {
        if (id == null || id.trim().length() == 0) {
            throw new IllegalArgumentException("签章注释的标识（ID）为空");
        }
        this.addAttribute("ID", id);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 签章注释的标识
     * <p>
     * 推荐使用“sNNN”的编码方式，NNN从1开始
     *
     * @return 签章注释的标识
     */
    public String getID() {
        String str = this.attributeValue("ID");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("签章注释的标识（ID）为空");
        }
        return str;
    }

    /**
     * 【必选 属性】
     * 设置 引用外观注释所在的页面的标识符
     *
     * @param pageRef 引用外观注释所在的页面的标识符
     * @return this
     */
    public StampAnnot setPageRef(ST_RefID pageRef) {
        if (pageRef == null) {
            throw new IllegalArgumentException("引用外观注释所在的页面的标识符（PageRef）为空");
        }
        this.addAttribute("PageRef", pageRef.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 引用外观注释所在的页面的标识符
     *
     * @return 引用外观注释所在的页面的标识符
     */
    public ST_RefID getPageRef() {
        String str = this.attributeValue("PageRef");
        if (str == null || str.trim().length() == 0) {
            throw new IllegalArgumentException("引用外观注释所在的页面的标识符（PageRef）为空");
        }
        return ST_RefID.getInstance(str);
    }

    /**
     * 【必选 属性】
     * 设置 签章注释的外观边框位置
     * <p>
     * 可用于签章注释所在页面内的定位
     *
     * @param boundary 签章注释的外观边框位置
     * @return this
     */
    public StampAnnot setBoundary(ST_Box boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("签章注释的外观边框位置（Boundary）为空");
        }
        this.addAttribute("Boundary", boundary.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 签章注释的外观边框位置
     * <p>
     * 可用于签章注释所在页面内的定位
     *
     * @return 签章注释的外观边框位置
     */
    public ST_Box getBoundary() {
        return ST_Box.getInstance(this.attributeValue("Boundary"));
    }

    /**
     * 【可选 属性】
     * 设置 签章注释的外观裁剪设置
     *
     * @param clip 签章注释的外观裁剪设置
     * @return this
     */
    public StampAnnot setClip(ST_Box clip) {
        if (clip == null) {
            this.removeAttr("Clip");
            return this;
        }
        this.addAttribute("Clip", clip.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 签章注释的外观裁剪设置
     *
     * @return 签章注释的外观裁剪设置
     */
    public ST_Box getClip() {
        return ST_Box.getInstance(this.attributeValue("Clip"));
    }
}
