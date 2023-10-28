package org.ofdrw.layout.areaholder;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_Box;
import org.ofdrw.core.basicType.ST_Loc;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * 【OFDRW 扩展】 区域占位区块
 * <p>
 * 该元素用于存储和表示文档生成者在文档中预置的绘制区域占位符。
 *
 * @author 权观宇
 * @since 2023-10-28 15:02:23
 */
public class CT_AreaHolderBlock extends OFDElement {
    public CT_AreaHolderBlock(Element proxy) {
        super(proxy);
    }

    public CT_AreaHolderBlock() {
        super("AreaHolderBlock");
    }

    /**
     * 创建 区域占位区块
     *
     * @param areaName 区域名称
     */
    public CT_AreaHolderBlock(String areaName) {
        this();
        this.setAreaName(areaName);
    }

    /**
     * 【必选 属性】
     * 设置 区域名称
     *
     * @param areaName 区域名称
     * @return this
     */
    public CT_AreaHolderBlock setAreaName(String areaName) {
        if (areaName == null) {
            throw new IllegalArgumentException("区域名称（AreaName）不能为空");
        }
        this.addAttribute("AreaName", areaName);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 区域名称
     *
     * @return 区域名称
     */
    public String getAreaName() {
        return this.attributeValue("AreaName");
    }

    /**
     * 【必选 属性】
     * 设置 页块引用
     * <p>
     * 通过该ID在页面中定位容器元素。
     *
     * @param pageBlockID 页块引用
     * @return this
     */
    public CT_AreaHolderBlock setPageBlockID(ST_RefID pageBlockID) {
        if (pageBlockID == null) {
            throw new IllegalArgumentException("页块引用（pageBlockID）不能为空");
        }

        this.addAttribute("PageBlockID", pageBlockID.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 页块引用
     * <p>
     * 通过该ID在页面中定位容器元素。
     *
     * @return 页块引用
     */
    public ST_RefID getPageBlockID() {
        String pageBlock = this.attributeValue("PageBlockID");
        if (pageBlock == null || pageBlock.trim().length() == 0) {
            return null;
        }
        return ST_RefID.getInstance(pageBlock);
    }

    /**
     * 【必选 属性】
     * 设置 区域外接矩形
     * <p>
     * 区域占位区块在页面中的位置和大小。
     *
     * @param boundary 区域外接矩形
     * @return this
     */
    public CT_AreaHolderBlock setBoundary(ST_Box boundary) {
        if (boundary == null) {
            throw new IllegalArgumentException("区域外接矩形（boundary）为空");
        }
        this.addAttribute("Boundary", boundary.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 区域外接矩形
     * <p>
     * 区域占位区块在页面中的位置和大小。
     *
     * @return 区域外接矩形
     */
    public ST_Box getBoundary() {
        String boundary = this.attributeValue("Boundary");
        if (boundary == null || boundary.trim().length() == 0) {
            return null;
        }
        return ST_Box.getInstance(boundary);
    }


    /**
     * 【必选】
     * 设置 页面文件
     *
     * @param pageLoc 页面文件路径，占位区域块所在页面的在OFD容器中的绝对路径。
     * @return this
     */
    public CT_AreaHolderBlock setPageFile(ST_Loc pageLoc) {
        if (pageLoc == null || pageLoc.isEmpty()) {
            throw new IllegalArgumentException("页面对象路径（pageLoc）为空");
        }
        this.setOFDEntity("PageFile", pageLoc.toString());
        return this;
    }

    /**
     * 【必选】
     * 设置 页面文件
     *
     * @param absPath 页面文件路径，占位区域块所在页面的在OFD容器中的绝对路径。
     * @return this
     */
    public CT_AreaHolderBlock setFontFile(String absPath) {
        return setPageFile(new ST_Loc(absPath));
    }

    /**
     * 【必选】
     * 获取 页面文件
     *
     * @return 占位区域块所在页面的在OFD容器中的绝对路径。
     */
    public ST_Loc getFontFile() {
        return ST_Loc.getInstance(this.getOFDElementText("PageFile"));
    }
}
