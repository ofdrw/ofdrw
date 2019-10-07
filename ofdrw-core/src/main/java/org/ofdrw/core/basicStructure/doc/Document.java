package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.basicStructure.OFDElement;
import org.ofdrw.core.basicStructure.doc.permission.CT_Permission;
import org.ofdrw.core.basicStructure.doc.vpreferences.CT_VPreferences;
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

    /**
     * 【可选】
     * 设置 文档的权限声明
     *
     * @param permission 文档的权限声明
     * @return this
     */
    public Document setPermissions(CT_Permission permission) {
        this.add(permission);
        return this;
    }

    /**
     * 【可选】
     * 获取 文档的权限声明
     *
     * @return 文档的权限声明
     */
    public CT_Permission getPermission() {
        Element e = this.getOFDElement("Permission");
        return e == null ? null : new CT_Permission(e);
    }

    /**
     * 【可选】
     * 设置 文档关联的动作序列
     * <p>
     * 当存在多个 Action 对象时，所有动作依次执行
     *
     * @param actions 文档关联的动作序列
     * @return this
     */
    public Document setActions(Actions actions) {
        this.add(actions);
        return this;
    }

    /**
     * 【可选】
     * 获取 文档关联的动作序列
     * <p>
     * 当存在多个 Action 对象时，所有动作依次执行
     *
     * @return 文档关联的动作序列
     */
    public Actions getActions() {
        Element e = this.getOFDElement("Actions");
        return e == null ? null : new Actions(e);
    }

    /**
     * 【可选】
     * 设置 文档的视图首选项
     *
     * @param vPreferences 文档的视图首选项
     * @return this
     */
    public Document setVPreferences(CT_VPreferences vPreferences) {
        this.add(vPreferences);
        return this;
    }

    /**
     * 【可选】
     * 获取 文档的视图首选项
     *
     * @return 文档的视图首选项
     */
    public CT_VPreferences getVPreferences() {
        Element e = this.getOFDElement("VPreferences");
        return e == null ? null : new CT_VPreferences(e);
    }

    // TODO 2019-10-7 23:00:54 Bookmarks
}
