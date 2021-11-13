package org.ofdrw.reader.model;

import org.ofdrw.core.basicStructure.pageObj.CT_TemplatePage;
import org.ofdrw.core.basicStructure.pageObj.Page;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_ID;
import org.ofdrw.core.basicType.ST_Loc;

/**
 * 页面模板对象实体
 *
 * @author 权观宇
 * @since 2021-04-17 16:17:34
 */
public class TemplatePageEntity {

    /**
     * 模板内容
     */
    private Page page;


    /**
     * 模板信息
     */
    private CT_TemplatePage tplInfo;

    /**
     * 页面顺序
     */
    private Type order;

    /**
     * 创建实例
     *
     * @param tplInfo 模板信息
     * @param page    模板页面
     */
    public TemplatePageEntity(CT_TemplatePage tplInfo, Page page) {
        this.page = page;
        this.tplInfo = tplInfo;
        this.order = tplInfo.getZOrder();
    }

    /**
     * 用于构造排序
     *
     * @param order 顺序
     * @param page  页面内容
     */
    public TemplatePageEntity(Type order, Page page) {
        this.page = page;
        this.order = order;
    }

    /**
     * 获取模板ID
     *
     * @return 模板ID
     */
    public String getID() {
        return tplInfo.getID().toString();
    }

    /**
     * 获取 模板页名称
     *
     * @return 模板页名称，可能为null
     */
    public String getTemplatePageName() {
        final ST_ID name = tplInfo.getTemplatePageName();
        if (name == null) {
            return null;
        }
        return name.toString();
    }

    /**
     * 获取 模板页的默认视图类型
     *
     * @return 模板页的默认视图类型
     */
    public Type getZOrder() {
        return this.order;
    }

    /**
     * 通过页面配置的oder覆盖commonData配置的order
     *
     * @param order 新的顺序
     */
    public void setOrder(Type order) {
        if (order == null) {
            return;
        }
        this.order = order;
    }

    /**
     * 获取 指向模板页内容描述文件 路径
     *
     * @return 指向模板页内容描述文件 路径
     */
    public ST_Loc getBaseLoc() {
        return tplInfo.getBaseLoc();
    }

    /**
     * 获取模板页面内容
     *
     * @return 页面内容
     */
    public Page getPage() {
        return page;
    }

    /**
     * 模板对象信息
     *
     * @return 模板信息
     */
    public CT_TemplatePage getTplInfo() {
        return tplInfo;
    }
}
