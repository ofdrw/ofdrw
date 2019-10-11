package org.ofdrw.core.basicStructure.pageTree;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 页节点
 * <p>
 * 表 11 页树属性
 *
 * @author 权观宇
 * @since 2019-10-05 10:49:09
 */
public class Page extends OFDElement {
    public Page(Element proxy) {
        super(proxy);
    }

    public Page() {
        super("Page");
    }

    public Page(long id, String baseLoc) {
        this();
        this.setID(new ST_ID(id))
                .setBaseLoc(ST_Loc.getInstance(baseLoc));
    }

    public Page(ST_ID id, ST_Loc baseLoc) {
        this();
        this.setID(id)
                .setBaseLoc(baseLoc);
    }

    /**
     * 【必选 属性】
     * 设置 页的标识符，不能与已有标识重复
     *
     * @param id 页的标识符
     * @return this
     */
    public Page setID(ST_ID id) {
        this.addAttribute("ID", id.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 页的标识符，不能与已有标识重复
     *
     * @return 页的标识符
     */
    public ST_ID getID() {
        return ST_ID.getInstance(this.attributeValue("ID"));
    }

    /**
     * 【必选 属性】
     * 设置 页对象描述文件
     *
     * @param baseLoc 页对象描述文件路径
     * @return this
     */
    public Page setBaseLoc(ST_Loc baseLoc) {
        this.addAttribute("BaseLoc", baseLoc.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 页对象描述文件
     *
     * @return 页对象描述文件路径
     */
    public ST_Loc getBaseLoc() {
        return ST_Loc.getInstance(this.attributeValue("BaseLoc"));
    }
}
