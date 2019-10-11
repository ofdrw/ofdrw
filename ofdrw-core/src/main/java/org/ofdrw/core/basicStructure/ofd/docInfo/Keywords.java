package org.ofdrw.core.basicStructure.ofd.docInfo;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.ArrayList;
import java.util.List;

/**
 * 关键词集合，每一个关键词用一个“Keyword”子节点来表达
 * <p>
 * 表 4 文档元数据属性
 *
 * @author 权观宇
 * @since 2019-10-01 05:44:42
 */
public class Keywords extends OFDElement {

    public Keywords(Element proxy) {
        super(proxy);
    }

    public Keywords() {
        super("Keywords");
    }

    @Override
    public String getQualifiedName() {
        return "ofd:Keywords";
    }

    /**
     * 【必选】
     * 增加关键字
     *
     * @param keyword 关键字
     * @return this
     */
    public Keywords addKeyword(String keyword) {
        this.addOFDEntity("Keyword", keyword);
        return this;
    }

    /**
     * 获取关键字列表
     *
     * @return 关键字列表
     */
    public List<String> getKeywords() {
        List<Element> elements = this.getOFDElements("Keywords");
        List<String> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(item.getText()));
        return res;
    }

}
