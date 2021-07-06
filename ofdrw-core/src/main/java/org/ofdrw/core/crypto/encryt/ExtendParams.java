package org.ofdrw.core.crypto.encryt;

import org.dom4j.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ofdrw.core.OFDElement;

import java.util.List;

/**
 * 扩展参数节点
 * <p>
 * GMT0099 附录C 图 1
 *
 * @author 权观宇
 * @since 2021-06-23 19:31:33
 */
public class ExtendParams extends OFDElement {

    public ExtendParams(Element proxy) {
        super(proxy);
    }

    public ExtendParams() {
        super("ExtendParams");
    }

    /**
     * 【可选 OFD 2.0】
     * 增加 扩展参数
     *
     * @param parameter 扩展参数
     * @return this
     */
    public ExtendParams addParameter(Parameter parameter) {
        if (parameter == null) {
            return this;
        }
        this.add(parameter);
        return this;
    }

    /**
     * 【可选 OFD 2.0】
     * 增加 扩展参数
     *
     * @param name  扩展参数名称
     * @param value 扩展参数值
     * @return this
     */
    public ExtendParams addParameter(@NotNull String name, @Nullable String value) {
        return addParameter(new Parameter(name, value));
    }

    /**
     * 【可选 OFD 2.0】
     * 获取 扩展参数列表
     *
     * @return 扩展参数列表
     */
    public List<Parameter> getParameters() {
        return this.getOFDElements("Parameter", Parameter::new);
    }

    /**
     * 根据参数名获取参数值
     *
     * @param name 参数名
     * @return 参数值，可能为null
     */
    public String getValue(@Nullable String name) {
        if (name == null) {
            return null;
        }
        for (Parameter parameter : getParameters()) {
            if (parameter.getAttrName().equals(name)) {
                return parameter.getValue();
            }
        }
        return null;
    }
}
