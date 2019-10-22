package org.ofdrw.core.basicStructure.pageObj;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;
import org.ofdrw.core.basicStructure.pageObj.layer.Type;
import org.ofdrw.core.basicType.ST_RefID;

/**
 * 页面使用的模板页
 * <p>
 * 模板页的内容和结构与普通页相同，定义在 CommonData
 * 指定的 XML 文件中。一个页可以使用多个模板页。该节点
 * 使用是通过 TemplateID 来引用具体模板，并通过 ZOrder
 * 属性来控制模板在页面中的显示顺序。
 * <p>
 * 注：在模板页的内容描述中该属性无效。
 *
 * @author 权观宇
 * @since 2019-10-09 09:44:37
 */
public class Template extends OFDElement {
    public Template(Element proxy) {
        super(proxy);
    }

    public Template() {
        super("Template");
    }

    /**
     * 【必选 属性】
     * 设置 引用在文档共用数据（CommonData）中定义的模板标识符
     *
     * @param templateId 引用在文档共用数据（CommonData）中定义的模板标识符
     * @return this
     */
    public Template setTemplateID(ST_RefID templateId) {
        this.addAttribute("TemplateID", templateId.toString());
        return this;
    }

    /**
     * 【必选 属性】
     * 获取 引用在文档共用数据（CommonData）中定义的模板标识符
     *
     * @return 引用在文档共用数据（CommonData）中定义的模板标识符
     */
    public ST_RefID getTemplateID() {
        return ST_RefID.getInstance(this.attributeValue("TemplateID"));
    }

    /**
     * 【可选 属性】
     * 设置 模板在页面中的呈现顺序
     * <p>
     * 控制模板在页面中的呈现顺序，其类型描述和呈现顺序与Layer中Type的描述和处理一直。
     * <p>
     * 如果多个图层的此属性相同，则应根据其出现的顺序来显示，先出现者先绘制
     * <p>
     * 默认值为 Background
     *
     * @param zOrder 模板在页面中的呈现顺序
     * @return this
     */
    public Template setZOrder(Type zOrder) {
        if (zOrder == null) {
            this.removeAttr("ZOrder");
            return this;
        }
        this.addAttribute("ZOrder", zOrder.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 模板在页面中的呈现顺序
     * <p>
     * 控制模板在页面中的呈现顺序，其类型描述和呈现顺序与Layer中Type的描述和处理一直。
     * <p>
     * 如果多个图层的此属性相同，则应根据其出现的顺序来显示，先出现者先绘制
     * <p>
     * 默认值为 Background
     *
     * @return 模板在页面中的呈现顺序
     */
    public Type getZOrder() {
        Type type = Type.getInstance(this.attributeValue("ZOrder"));
        return type == null ? Type.Background : type;
    }
}
