package org.ofdrw.core.signatures.sig;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

/**
 * 签名扩展属性 OFD 2.0
 * <p>
 * 记录一个属性的“键”（Name）和“值”，值在该节点的内容中记录。
 * <p>
 * GMT0099 B.2
 *
 * @author 权观宇
 * @since 2021-06-19 13:02:59
 */
public class Parameter extends OFDElement {
    public Parameter(Element proxy) {
        super(proxy);
    }

    public Parameter() {
        super("Parameter");
    }

    /**
     * 创建  签名扩展属性
     *
     * @param name  属性的名称
     * @param type  属性的类型
     * @param value 属性的值
     */
    public Parameter(@NotNull String name, @Nullable String type, @Nullable String value) {
        this();
        setNameAttr(name);
        setType(type);
        setValue(value);
    }

    /**
     * 创建  签名扩展属性
     *
     * @param name  属性的名称
     * @param value 属性的值
     */
    public Parameter(@NotNull String name, @Nullable String value) {
        this();
        setNameAttr(name);
        setValue(value);
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 设置 扩展属性的名称
     *
     * @param name 扩展属性的名称
     * @return this
     */
    public Parameter setNameAttr(@NotNull String name) {
        if (name == null || name.trim().length() == 0) {
            throw new IllegalArgumentException("扩展属性的名称（Name）不能为空");
        }
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 获取 扩展属性的名称
     *
     * @return 扩展属性的名称
     */
    @NotNull
    public String getNameAttr() {
        return this.attributeValue("Name");
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 设置 扩展属性的值类型
     *
     * @param type 扩展属性的值类型,null表示删除属性
     * @return this
     */
    public Parameter setType(@Nullable String type) {
        if (type == null) {
            this.removeAttr(type);
            return this;
        }
        this.addAttribute("Type", type);
        return this;
    }

    /**
     * 【可选 属性 OFD 2.0】
     * 获取 扩展属性的值类型
     *
     * @return 扩展属性的值类型，可能为空
     */
    @Nullable
    public String getType() {
        return this.attributeValue("Type");
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 签名扩展属性值
     *
     * @param value 签名扩展属性值
     * @return this
     */
    public Parameter setValue(@Nullable String value) {
        if (value == null) {
            value = "";
        }
        this.setText(value);
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 签名扩展属性值
     *
     * @return 签名扩展属性值，或空串
     */
    @NotNull
    public String getValue() {
        return this.getTextTrim();
    }
}
