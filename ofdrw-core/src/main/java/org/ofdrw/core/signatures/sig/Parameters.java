package org.ofdrw.core.signatures.sig;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * OFD 2.0 GMT0099 B.2
 * <p>
 * 签名扩展属性集
 *
 * @author 权观宇
 * @since 2021-06-19 12:53:53
 */
public class Parameters extends OFDElement {
    public Parameters(Element proxy) {
        super(proxy);
    }

    public Parameters() {
        super("Parameters");
    }

    /**
     * 【必选 OFD 2.0】
     * 增加 签名扩展属性
     *
     * @param parameter 签名扩展属性
     * @return this
     */
    public Parameters addParameter(@Nullable Parameter parameter) {
        if (parameter == null) {
            return this;
        }
        String name = parameter.getNameAttr();
        final Parameter oldP = getParameter(name);
        if (oldP != null) {
            // 对应名称的签名属性已经存在，那么就删除原有属性
            this.remove(oldP);
        }
        this.add(parameter);
        return this;
    }

    /**
     * 【必选 OFD 2.0】
     * 增加 签名扩展属性
     *
     * @param name  属性的名称
     * @param value 属性的值
     * @return this
     */
    public Parameters addParameter(@NotNull String name, String value) {
        final Parameter parameter = new Parameter(name,value);
        return this.addParameter(parameter);
    }
    /**
     * 【必选 OFD 2.0】
     * 增加 签名扩展属性
     *
     * @param name  属性的名称
     * @param type  属性的类型
     * @param value 属性的值
     * @return this
     */
    public Parameters addParameter(@NotNull String name, String type, String value) {
        final Parameter parameter = new Parameter(name, type, value);
        return this.addParameter(parameter);
    }

    /**
     * 通过 属性名删除 签名扩展属性
     *
     * @param name 签名扩展属性名称
     * @return 被删除的属性 或 null
     */
    @Nullable
    public Parameter removeParameter(@Nullable String name) {
        if (name == null) {
            return null;
        }
        Parameter oldP = getParameter(name);
        if (oldP != null) {
            this.remove(oldP);
        }
        return oldP;
    }

    /**
     * 通过属性名称获取签名扩展属性
     *
     * @param name 签名扩展属性名称
     * @return 签名扩展属性，或null
     */
    @Nullable
    public Parameter getParameter(@NotNull String name) {
        final List<Parameter> pList = getParameters();
        for (Parameter p : pList) {
            if (name.equals(p.getNameAttr())) {
                return p;
            }
        }
        return null;
    }

    /**
     * 【必选 OFD 2.0】
     * 获取 所有签名扩展属性
     *
     * @return 签名扩展属性列表
     */
    public List<Parameter> getParameters() {
        return this.getOFDElements("Parameter", Parameter::new);
    }
}
