package org.ofdrw.core.basicStructure.doc.bookmark;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.actionGoto.CT_Dest;
import org.ofdrw.core.OFDElement;

/**
 * 本标准支持书签，可以将常用位置定义为书签，
 * 文档可以包含一组书签。
 * <p>
 * 7.5 图 11 书签结构
 *
 * @author 权观宇
 * @since 2019-10-09 08:06:35
 */
public class Bookmark extends OFDElement {
    public Bookmark(Element proxy) {
        super(proxy);
    }

    public Bookmark() {
        super("Bookmark");
    }

    /**
     * @param name 书签名称
     * @param dest 书签对应的文档版位置
     */
    public Bookmark(String name, CT_Dest dest) {
        this();
        this.setBookmarkName(name)
                .setDest(dest);
    }

    /**
     * 【必选 属性】
     * 设置 书签名称
     *
     * @param name 书签名称
     * @return this
     */
    public Bookmark setBookmarkName(String name) {
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 书签名称
     *
     * @return 书签名称
     */
    public String getBookmarkName() {
        return this.attributeValue("Name");
    }

    /**
     * 【必选】
     * 设置 书签对应的文档版位置
     * <p>
     * 见表 54
     *
     * @param dest 书签对应的文档版位置
     * @return this
     */
    public Bookmark setDest(CT_Dest dest) {
        this.set(dest);
        return this;
    }

    /**
     * 【必选】
     * 获取 书签对应的文档版位置
     * <p>
     * 见表 54
     *
     * @return 书签对应的文档版位置
     */
    public CT_Dest getDest() {
        Element e = this.getOFDElement("Dest");
        return e == null ? null : new CT_Dest(e);
    }
}
