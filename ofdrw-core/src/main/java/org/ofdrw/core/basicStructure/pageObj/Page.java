package org.ofdrw.core.basicStructure.pageObj;

import org.dom4j.Element;
import org.ofdrw.core.Const;
import org.ofdrw.core.action.Actions;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 页对象
 * <p>
 * 页对象支持模板页描述，每一页经常要重复显示的内容可统一在模板页中描述，
 * 文档可以包含多个模板页。通过使用模板页可以使重复显示的内容不必出现在
 * 描述每一页的页面描述内容中，而只需通过 Template 节点进行应用。
 * <p>
 * 7.7 图 13 页对象结构；表 12 页对象属性
 *
 * @author 权观宇
 * @since 2019-10-09 09:38:35
 */
public class Page extends OFDElement {

    public Page(Element proxy) {
        super(proxy);
    }

    public Page() {
        super("Page");
    }


    /**
     * 【可选】
     * 设置 页面区域的大小和位置，仅对该页面有效。
     * <p>
     * 该节点不出现时则使用模板页中的定义，如果模板页不存在或模板页中
     * 没有定义页面区域，则使用文件 CommonData 中的定义。
     *
     * @param area 页面区域的大小和位置
     * @return this
     */
    public Page setArea(CT_PageArea area) {
        area.setOFDName("Area");
        this.set(area);
        return this;
    }

    /**
     * 【可选】
     * 获取 页面区域的大小和位置，仅对该页面有效。
     * <p>
     * 该节点不出现时则使用模板页中的定义，如果模板页不存在或模板页中
     * 没有定义页面区域，则使用文件 CommonData 中的定义。
     *
     * @return 页面区域的大小和位置
     */
    public CT_PageArea getArea() {
        Element e = this.getOFDElement("Area");
        return e == null ? null : new CT_PageArea(e);
    }


    /**
     * 【可选】
     * 设置 页面使用的模板页
     * <p>
     * 模板页的内容和结构与普通页相同，定义在 CommonData
     * 指定的 XML 文件中。一个页可以使用多个模板页。该节点
     * 使用是通过 TemplateID 来引用具体模板，并通过 ZOrder
     * 属性来控制模板在页面中的显示顺序。
     * <p>
     * 注：在模板页的内容描述中该属性无效。
     *
     * @param template 页面使用的模板页
     * @return this
     */
    public Page addTemplate(Template template) {
        if (template == null) {
            return this;
        }
        this.add(template);
        return this;
    }

    /**
     * @param template 模板
     * @return this
     * @deprecated {@link #addTemplate(Template)}
     */
    @Deprecated
    public Page setTemplate(Template template) {
        this.set(template);
        return this;
    }

    /**
     * 【可选】
     * 获取 页面使用的模板页
     * <p>
     * 模板页的内容和结构与普通页相同，定义在 CommonData
     * 指定的 XML 文件中。一个页可以使用多个模板页。该节点
     * 使用是通过 TemplateID 来引用具体模板，并通过 ZOrder
     * 属性来控制模板在页面中的显示顺序。
     * <p>
     * 注：在模板页的内容描述中该属性无效。
     *
     * @return 页面使用的模板页
     */
    public List<Template> getTemplates() {
        return this.getOFDElements("Template", Template::new);
    }

    /**
     * @return 页面使用的模板页(第一个)
     * @deprecated {@link #getTemplates()}
     */
    @Deprecated
    public Template getTemplate() {
        Element e = this.getOFDElement("Template");
        return e == null ? null : new Template(e);
    }


    /**
     * 【可选】
     * 设置 页资源
     * <p>
     * 指向该页使用的资源文件
     *
     * @param pageRes 页资源路径
     * @return this
     */
    public Page addPageRes(ST_Loc pageRes) {
        this.addOFDEntity("PageRes", pageRes);
        return this;
    }

    /**
     * 【可选】
     * 获取 页资源
     * <p>
     * 指向该页使用的资源文件
     *
     * @return 页资源路径列表
     */
    public List<ST_Loc> getPageResList() {
        return this.getOFDElements("PageRes", e -> new ST_Loc(e.getText()));
    }


    /**
     * 【可选】
     * 设置 页面内容描述，该节点不存在时，标识空白页
     *
     * @param content 页面内容
     * @return this
     */
    public Page setContent(Content content) {
        this.set(content);
        return this;
    }

    /**
     * 【可选】
     * 获取 页面内容描述，该节点不存在时，标识空白页
     *
     * @return 页面内容
     */
    public Content getContent() {
        Element e = this.getOFDElement("Content");
        return e == null ? null : new Content(e);
    }


    /**
     * 【可选】
     * 设置 与页面关联的动作序列。
     * <p>
     * 当存在多个 Action对象时，所有动作依次执行。
     * <p>
     * 动作列表的动作与页面关联，事件类型为 PO（页面打开，见表 52 事件类型）
     *
     * @param actions 动作序列
     * @return this
     */
    public Page setActions(Actions actions) {
        this.set(actions);
        return this;
    }

    /**
     * 【可选】
     * 设置 与页面关联的动作序列。
     * <p>
     * 当存在多个 Action对象时，所有动作依次执行。
     * <p>
     * 动作列表的动作与页面关联，事件类型为 PO（页面打开，见表 52 事件类型）
     *
     * @return 动作序列
     */
    public Actions getActions() {
        Element e = this.getOFDElement("Actions");
        return e == null ? null : new Actions(e);
    }
}
