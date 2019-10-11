package org.ofdrw.core.basicStructure.pageObj;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 模板页
 * <p>
 * ————《GB/T 33190-2016》 图 14
 *
 * @author 权观宇
 * @since 2019-10-04 10:37:10
 */
public class CT_TemplatePage extends OFDElement {
    public CT_TemplatePage(Element proxy) {
        super(proxy);
    }

    public CT_TemplatePage() {
        super("TemplatePage");
    }

    /**
     * 【必选 属性】
     * 设置 模板页的标识符，不能与已有标识符重复
     *
     * @param id 模板页的标识符
     * @return this
     */
    public CT_TemplatePage setID(ST_ID id) {
        this.addAttribute("ID", id.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 模板页的标识符，不能与已有标识符重复
     *
     * @return 模板页的标识符
     */
    public ST_ID getID() {
        return ST_ID.getInstance(this.attributeValue("ID"));
    }


    /**
     * 【可选 属性】
     * 设置 模板页名称
     *
     * @param name 模板页名称
     * @return this
     */
    public CT_TemplatePage setTemplatePageName(String name) {
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 模板页名称
     *
     * @return 模板页名称
     */
    public ST_ID getTemplatePageName() {
        return ST_ID.getInstance(this.attributeValue("Name"));
    }


    /**
     * 【可选 属性】
     * 设置 模板页的默认视图类型
     * <p>
     * 其类型描述和呈现顺序与 Layer中 Type的描述和处理一致，见表 15
     * 如果页面引用的多个模板的次属性相同，则应根据引用的顺序来显示
     * 先引用者先绘制
     * <p>
     * 默认值为 Background
     *
     * @param zOrder 模板页的默认视图类型
     * @return this
     */
    public CT_TemplatePage setZOrder(String zOrder) {
        this.addAttribute("ZOrder", zOrder);
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 模板页的默认视图类型
     * <p>
     * 其类型描述和呈现顺序与 Layer中 Type的描述和处理一致，见表 15
     * 如果页面引用的多个模板的次属性相同，则应根据引用的顺序来显示
     * 先引用者先绘制
     * <p>
     * 默认值为 Background
     *
     * @return 模板页的默认视图类型
     */
    public String getZOrder() {
        String value = this.attributeValue("ZOrder");
        return (value == null || value.trim().length() == 0) ? "Background" : value;
    }


    /**
     * 【可选 属性】
     * 设置 指向模板页内容描述文件 路径
     *
     * @param baseLoc 指向模板页内容描述文件 路径
     * @return this
     */
    public CT_TemplatePage setBaseLoc(ST_Loc baseLoc) {
        this.addAttribute("BaseLoc", baseLoc.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 指向模板页内容描述文件 路径
     *
     * @return 指向模板页内容描述文件 路径
     */
    public ST_Loc getBaseLoc() {
        return ST_Loc.getInstance(this.attributeValue("BaseLoc"));
    }
}
