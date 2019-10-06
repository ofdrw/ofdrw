package org.ofdrw.core.basicStructure.outlines;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.function.Consumer;

/**
 * 大纲按照树形结构进行组织
 * <p>
 * 图 18 大纲节点结构
 *
 * @author 权观宇
 * @since 2019-10-05 11:12:39
 */
public class Outlines extends OFDElement implements Iterable<CT_OutlineElem> {
    public Outlines(Element proxy) {
        super(proxy);
    }

    public Outlines() {
        super("Outlines");
    }

    /**
     * 【必选】
     * 增加 大纲节点
     *
     * @param outlineElem 大纲节点
     * @return this
     */
    public Outlines addOutlineElem(CT_OutlineElem outlineElem) {
        this.add(outlineElem);
        return this;
    }

    /**
     * 【必选】
     * 增加 大纲节点
     *
     * @return 大纲节点
     */
    public List<CT_OutlineElem> getOutlineElems() {
        List<Element> elements = this.getOFDElements("OutlineElem");
        List<CT_OutlineElem> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new CT_OutlineElem(item)));
        return res;
    }

    @Override
    public Iterator<CT_OutlineElem> iterator() {
        return getOutlineElems().iterator();
    }

    @Override
    public void forEach(Consumer<? super CT_OutlineElem> action) {
        getOutlineElems().forEach(action);
    }

    /**
     * 未实现 不可使用
     * @return null
     */
    @Deprecated
    @Override
    public Spliterator<CT_OutlineElem> spliterator() {
        return null;
    }
}
