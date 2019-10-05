package org.ofdrw.core.action.actionType.actionGoto;

import org.dom4j.Element;
import org.ofdrw.core.action.actionType.GotoTarget;

/**
 * 跳转的目的书签
 * <p>
 * 表 53 跳转动作属性
 *
 * @author 权观宇
 * @since 2019-10-05 09:10:51
 */
public class Bookmark extends GotoTarget {
    public Bookmark(Element proxy) {
        super(proxy);
    }

    public Bookmark() {
        super("Bookmark");
    }

    public Bookmark(String name) {
        this();
        setBookmarkName(name);
    }

    /**
     * 【必选 属性】
     * 设置 目标书签的名称，引用文档书签中的名称
     *
     * @param name 目标书签的名称
     * @return this
     */
    public Bookmark setBookmarkName(String name) {
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 目标书签的名称，引用文档书签中的名称
     *
     * @return 目标书签的名称
     */
    public String getBookmarkName() {
        return this.attributeValue("Name");
    }
}
