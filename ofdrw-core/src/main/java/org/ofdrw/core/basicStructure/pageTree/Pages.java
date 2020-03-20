package org.ofdrw.core.basicStructure.pageTree;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 页树
 * <p>
 * 图 12 页树结构
 *
 * @author 权观宇
 * @since 2019-10-05 10:39:31
 */
public class Pages extends OFDElement {
    public Pages(Element proxy) {
        super(proxy);
    }

    public Pages() {
        super("Pages");
    }


    /**
     * 【必选】
     * 增加 叶节点
     * <p>
     * 一个页树中可以包含一个或多个叶节点，页顺序是
     * 根据页树进行前序遍历时叶节点的顺序。
     *
     * @param page 叶节点
     * @return this
     */
    public Pages addPage(Page page) {
        this.add(page);
        return this;
    }

    /**
     * 获取页面数量
     *
     * @return 页面数量
     */
    public int getSize() {
        return this.elements().size();
    }


    /**
     * 【必选】
     * 获取 叶节点序列
     * <p>
     * 一个页树中可以包含一个或多个叶节点，页顺序是
     * 根据页树进行前序遍历时叶节点的顺序。
     *
     * @return 叶节点序列 （大于等于 1）
     */
    public List<Page> getPages() {
        return this.getOFDElements("Page",Page::new);
    }
}
