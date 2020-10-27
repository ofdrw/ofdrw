package org.ofdrw.core.action.actionType.actionGoto;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.STBase;
import org.ofdrw.core.basicType.ST_RefID;


/**
 * 目标区域
 * <p>
 * 图 75 目标区域结构
 *
 * @author 权观宇
 * @since 2019-10-05 08:38:08
 */
public class CT_Dest extends OFDElement implements OFDGotoTarget {
    public CT_Dest(Element proxy) {
        super(proxy);
    }

    public CT_Dest() {
        super("Dest");
    }


    /**
     * 【必选 属性】
     * 设置 目标区域的描述方法
     *
     * @param type 目标区域的描述方法
     * @return this
     */
    public CT_Dest setType(DestType type) {
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 目标区域的描述方法
     *
     * @return 目标区域的描述方法
     */
    public DestType getType() {
        return DestType.getInstance(this.attributeValue("Type"));
    }

    /**
     * 【必选】
     * 设置 引用跳转目标页面的标识
     *
     * @param pageId 引用跳转目标页面的标识
     * @return this
     */
    public CT_Dest setPageID(ST_RefID pageId) {
        this.addAttribute("PageID", pageId.toString());
        return this;
    }

    /**
     * 【必选】
     * 获取 引用跳转目标页面的标识
     *
     * @return 引用跳转目标页面的标识
     */
    public ST_RefID getPageID() {
        return ST_RefID.getInstance(this.attributeValue("PageID"));
    }

    /**
     * 【可选】
     * 设置 目标区域左上角 x坐标
     * <p>
     * 默认值为 0
     *
     * @param left 目标区域左上角 x坐标
     * @return this
     */
    public CT_Dest setLeft(double left) {
        this.setOFDEntity("Left", STBase.fmt(left));
        return this;
    }

    /**
     * 【可选】
     * 获取 目标区域左上角 x坐标
     * <p>
     * 默认值为 0
     *
     * @return 目标区域左上角 x坐标
     */
    public Double getLeft() {
        String str = this.getOFDElementText("Left");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }


    /**
     * 【可选】
     * 设置 目标区域右上角 x坐标
     * <p>
     * 默认值为 0
     *
     * @param right 目标区域右上角 x坐标
     * @return this
     */
    public CT_Dest setRight(double right) {
        this.setOFDEntity("Right", STBase.fmt(right));
        return this;
    }

    /**
     * 【可选】
     * 获取 目标区域右上角 x坐标
     * <p>
     * 默认值为 0
     *
     * @return 目标区域右上角 x坐标
     */
    public Double getRight() {
        String str = this.getOFDElementText("Right");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }


    /**
     * 【可选】
     * 设置 目标区域左上角 y坐标
     * <p>
     * 默认值为 0
     *
     * @param top 目标区域左上角 y坐标
     * @return this
     */
    public CT_Dest setTop(double top) {
        this.setOFDEntity("Top", STBase.fmt(top));
        return this;
    }

    /**
     * 【可选】
     * 获取 目标区域左上角 x坐标
     * <p>
     * 默认值为 0
     *
     * @return 目标区域左上角 x坐标
     */
    public Double getTop() {
        String str = this.getOFDElementText("Top");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }

    /**
     * 【可选】
     * 设置 目标区域右下角 y坐标
     * <p>
     * 默认值为 0
     *
     * @param bottom 目标区域右下角 y坐标
     * @return this
     */
    public CT_Dest setBottom(double bottom) {
        this.setOFDEntity("Bottom", STBase.fmt(bottom));
        return this;
    }

    /**
     * 【可选】
     * 获取 目标区域右下角 y坐标
     * <p>
     * 默认值为 0
     *
     * @return 目标区域右下角 y坐标
     */
    public Double getBottom() {
        String str = this.getOFDElementText("Bottom");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }


    /**
     * 【可选】
     * 设置 目标区域页面缩放比例
     * <p>
     * 为 0 或不出现则按照但前缩放比例跳转，可取值范围[0.1 64.0]
     *
     * @param zoom 目标区域页面缩放比例
     * @return this
     */
    public CT_Dest setZoom(double zoom) {
        this.setOFDEntity("Zoom", STBase.fmt(zoom));
        return this;
    }

    /**
     * 【可选】
     * 获取 目标区域页面缩放比例
     * <p>
     * 为 0 或不出现则按照但前缩放比例跳转，可取值范围[0.1 64.0]
     *
     * @return 目标区域页面缩放比例
     */
    public Double getZoom() {
        String str = this.getOFDElementText("Zoom");
        if (str == null || str.trim().length() == 0) {
            return 0D;
        }
        return Double.parseDouble(str);
    }
}
