package org.ofdrw.core.basicStructure.pageObj;

import org.dom4j.Element;
import org.ofdrw.core.basicStructure.OFDElement;
import org.ofdrw.core.basicStructure.doc.CT_PageArea;
import org.ofdrw.core.basicType.ST_Loc;

import java.util.ArrayList;
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
        this.add(area);
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
    public Page setTemplate(Template template) {
        this.add(template);
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
        List<Element> elements = this.getOFDElements("PageRes");
        List<ST_Loc> res = new ArrayList<>(elements.size());
        elements.forEach(item -> res.add(new ST_Loc(item.getText())));
        return res;
    }

    // TODO 2019-10-10 18:31:10 Content
}
