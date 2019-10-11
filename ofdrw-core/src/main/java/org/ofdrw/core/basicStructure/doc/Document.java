package org.ofdrw.core.basicStructure.doc;

import org.dom4j.Element;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.bookmark.Bookmarks;
import org.ofdrw.core.basicStructure.doc.permission.CT_Permission;
import org.ofdrw.core.basicStructure.doc.vpreferences.CT_VPreferences;
import org.ofdrw.core.basicStructure.outlines.Outlines;
import org.ofdrw.core.basicStructure.pageTree.Pages;
import org.ofdrw.core.basicType.ST_Loc;

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


    /**
     * 【可选】
     * 设置 文档的书签集，包含一组书签
     * <p>
     * 7.5 文档根节点 表 5 文档根节点属性
     *
     * @param bookmarks 文档的书签集
     * @return this
     */
    public Document setBookmarks(Bookmarks bookmarks) {
        this.add(bookmarks);
        return this;
    }

    /**
     * 【可选】
     * 获取 文档的书签集，包含一组书签
     * <p>
     * 7.5 文档根节点 表 5 文档根节点属性
     *
     * @return 文档的书签集
     */
    public Bookmarks getBookmarks() {
        Element e = this.getOFDElement("Bookmarks");
        return e == null ? null : new Bookmarks(e);
    }

    /**
     * 【可选】
     * 设置 指向注释列表的文件
     * <p>
     * 有关注释描述见第 15 章
     *
     * @param annotations 指向注释列表的文件路径
     * @return this
     */
    public Document setAnnotations(ST_Loc annotations) {
        this.addOFDEntity("Annotations", annotations.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 指向注释列表的文件
     * <p>
     * 有关注释描述见第 15 章
     *
     * @return 指向注释列表的文件路径
     */
    public ST_Loc getAnnotations() {
        return ST_Loc.getInstance(this.getOFDElementText("Annotations"));
    }

    /**
     * 【可选】
     * 设置 指向自定义标引列表文件
     * <p>
     * 有关自定义标引描述见第 16 章
     *
     * @param customTags 指向自定义标引列表文件路径
     * @return this
     */
    public Document setCustomTags(ST_Loc customTags) {
        this.addOFDEntity("CustomTags", customTags.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 指向自定义标引列表文件
     * <p>
     * 有关自定义标引描述见第 16 章
     *
     * @return 指向自定义标引列表文件路径
     */
    public ST_Loc getCustomTags() {
        return ST_Loc.getInstance(this.getOFDElementText("CustomTags"));
    }

    /**
     * 【可选】
     * 设置 指向附件列表文件
     * <p>
     * 有关附件描述见第 20 章
     *
     * @param attachments 指向附件列表文件路径
     * @return this
     */
    public Document setAttachments(ST_Loc attachments) {
        this.addOFDEntity("Attachments", attachments.toString());
        return this;
    }

    /**
     * 【可选】
     * 获取 指向附件列表文件
     * <p>
     * 有关附件描述见第 20 章
     *
     * @return 指向附件列表文件路径
     */
    public ST_Loc getAttachments() {
        return ST_Loc.getInstance(this.getOFDElementText("Attachments"));
    }

    /**
     * 【可选】
     * 设置 指向扩展列表文件
     * <p>
     * 有关扩展列表文件见第 17 章
     *
     * @param extensions 指向扩展列表文件路径
     * @return this
     */
    public Document setExtensions(ST_Loc extensions) {
        this.addOFDEntity("Extensions", extensions.toString());
        return this;
    }

    /**
     * 【可选】
     * 设置 指向扩展列表文件
     * <p>
     * 有关扩展列表文件见第 17 章
     *
     * @return 指向扩展列表文件路径
     */
    public ST_Loc getExtensions() {
        return ST_Loc.getInstance(this.getOFDElementText("Extensions"));
    }

}
