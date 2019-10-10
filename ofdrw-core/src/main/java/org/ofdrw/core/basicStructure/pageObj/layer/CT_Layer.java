package org.ofdrw.core.basicStructure.pageObj.layer;

import org.dom4j.Element;
import org.ofdrw.core.basicType.ST_RefID;

public class CT_Layer extends CT_PageBlock {
    public CT_Layer(Element proxy) {
        super(proxy);
    }

    public CT_Layer() {
        super("Layer");
    }

    /**
     * 【可选 属性】
     * 设置 层类型描述
     * <p>
     * 默认值为 Body
     *
     * @param type 层类型
     * @return this
     */
    public CT_Layer setType(Type type) {
        type = type == null ? Type.Body : type;
        this.addAttribute("Type", type.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 层类型描述
     * <p>
     * 默认值为 Body
     *
     * @return 层类型
     */
    public Type getType() {
        Type type = Type.getInstance(this.attributeValue("Type"));
        return type == null ? Type.Body : type;
    }

    /**
     * 【可选 属性】
     * 设置 图层的绘制参数，引用资源文件总定义的绘制参数标识
     *
     * @param drawParam 资源文件总定义的绘制参数标识
     * @return this
     */
    public CT_Layer setDrawParam(ST_RefID drawParam) {
        this.addAttribute("DrawParam", drawParam.toString());
        return this;
    }

    /**
     * 【可选 属性】
     * 获取 图层的绘制参数，引用资源文件总定义的绘制参数标识
     *
     * @return 资源文件总定义的绘制参数标识，null表示不存在
     */
    public ST_RefID getDrawParam() {
        return ST_RefID.getInstance(this.attributeValue("DrawParam"));
    }
}
