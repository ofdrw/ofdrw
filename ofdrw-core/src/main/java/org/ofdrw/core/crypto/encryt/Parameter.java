package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

/**
 * 扩展参数
 * <p>
 * GMT0099 附录C 图 C.1
 *
 * @author 权观宇
 * @since 2021-06-23 19:43:45
 */
public class Parameter extends OFDElement {
    public Parameter(Element proxy) {
        super(proxy);
    }

    public Parameter() {
        super("Parameter");
    }

    /**
     * 创建扩展参数
     *
     * @param name 扩展参数名，不能为空
     */
    public Parameter(@NotNull String name) {
        this();
        setAttrName(name);
    }

    /**
     * 创建扩展参数
     *
     * @param name  扩展参数名，不能为空
     * @param value 参数值，可选
     */
    public Parameter(@NotNull String name, @Nullable String value) {
        this();
        setAttrName(name);
        setValue(value);
    }


    /**
     * 【必选 属性 OFD 2.0】
     * 设置  扩展参数名称
     *
     * @param name 扩展参数名称
     * @return this
     */
    public Parameter setAttrName(@NotNull String name) {
        if (name == null) {
            throw new IllegalArgumentException("扩展参数名称(name)为空");
        }
        this.addAttribute("Name", name);
        return this;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 设置  扩展参数名称
     *
     * @param name 扩展参数名称
     * @return this
     */
    public Parameter setKeyName(@NotNull String name) {
        return setAttrName(name);
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 获取  扩展参数名称
     *
     * @return 扩展参数名称
     */
    @NotNull
    public String getAttrName() {
        String name = this.attributeValue("Name");
        if (name == null) {
            throw new IllegalArgumentException("扩展参数名称(name)为空");
        }
        return name;
    }

    /**
     * 【必选 属性 OFD 2.0】
     * 获取  扩展参数名称
     *
     * @return 扩展参数名称
     */
    @NotNull
    public String getKeyName() {
        return getAttrName();
    }

    /**
     * 【可选 OFD 2.0】
     * 设置 扩展参数值
     *
     * @param value 扩展参数值
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
     * 获取 扩展参数值
     *
     * @return 扩展参数值，可能为null
     */
    @Nullable
    public String getValue() {
        return this.getTextTrim();
    }
}
