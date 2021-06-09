package org.ofdrw.core.basicStructure.doc.bookmark;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 文档的书签集，包含一组书签
 * <p>
 * 7.5 文档根节点 表 5 文档根节点属性
 *
 * @author 权观宇
 * @since 2019-10-09 08:01:58
 */
public class Bookmarks extends OFDElement {
    public Bookmarks(Element proxy) {
        super(proxy);
    }

    public Bookmarks() {
        super("Bookmarks");
    }

    /**
     * 【必选】
     * 增加 书签
     *
     * @param bookmark 书签
     * @return this
     */
    public Bookmarks addBookmark(Bookmark bookmark) {
        this.add(bookmark);
        return this;
    }

    /**
     * 【必选】
     * 获取 书签列表
     *
     * @return 书签列表
     */
    public List<Bookmark> getBookmarks() {
        return this.getElements("Bookmark",Bookmark::new);
    }

}
