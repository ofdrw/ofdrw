package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;
import org.ofdrw.core.basicStructure.outlines.Outlines;
import org.ofdrw.core.basicStructure.page.tree.Pages;

/**
 * 文档根节点
 * Document.xml
 * <p>
 * ————《GB/T 33190-2016》 图 5
 *
 * @author 权观宇
 * @since 2019-10-04 07:43:14
 */
public class Document extends OFDElement {
    public Document(Element proxy) {
        super(proxy);
    }

    public Document() {
        super("Document");
    }

    /**
     * 【必选】
     * 设置 文档公共数据
     * <p>
     * 定义了页面区域、公共资源
     *
     * @param commonData 文档公共数据
     * @return this
     */
    public Document setCommonData(CT_CommonData commonData) {
        this.add(commonData);
        return this;
    }

    /**
     * 【必选】
     * 获取 文档公共数据
     * <p>
     * 定义了页面区域、公共资源
     *
     * @return 文档公共数据
     */
    public CT_CommonData getCommonData() {
        Element e = this.getOFDElement("CommonData");
        return e == null ? null : new CT_CommonData(e);
    }

    /**
     * 【必选】
     * 设置 页树
     * <p>
     * 有关页树的描述键 7.6
     *
     * @param pages 页树
     * @return this
     */
    public Document setPages(Pages pages) {
        this.add(pages);
        return this;
    }

    /**
     * 【必选】
     * 获取 页树
     * <p>
     * 有关页树的描述键 7.6
     *
     * @return 页树
     */
    public Pages getPages() {
        Element e = this.getOFDElement("Pages");
        return e == null ? null : new Pages(e);
    }

    /**
     * 【可选】
     * 设置 大纲
     *
     * @param outlines 大纲
     * @return this
     */
    public Document setOutlines(Outlines outlines) {
        this.add(outlines);
        return this;
    }

    /**
     * 【可选】
     * 获取 大纲
     *
     * @return 大纲
     */
    public Outlines getOutlines() {
        Element e = this.getOFDElement("Outlines");
        return e == null ? null : new Outlines(e);
    }
    // TODO 2019-10-6 20:02:54 Permissions
}
