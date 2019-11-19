package org.ofdrw.core.annotation.pageannot;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 分页注释文件
 * <p>
 * 15.2 图 81 表 61
 *
 * @author 权观宇
 * @since 2019-11-16 02:20:54
 */
public class PageAnnot extends OFDElement {

    public PageAnnot(Element proxy) {
        super(proxy);
    }

    public PageAnnot() {
        super("PageAnnot");
    }

    /**
     * 【必选】
     * 增加 注释对象
     *
     * @param annot 注释对象
     * @return this
     */
    public PageAnnot addAnnot(Annot annot) {
        if (annot == null) {
            return this;
        }
        this.add(annot);
        return this;
    }

    /**
     * 【必选】
     * 获取 注释对象列表
     *
     * @return 注释对象列表
     */
    public List<Annot> getAnnots() {
        return this.getOFDElements("Annot", Annot::new);
    }
}
