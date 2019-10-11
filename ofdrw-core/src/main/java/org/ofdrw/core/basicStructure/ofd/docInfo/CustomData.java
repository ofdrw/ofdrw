package org.ofdrw.core.basicStructure.ofd.docInfo;

import org.dom4j.Element;
import org.ofdrw.core.OFDElement;

/**
 * 用户自定义元数据，可以指定一个名称及其对应的值
 *
 * @author 权观宇
 * @since 2019-10-01 07:38:27
 */
public class CustomData extends OFDElement {
    public CustomData(Element proxy) {
        super(proxy);
    }

    /**
     * 自定元数据
     *
     * @param name  元数据名称
     * @param value 元数据值
     */
    public CustomData(String name, String value) {
        super("CustomData");
        this.addAttribute("Name", name);
        this.addText(value);
    }

    /**
     * 【必选】
     * 获取元数据名称
     * <p>
     * 默认获取第一个属性作为元数据名称
     *
     * @return 元数据名称(Name)
     */
    public String getDataName() {
        return this.attributeValue("Name");
    }

    /**
     * 【必选 属性】
     * 设置元数据名称
     *
     * @return this
     */
    public CustomData setDataName(String name) {
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 获取元数据值
     *
     * @return 元数据值
     */
    public String getValue() {
        return this.getText();
    }

    /**
     * 设置元数据的值
     *
     * @param value 元数据值
     * @return this
     */
    public CustomData setValue(String value) {
        this.setText(value);
        return this;
    }

    @Override
    public String getQualifiedName() {
        return "ofd:CustomData";
    }
}
